package org.telegram.ui.Stars.demo;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SearchField;
import org.telegram.ui.Stars.StarGiftPreviewSheet;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

import java.util.ArrayList;
import java.util.Random;

/**
 * Step 2 of the DEMO: pick the model, the backdrop and the symbol independently, and see the result at once.
 *
 * <p>This is the stock {@link StarGiftPreviewSheet} (the "Models / Backdrops / Symbols" screen of the upgrade preview,
 * rendered by the stock {@code TopView}) fed with the whole {@link DemoGiftCatalog} instead of one gift's attributes, so
 * any model can be combined with any backdrop and any symbol. It adds a search field, a gift chip and the DEMO
 * "Upgrade" button. "Upgrade" never sends anything: it plays a roll through catalog traits with the stock cross-fade
 * and then shows {@link DemoResultSheet}.
 */
public final class DemoGiftEditorSheet extends StarGiftPreviewSheet {

    // Timeline of the roll, in ms after the button is pressed: random traits flick by, then the chosen ones land.
    private static final long[] ROLL_STEPS_MS = {350, 720, 1090, 1490, 1990, 2590};
    private static final long ROLL_LAND_MS = 3300;
    private static final long ROLL_DONE_MS = 4150;

    private final int account;
    private final Theme.ResourcesProvider themeProvider;
    private final DemoGiftCatalog catalog;
    private TL_stars.StarGift gift;

    private final LinearLayout bar;
    private final TextView giftChip;
    private final ButtonWithCounterView upgradeButton;
    private final View inputBlocker;

    private final ArrayList<Runnable> scheduled = new ArrayList<>();
    private final Random random = new Random();
    private boolean busy;
    private boolean dismissed;

    public DemoGiftEditorSheet(
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        int account,
        DemoGiftCatalog catalog,
        TL_stars.StarGift gift,
        StarGiftPreviewSheet.Attributes initial
    ) {
        super(context, resourcesProvider, account, DemoText.giftLabel(gift), catalog.all, false, initial, true);
        this.account = account;
        this.themeProvider = resourcesProvider;
        this.catalog = catalog;
        this.gift = gift;

        setStatusText(DemoText.statusSelected());

        // Search over the model / backdrop / symbol grids (names only; nothing is sent anywhere).
        final SearchField search = new SearchField(context, false, resourcesProvider) {
            @Override
            public void onTextChange(String text) {
                if (!busy) {
                    setAttributeFilter(text);
                }
            }
        };
        search.setHint(DemoText.search());
        getHeaderView().addView(search, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58, Gravity.TOP, 44, 0, 44, 0));

        getCollectionTitleView().setOnClickListener(v -> openGiftPicker(false));

