package org.telegram.ui.Stars.demo;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.GiftAuctionController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.Stars.StarGiftPreviewSheet;
import org.telegram.ui.Stars.StarsController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * The gifts the DEMO offers, and - per gift - the traits that belong to it.
 *
 * <p>Data comes only from what the client already loads (nothing is invented, nothing is merged across gifts):
 * <ul>
 *   <li>gifts - the stock gift list ({@link StarsController#gifts}, {@code payments.getStarGifts}); only base gifts that
 *       can be upgraded and have artwork;</li>
 *   <li>traits - the full list of that one gift's models, backdrops and symbols, from the request the stock
 *       "view all variants" screen uses ({@link GiftAuctionController#requestAuctionUpgrades},
 *       {@code payments.getStarGiftUpgradeAttributes}). Only if that fails, the much smaller sample the upgrade
 *       preview carries ({@link StarsController#getStarGiftPreview}, {@code payments.getStarGiftUpgradePreview},
 *       {@link GiftTraits#partial}).</li>
 * </ul>
 * All requests are read-only. If the server returns no traits for a gift, {@link GiftTraits} is not usable and the DEMO
 * says so instead of showing anything made up.
 */
public final class DemoGiftCatalog {

    public interface Listener {
        void onReady(DemoGiftCatalog catalog);
    }

    public interface TraitsListener {
        /** Null if the request failed; otherwise check {@link GiftTraits#isUsable()}. */
        void onTraits(@Nullable GiftTraits traits);
    }

    /** The models, backdrops and symbols of one gift, as the server returned them (original objects, with rarity). */
    public static final class GiftTraits {
        /** The server's list for this gift only (backdrops, symbols, models) - what the stock preview sheet takes. */
        public final ArrayList<TL_stars.StarGiftAttribute> all = new ArrayList<>();
        public final ArrayList<TL_stars.starGiftAttributeModel> models = new ArrayList<>();
        public final ArrayList<TL_stars.starGiftAttributePattern> patterns = new ArrayList<>();
        public final ArrayList<TL_stars.starGiftAttributeBackdrop> backdrops = new ArrayList<>();

        /** True when only the small sample from the upgrade preview is available (the full list could not be loaded). */
        public boolean partial;

        private static final int ROLL_POOL_PER_KIND = 12;

        /**
         * What the roll passes through: a random handful of this gift's own variants, about as many as the stock upgrade
         * preview sample carries. (The roll loads an animated sticker per model, so the full list is not used for it.)
         */
        public ArrayList<TL_stars.StarGiftAttribute> rollPool() {
            final ArrayList<TL_stars.StarGiftAttribute> pool = new ArrayList<>();
            pool.addAll(pick(backdrops));
            pool.addAll(pick(patterns));
            pool.addAll(pick(models));
            return pool;
        }

        private static <T> ArrayList<T> pick(ArrayList<T> from) {
            final ArrayList<T> copy = new ArrayList<>(from);
            Collections.shuffle(copy);
            return new ArrayList<>(copy.subList(0, Math.min(ROLL_POOL_PER_KIND, copy.size())));
        }

        /** All three kinds are present, which is what the picker and the renderer need. */
        public boolean isUsable() {
            return !models.isEmpty() && !patterns.isEmpty() && !backdrops.isEmpty();
        }

        /** The gift's own first model / symbol / backdrop, or null when not usable. */
        @Nullable
        public StarGiftPreviewSheet.Attributes defaults() {
            if (!isUsable()) {
                return null;
            }
            return new StarGiftPreviewSheet.Attributes(backdrops.get(0), patterns.get(0), models.get(0));
        }
    }

    private static final long GIFTS_WAIT_MS = 20_000;
    private static final long CACHE_TTL_MS = 10 * 60 * 1000L;

    private static final HashMap<Integer, DemoGiftCatalog> cache = new HashMap<>();

    /** The catalog for the account: the ready cached one when it is recent, otherwise a fresh (unloaded) one. */
    public static DemoGiftCatalog forAccount(int account) {
        synchronized (cache) {
            final DemoGiftCatalog cached = cache.get(account);
            if (cached != null && cached.ready && System.currentTimeMillis() - cached.readyAt < CACHE_TTL_MS) {
                return cached;
            }
            final DemoGiftCatalog fresh = new DemoGiftCatalog(account);
            cache.put(account, fresh);
            return fresh;
        }
    }

    private final int account;

    /** Base gifts that can be upgraded and have artwork to show. */
    public final ArrayList<TL_stars.StarGift> gifts = new ArrayList<>();
    /** Base gifts left out because they cannot be upgraded (so they have no models / backdrops / symbols). */
    public int skippedGifts;

    private boolean ready;
    private long readyAt;
    private boolean cancelled;
    private Listener listener;
    private NotificationCenter.NotificationCenterDelegate giftsObserver;
    private Runnable giftsTimeout;

    private DemoGiftCatalog(int account) {
        this.account = account;
    }

    public boolean isReady() {
        return ready;
    }

    /** Loads the gift list (a read-only request the client makes anyway) and reports on the UI thread. */
    public void load(Listener listener) {
        if (ready) {
            listener.onReady(this);
            return;
        }
        this.listener = listener;
        this.cancelled = false;
        if (giftListAvailable()) {
            finish();
            return;
        }
        giftsObserver = (id, acc, args) -> {
            if (id == NotificationCenter.starGiftsLoaded && giftListAvailable()) {
                stopWaiting();
                finish();
            }
        };
        NotificationCenter.getInstance(account).addObserver(giftsObserver, NotificationCenter.starGiftsLoaded);
        giftsTimeout = () -> {
            giftsTimeout = null;
            stopWaiting();
            finish(); // whatever the controller has by now; an empty list is reported as such
        };
        AndroidUtilities.runOnUIThread(giftsTimeout, GIFTS_WAIT_MS);
        StarsController.getInstance(account).loadStarGifts();
    }

    public void cancel() {
        cancelled = true;
        listener = null;
        stopWaiting();
    }

    private boolean giftListAvailable() {
        final StarsController controller = StarsController.getInstance(account);
        return controller.giftsLoaded && !controller.gifts.isEmpty();
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

    private void finish() {
        if (cancelled || ready) {
            return;
        }
        gifts.clear();
        skippedGifts = 0;
        for (TL_stars.StarGift gift : new ArrayList<>(StarsController.getInstance(account).gifts)) {
            // Base gifts only (collectibles are the result of an upgrade), with artwork to show.
            if (!(gift instanceof TL_stars.TL_starGift) || gift.getDocument() == null) {
                continue;
            }
            if (looksUpgradable(gift)) {
                gifts.add(gift);
            } else {
                skippedGifts++;
            }
        }
        ready = true;
        readyAt = System.currentTimeMillis();
        final Listener l = listener;
        listener = null;
        if (l != null) {
            l.onReady(this);
        }
    }

    /**
     * Loads the traits of exactly this gift: the full list first, the small preview sample only as a fallback. The
     * callback runs on the UI thread.
     */
    public void loadTraits(TL_stars.StarGift gift, TraitsListener callback) {
        GiftAuctionController.getInstance(account).requestAuctionUpgrades(gift.id, attributes -> {
            if (attributes != null && !attributes.isEmpty()) {
                callback.onTraits(buildTraits(attributes, false));
                return;
            }
            StarsController.getInstance(account).getStarGiftPreview(gift.id, preview -> {
                if (preview == null || preview.sample_attributes == null) {
                    callback.onTraits(null);
                    return;
                }
                callback.onTraits(buildTraits(preview.sample_attributes, true));
            });
        });
    }

    private static GiftTraits buildTraits(ArrayList<TL_stars.StarGiftAttribute> source, boolean partial) {
        final GiftTraits traits = new GiftTraits();
        traits.partial = partial;
        for (TL_stars.StarGiftAttribute attribute : source) {
            if (attribute instanceof TL_stars.starGiftAttributeModel && ((TL_stars.starGiftAttributeModel) attribute).document != null) {
                traits.models.add((TL_stars.starGiftAttributeModel) attribute);
            } else if (attribute instanceof TL_stars.starGiftAttributePattern && ((TL_stars.starGiftAttributePattern) attribute).document != null) {
                traits.patterns.add((TL_stars.starGiftAttributePattern) attribute);
            } else if (attribute instanceof TL_stars.starGiftAttributeBackdrop) {
                traits.backdrops.add((TL_stars.starGiftAttributeBackdrop) attribute);
            }
        }
        traits.all.addAll(traits.backdrops);
        traits.all.addAll(traits.patterns);
        traits.all.addAll(traits.models);
        return traits;
    }
}
