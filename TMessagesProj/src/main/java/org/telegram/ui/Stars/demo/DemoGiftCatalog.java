package org.telegram.ui.Stars.demo;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.QrShare.DemoBackdropsBridge;
import org.telegram.ui.Stars.StarGiftPreviewSheet;
import org.telegram.ui.Stars.StarsController;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;

/**
 * The independent catalog the DEMO editor picks from: every base gift the client knows, and the union of the models,
 * symbols (patterns) and backdrops of all of them, offered with no compatibility rules.
 *
 * <p>Where the data comes from (nothing here is invented):
 * <ul>
 *   <li>gifts - the stock gift list ({@link StarsController#gifts}, {@code payments.getStarGifts});</li>
 *   <li>models, symbols, backdrops - each upgradable gift's stock attribute preview
 *       ({@link StarsController#getStarGiftPreview}, {@code payments.getStarGiftUpgradePreview}), merged and de-duplicated;</li>
 *   <li>extra backdrops - the 80-entry table built into the client ({@link DemoBackdropsBridge}), only for names the
 *       server data does not already cover. Those carry colours and a name but no server id.</li>
 * </ul>
 * Both requests are read-only. Models and symbols exist only as server-delivered stickers, so without server data the
 * catalog has no models/symbols and {@link #isUsable()} is false.
 */
public final class DemoGiftCatalog {

    public interface Listener {
        void onProgress(int done, int total);

        void onReady(DemoGiftCatalog catalog);
    }

    private static final int MAX_PARALLEL_PREVIEWS = 6;
    private static final long GIFTS_WAIT_MS = 20_000;
    private static final long PREVIEWS_WATCHDOG_MS = 90_000;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;

    private static final HashMap<Integer, DemoGiftCatalog> cache = new HashMap<>();

    /** The catalog for the account: the ready cached one when it is recent and usable, otherwise a fresh (unloaded) one. */
    public static DemoGiftCatalog forAccount(int account) {
        synchronized (cache) {
            final DemoGiftCatalog cached = cache.get(account);
            if (cached != null && cached.ready && cached.isUsable() && System.currentTimeMillis() - cached.readyAt < CACHE_TTL_MS) {
                return cached;
            }
            final DemoGiftCatalog fresh = new DemoGiftCatalog(account);
            cache.put(account, fresh);
            return fresh;
        }
    }

    private final int account;

    // ---- the catalog ----
    public final ArrayList<TL_stars.StarGift> gifts = new ArrayList<>();
    public final ArrayList<TL_stars.starGiftAttributeModel> models = new ArrayList<>();
    public final ArrayList<TL_stars.starGiftAttributePattern> patterns = new ArrayList<>();
    public final ArrayList<TL_stars.starGiftAttributeBackdrop> backdrops = new ArrayList<>();
    /** Backdrops, symbols and models together, in the form the stock preview sheet takes. */
    public final ArrayList<TL_stars.StarGiftAttribute> all = new ArrayList<>();

    // ---- bookkeeping, shown to the user / used for the report ----
    public int giftsWithAttributes;
    public int giftsPreviewFailed;
    public int giftsNotUpgradable;
    public int backdropsFromServer;
    public int backdropsFromBuiltInTable;

    private static final class GiftTraits {
        final ArrayList<TL_stars.starGiftAttributeModel> models = new ArrayList<>();
        final ArrayList<TL_stars.starGiftAttributePattern> patterns = new ArrayList<>();
        final ArrayList<TL_stars.starGiftAttributeBackdrop> backdrops = new ArrayList<>();
    }

    private final HashMap<Long, GiftTraits> traitsByGift = new HashMap<>();
    private final IdentityHashMap<TL_stars.StarGiftAttribute, String> sourceGiftTitle = new IdentityHashMap<>();
    private final IdentityHashMap<TL_stars.starGiftAttributeBackdrop, Boolean> builtInOnly = new IdentityHashMap<>();
    private final HashMap<Long, TL_stars.starGiftAttributeModel> modelByDocument = new HashMap<>();
    private final HashMap<Long, TL_stars.starGiftAttributePattern> patternByDocument = new HashMap<>();
    private final HashMap<Integer, TL_stars.starGiftAttributeBackdrop> backdropById = new HashMap<>();
    private final HashSet<String> backdropNames = new HashSet<>();