        // Floating action row above the stock tabs: [gift chip] [Upgrade (DEMO)]
        bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);

        giftChip = new TextView(context);
        giftChip.setSingleLine(true);
        giftChip.setEllipsize(TextUtils.TruncateAt.END);
        giftChip.setGravity(Gravity.CENTER);
        giftChip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        giftChip.setTypeface(AndroidUtilities.bold());
        giftChip.setPadding(dp(12), 0, dp(12), 0);
        final int accent = Theme.getColor(Theme.key_featuredStickers_addButton, resourcesProvider);
        giftChip.setTextColor(accent);
        giftChip.setBackground(Theme.createRoundRectDrawable(dp(24), Theme.multAlpha(accent, 0.16f)));
        giftChip.setOnClickListener(v -> openGiftPicker(false));
        bar.addView(giftChip, LayoutHelper.createLinear(0, 48, 0.42f, Gravity.FILL, 0, 0, 8, 0));

        upgradeButton = new ButtonWithCounterView(context, true, resourcesProvider).setRound();
        upgradeButton.setText(DemoText.upgradeDemo(), false);
        upgradeButton.setOnClickListener(v -> startUpgrade());
        bar.addView(upgradeButton, LayoutHelper.createLinear(0, 48, 0.58f, Gravity.FILL, 0, 0, 0, 0));

        containerView.addView(bar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 12, 0, 12, 5 + 64 + 8));
        setExtraBottomPadding(dp(48 + 8 + 10));

        // Swallows touches while the roll plays so the choices cannot change under it.
        inputBlocker = new View(context);
        inputBlocker.setClickable(true);
        inputBlocker.setVisibility(View.GONE);
        containerView.addView(inputBlocker, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        updateGiftChip();
        updateBarMargin();
    }

    // ------------------------------------------------------------------------------------------------ layout

    @Override
    protected void onInsetsChanged() {
        super.onInsetsChanged();
        updateBarMargin();
    }

    private void updateBarMargin() {
        if (bar == null) {
            return; // called by the superclass before this class is constructed
        }
        final ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bar.getLayoutParams();
        final int margin = dp(5 + 64 + 8) + getSystemBottomInset();
        if (lp.bottomMargin != margin) {
            lp.bottomMargin = margin;
            bar.setLayoutParams(lp);
        }
    }

    private void updateGiftChip() {
        giftChip.setText(DemoText.giftLabel(gift) + " ▾");
    }

    // ------------------------------------------------------------------------------------------------ gift

    private void openGiftPicker(boolean resetTraits) {
        if (busy || dismissed) {
            return;
        }
        new DemoGiftPickerSheet(getContext(), themeProvider, account, catalog, picked -> setGift(picked, resetTraits)).show();
    }

    private void setGift(TL_stars.StarGift picked, boolean resetTraits) {
        this.gift = picked;
        setCollectionTitle(DemoText.giftLabel(picked));
        updateGiftChip();
        if (resetTraits) {
            final StarGiftPreviewSheet.Attributes defaults = catalog.defaultsFor(picked);
            if (defaults != null) {
                selectAttributes(defaults);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ upgrade (local only)

    private void startUpgrade() {
        if (busy || dismissed) {
            return;
        }
        final StarGiftPreviewSheet.Attributes selected = commitSelectedAttributes();
        if (selected == null || selected.model == null || selected.pattern == null || selected.backdrop == null) {
            Toast.makeText(getContext(), DemoText.catalogUnavailable(), Toast.LENGTH_LONG).show();
            return;
        }
        busy = true;
        inputBlocker.setVisibility(View.VISIBLE);
        upgradeButton.setLoading(true);

        for (long delay : ROLL_STEPS_MS) {
            schedule(delay, () -> previewAttributes(randomTraits()));
        }
        schedule(ROLL_LAND_MS, () -> previewAttributes(selected));
        schedule(ROLL_DONE_MS, () -> finishUpgrade(selected));
    }

    private StarGiftPreviewSheet.Attributes randomTraits() {
        return new StarGiftPreviewSheet.Attributes(
            catalog.backdrops.get(random.nextInt(catalog.backdrops.size())),
            catalog.patterns.get(random.nextInt(catalog.patterns.size())),
            catalog.models.get(random.nextInt(catalog.models.size()))
        );
    }

    private void finishUpgrade(StarGiftPreviewSheet.Attributes selected) {
        if (dismissed) {
            return;
        }
        final DemoGiftStore store = DemoGiftStore.get();
        final int serial = store.nextSerial();
        final TL_stars.TL_starGiftUnique unique = DemoUniqueGifts.build(gift, selected, serial);
        final int stored = store.add(DemoUniqueGifts.toRecord(catalog, gift, selected, serial));

        busy = false;
        inputBlocker.setVisibility(View.GONE);
        upgradeButton.setLoading(false);

        new DemoResultSheet(getContext(), themeProvider, catalog, gift, selected, unique, stored, new DemoResultSheet.Callbacks() {
            @Override
            public void onEdit() {
                // the editor is still here with the same choices
            }

            @Override
            public void onRepeat() {
                // let the result sheet finish closing first
                schedule(260, DemoGiftEditorSheet.this::startUpgrade);
            }

            @Override
            public void onPickAnother() {
                schedule(260, () -> openGiftPicker(true));
            }
        }).show();
    }

    // ------------------------------------------------------------------------------------------------ plumbing

    private void schedule(long delayMs, Runnable action) {
        final Runnable[] holder = new Runnable[1];
        holder[0] = () -> {
            scheduled.remove(holder[0]);
            if (!dismissed) {
                action.run();
            }
        };
        scheduled.add(holder[0]);
        AndroidUtilities.runOnUIThread(holder[0], delayMs);
    }

    @Override
    public void dismiss() {
        dismissed = true;
        for (Runnable r : new ArrayList<>(scheduled)) {
            AndroidUtilities.cancelRunOnUIThread(r);
        }
        scheduled.clear();
        super.dismiss();
    }
}
