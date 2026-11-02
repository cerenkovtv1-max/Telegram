package org.telegram.ui.Stars.demo;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SearchField;
import org.telegram.ui.Stars.StarGiftPreviewSheet;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

/**
 * The DEMO picker: three steps - model, backdrop, symbol - for ONE gift.
 *
 * <p>It is the stock {@link StarGiftPreviewSheet} (the "Models / Backdrops / Symbols" screen the upgrade preview uses,
 * drawn by the stock {@code TopView}), fed with the attributes of the chosen gift only
 * ({@link DemoGiftCatalog.GiftTraits}): other gifts' models, backdrops and symbols are never in the list. A bottom
 * button walks through the steps ("Next: backdrop" -> "Next: symbol" -> "Done"); the tab bar still allows jumping
 * back. "Done" hands the picked traits to {@code onDone}; nothing is sent anywhere.
 */
public final class DemoGiftEditorSheet extends StarGiftPreviewSheet {

    public interface OnDone {
        void onDone(StarGiftPreviewSheet.Attributes chosen);
    }

    private static final int STEPS = 3;

    private final String giftTitle;
    private final OnDone onDone;

    private final FrameLayout bar;
    private final ButtonWithCounterView nextButton;
    private final SearchField search;

    public DemoGiftEditorSheet(
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        int account,
        TL_stars.StarGift gift,
        DemoGiftCatalog.GiftTraits traits,
        StarGiftPreviewSheet.Attributes initial,
        OnDone onDone
    ) {
        super(context, resourcesProvider, account, DemoText.giftLabel(gift), traits.all, false, initial, true);
        this.giftTitle = DemoText.giftLabel(gift);
        this.onDone = onDone;

        // Search over the current step's grid (names only; nothing is sent anywhere).
        search = new SearchField(context, false, resourcesProvider) {
            @Override
            public void onTextChange(String text) {
                setAttributeFilter(text);
            }
        };
        search.setHint(DemoText.search());
        getHeaderView().addView(search, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 58, Gravity.TOP, 44, 0, 44, 0));

        // The step button, floating above the stock tabs.
        bar = new FrameLayout(context);
        nextButton = new ButtonWithCounterView(context, true, resourcesProvider).setRound();
        nextButton.setOnClickListener(v -> onNext());
        bar.addView(nextButton, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48));
        containerView.addView(bar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.BOTTOM, 12, 0, 12, 5 + 64 + 8));
        setExtraBottomPadding(dp(48 + 8 + 10));

        updateStepUi(getSelectedStep(), false);
        updateBarMargin();
    }

    // ------------------------------------------------------------------------------------------------ steps

    @Override
    protected void onStepChanged(int tab) {
        // the filter belongs to the step it was typed in
        if (search != null && search.getSearchEditText().length() > 0) {
            search.getSearchEditText().setText("");
        }
        updateStepUi(tab, true);
    }

    private void updateStepUi(int step, boolean animated) {
        if (nextButton == null) {
            return; // called by the superclass before this class is constructed
        }
        final CharSequence text;
        if (step <= 0) {
            text = DemoText.nextBackdrop();
        } else if (step == 1) {
            text = DemoText.nextSymbol();
        } else {
            text = DemoText.done();
        }
        nextButton.setText(text, animated);
        setStatusText(DemoText.statusStep(step, giftTitle));
    }

    private void onNext() {
        final int step = getSelectedStep();
        if (step < STEPS - 1) {
            selectStep(step + 1);
            return;
        }
        final StarGiftPreviewSheet.Attributes chosen = commitSelectedAttributes();
        if (chosen == null || chosen.model == null || chosen.pattern == null || chosen.backdrop == null) {
            Toast.makeText(getContext(), DemoText.noTraits(giftTitle), Toast.LENGTH_LONG).show();
            return;
        }
        dismiss();
        onDone.onDone(chosen);
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
}