    // ---- loading state ----
    private boolean ready;
    private long readyAt;
    private boolean cancelled;
    private boolean finished;
    private Listener listener;
    private NotificationCenter.NotificationCenterDelegate giftsObserver;
    private Runnable giftsTimeout;
    private Runnable previewsWatchdog;
    private final ArrayDeque<TL_stars.StarGift> previewQueue = new ArrayDeque<>();
    private int previewsTotal, previewsDone, previewsInFlight;
    private boolean pumping;

    private DemoGiftCatalog(int account) {
        this.account = account;
    }

    public boolean isReady() {
        return ready;
    }

    /** Models, symbols and backdrops are all present, which is what the editor needs to render anything. */
    public boolean isUsable() {
        return !models.isEmpty() && !patterns.isEmpty() && !backdrops.isEmpty() && !gifts.isEmpty();
    }

    /** Loads the catalog (read-only requests only) and reports on the UI thread. Safe to call once per instance. */
    public void load(Listener listener) {
        if (ready) {
            listener.onReady(this);
            return;
        }
        this.listener = listener;
        this.cancelled = false;
        waitForGifts();
    }

    public void cancel() {
        cancelled = true;
        listener = null;
        stopWaiting();
        if (previewsWatchdog != null) {
            AndroidUtilities.cancelRunOnUIThread(previewsWatchdog);
            previewsWatchdog = null;
        }
    }

    // ------------------------------------------------------------------------------------------------ gifts

    private boolean giftListAvailable() {
        final StarsController controller = StarsController.getInstance(account);
        return controller.giftsLoaded && !controller.gifts.isEmpty();
    }

    private void waitForGifts() {
        if (giftListAvailable()) {
            startPreviews();
            return;
        }
        giftsObserver = (id, acc, args) -> {
            if (id == NotificationCenter.starGiftsLoaded && giftListAvailable()) {
                stopWaiting();
                startPreviews();
            }
        };
        NotificationCenter.getInstance(account).addObserver(giftsObserver, NotificationCenter.starGiftsLoaded);
        giftsTimeout = () -> {
            giftsTimeout = null;
            stopWaiting();
            // Whatever the controller has by now (possibly from its own cache); the result is judged by isUsable().
            startPreviews();
        };
        AndroidUtilities.runOnUIThread(giftsTimeout, GIFTS_WAIT_MS);
        StarsController.getInstance(account).loadStarGifts();
    }

    private void stopWaiting() {
        if (giftsObserver != null) {
            NotificationCenter.getInstance(account).removeObserver(giftsObserver, NotificationCenter.starGiftsLoaded);
            giftsObserver = null;
        }
        if (giftsTimeout != null) {
            AndroidUtilities.cancelRunOnUIThread(giftsTimeout);
            giftsTimeout = null;
        }
    }

    private static boolean looksUpgradable(TL_stars.StarGift gift) {
        return gift.can_upgrade || gift.upgrade_stars > 0 || gift.upgrade_variants > 0;
    }

    // ------------------------------------------------------------------------------------------------ previews

    private void startPreviews() {
        if (cancelled || finished || previewsTotal > 0 || pumping) {
            return;
        }
        for (TL_stars.StarGift gift : new ArrayList<>(StarsController.getInstance(account).gifts)) {
            // Base gifts only (collectibles are the result of an upgrade), and only ones with artwork to show.
            if (!(gift instanceof TL_stars.TL_starGift) || gift.getDocument() == null) {
                continue;
            }
            gifts.add(gift);
            if (looksUpgradable(gift)) {
                previewQueue.add(gift);
            } else {
                giftsNotUpgradable++;
            }
        }
        previewsTotal = previewQueue.size();
        if (previewsTotal == 0) {
            finish();
            return;
        }
        previewsWatchdog = () -> {
            previewsWatchdog = null;
            FileLog.e("DemoGiftCatalog: preview loading timed out, using what has arrived");
            finish();
        };
        AndroidUtilities.runOnUIThread(previewsWatchdog, PREVIEWS_WATCHDOG_MS);
        pump();
    }

    private void pump() {
        if (pumping) {
            return; // a synchronous (cached) callback re-entered; the outer loop continues
        }
        pumping = true;
        try {
            while (!cancelled && !finished && previewsInFlight < MAX_PARALLEL_PREVIEWS && !previewQueue.isEmpty()) {
                final TL_stars.StarGift gift = previewQueue.poll();
                previewsInFlight++;
                StarsController.getInstance(account).getStarGiftPreview(gift.id, preview -> {
                    if (cancelled || finished) {
                        return;
                    }
                    previewsInFlight--;
                    previewsDone++;
                    onPreview(gift, preview);
                    if (listener != null) {
                        listener.onProgress(previewsDone, previewsTotal);
                    }
                    // Next request. If this callback ran synchronously (cached preview) we are still inside the
                    // loop below and pump() returns at once; the loop then carries on by itself.
                    pump();
                });
            }
        } finally {
            pumping = false;
        }
        if (!cancelled && !finished && previewsDone >= previewsTotal) {
            finish();
        }
    }

    private void onPreview(TL_stars.StarGift gift, @Nullable TL_stars.starGiftUpgradePreview preview) {
        if (preview == null || preview.sample_attributes == null || preview.sample_attributes.isEmpty()) {
            giftsPreviewFailed++;
            return;
        }
        giftsWithAttributes++;
        final String sourceTitle = DemoText.giftLabel(gift);
        final GiftTraits traits = new GiftTraits();
        traitsByGift.put(gift.id, traits);

        for (TL_stars.StarGiftAttribute attribute : preview.sample_attributes) {
            if (attribute instanceof TL_stars.starGiftAttributeModel) {
                final TL_stars.starGiftAttributeModel model = (TL_stars.starGiftAttributeModel) attribute;
                if (model.document == null) continue;
                TL_stars.starGiftAttributeModel canonical = modelByDocument.get(model.document.id);
                if (canonical == null) {
                    canonical = model;
                    modelByDocument.put(model.document.id, model);
                    models.add(model);
                    sourceGiftTitle.put(model, sourceTitle);
                }
                traits.models.add(canonical);
            } else if (attribute instanceof TL_stars.starGiftAttributePattern) {
                final TL_stars.starGiftAttributePattern pattern = (TL_stars.starGiftAttributePattern) attribute;
                if (pattern.document == null) continue;
                TL_stars.starGiftAttributePattern canonical = patternByDocument.get(pattern.document.id);
                if (canonical == null) {
                    canonical = pattern;
                    patternByDocument.put(pattern.document.id, pattern);
                    patterns.add(pattern);
                    sourceGiftTitle.put(pattern, sourceTitle);
                }
                traits.patterns.add(canonical);
            } else if (attribute instanceof TL_stars.starGiftAttributeBackdrop) {
                final TL_stars.starGiftAttributeBackdrop backdrop = (TL_stars.starGiftAttributeBackdrop) attribute;
                TL_stars.starGiftAttributeBackdrop canonical = backdropById.get(backdrop.backdrop_id);
                if (canonical == null) {
                    // A copy without rarity: the permille the server sends is specific to this gift's collection,
                    // so showing it for a backdrop merged from many collections would be misleading. The stock
                    // attribute (and the stock cache it lives in) is left untouched.
                    canonical = copyBackdrop(backdrop.name, backdrop.backdrop_id, backdrop.center_color, backdrop.edge_color, backdrop.pattern_color, backdrop.text_color);
                    backdropById.put(backdrop.backdrop_id, canonical);
                    backdrops.add(canonical);
                    backdropsFromServer++;
                    if (backdrop.name != null) backdropNames.add(backdrop.name.toLowerCase(Locale.ROOT));
                }
                traits.backdrops.add(canonical);
            }
        }
    }

    private static TL_stars.starGiftAttributeBackdrop copyBackdrop(String name, int id, int center, int edge, int pattern, int text) {
        final TL_stars.starGiftAttributeBackdrop copy = new TL_stars.starGiftAttributeBackdrop();
        copy.name = name;
        copy.backdrop_id = id;
        copy.center_color = center;
        copy.edge_color = edge;
        copy.pattern_color = pattern;
        copy.text_color = text;
        copy.rarity = null;
        return copy;
    }

    // ------------------------------------------------------------------------------------------------ finish

    private void finish() {
        if (finished || cancelled) {
            return;
        }
        finished = true;
        if (previewsWatchdog != null) {
            AndroidUtilities.cancelRunOnUIThread(previewsWatchdog);
            previewsWatchdog = null;
        }

        // The client's own backdrop table, for names the server data does not already cover.
        try {
            for (DemoBackdropsBridge.Entry entry : DemoBackdropsBridge.snapshot()) {
                if (entry.name == null || backdropNames.contains(entry.name.toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (entry.serverId != null && backdropById.containsKey(entry.serverId)) {
                    continue;
                }
                final TL_stars.starGiftAttributeBackdrop backdrop = copyBackdrop(
                    entry.name, entry.serverId != null ? entry.serverId : 0,
                    entry.center, entry.edge, entry.pattern, entry.text
                );
                if (entry.serverId == null) {
                    builtInOnly.put(backdrop, Boolean.TRUE);
                }
                backdrops.add(backdrop);
                backdropNames.add(entry.name.toLowerCase(Locale.ROOT));
                backdropsFromBuiltInTable++;
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }

        all.addAll(backdrops);
        all.addAll(patterns);
        all.addAll(models);
        ready = true;
        readyAt = System.currentTimeMillis();

        final Listener l = listener;
        listener = null;
        if (l != null) {
            l.onReady(this);
        }
    }

    // ------------------------------------------------------------------------------------------------ queries

    /** True for a backdrop that exists only in the client's built-in table (colours and name, no server id). */
    public boolean isBuiltInOnly(TL_stars.starGiftAttributeBackdrop backdrop) {
        return builtInOnly.containsKey(backdrop);
    }

    /** The title of the gift whose attribute preview this model / symbol first came from, or null. */
    @Nullable
    public String sourceGiftTitle(TL_stars.StarGiftAttribute attribute) {
        return sourceGiftTitle.get(attribute);
    }

    /**
     * The traits to start from when the user picks a gift: that gift's own first model / symbol / backdrop when the
     * server sent its attributes, otherwise the first of the whole catalog. Null only if the catalog is unusable.
     */
    @Nullable
    public StarGiftPreviewSheet.Attributes defaultsFor(@Nullable TL_stars.StarGift gift) {
        if (!isUsable()) {
            return null;
        }
        final GiftTraits own = gift == null ? null : traitsByGift.get(gift.id);
        final TL_stars.starGiftAttributeModel model = own != null && !own.models.isEmpty() ? own.models.get(0) : models.get(0);
        final TL_stars.starGiftAttributePattern pattern = own != null && !own.patterns.isEmpty() ? own.patterns.get(0) : patterns.get(0);
        final TL_stars.starGiftAttributeBackdrop backdrop = own != null && !own.backdrops.isEmpty() ? own.backdrops.get(0) : backdrops.get(0);
        return new StarGiftPreviewSheet.Attributes(backdrop, pattern, model);
    }

    /** Looks a gift up by its id among the catalog's gifts. */
    @Nullable
    public TL_stars.StarGift findGift(long id) {
        for (TL_stars.StarGift gift : gifts) {
            if (gift.id == id) {
                return gift;
            }
        }
        return null;
    }
}
