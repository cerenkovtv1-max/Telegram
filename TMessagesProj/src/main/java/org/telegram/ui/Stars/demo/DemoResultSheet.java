package org.telegram.ui.Stars.demo;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Stars.StarGiftPreviewSheet;
import org.telegram.ui.Stars.StarGiftSheet;
import org.telegram.ui.Stories.recorder.ButtonWithCounterView;

/**
 * The result card of a DEMO upgrade. The collectible is drawn by the stock renderer ({@code StarGiftSheet.TopView}, the
 * same view the real gift screen uses for a collectible) from a locally built object, and is marked as DEMO in three
 * places: a badge on the card, the subtitle and a notice below. There are no real actions here (no transfer, sell or
 * share), only the three DEMO ones.
 */
final class DemoResultSheet {

    interface Callbacks {
        /** Back to the editor with the same choices. */
        void onEdit();

        /** Upgrade again with the same gift and traits. */
        void onRepeat();

        /** Back to the gift list. */
        void onPickAnother();
    }

    private final BottomSheet sheet;

    DemoResultSheet(
        Context context,
        Theme.ResourcesProvider resourcesProvider,
        DemoGiftCatalog catalog,
        TL_stars.StarGift base,
        StarGiftPreviewSheet.Attributes traits,
        TL_stars.TL_starGiftUnique unique,
        int storedCount,
        Callbacks callbacks
    ) {
        sheet = new BottomSheet(context, false, resourcesProvider);
        sheet.fixNavigationBar(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

        final LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        // ---- the card, drawn by the stock renderer ----
        final StarGiftSheet.TopView topView = new StarGiftSheet.TopView(context, resourcesProvider, () -> sheet.dismiss(), v -> {}, null, v -> {}, v -> {}, v -> {}, v -> {}, v -> {}) {
            @Override
            public void onSwitchPage(StarGiftSheet.PageTransition p) {
                super.onSwitchPage(p);
                // the stock "..." menu has nothing to offer on a DEMO card
                if (optionsView != null) {
                    optionsView.setVisibility(View.GONE);
                }
            }
        };
        topView.setGift(unique, false, false, false, false, false);
        topView.setText(
            StarGiftSheet.PAGE_INFO,
            DemoText.giftLabel(base) + " #" + unique.num,
            DemoText.resultSubtitle(),
            null,
            null
        );
        topView.hideCloseButton();

        final FrameLayout topFrame = new FrameLayout(context);
        topFrame.addView(topView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.FILL_HORIZONTAL | Gravity.TOP));
        final TextView badge = new TextView(context);
        badge.setText(DemoText.demoBadge());
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setTextColor(Color.WHITE);
        badge.setPadding(dp(8), dp(3), dp(8), dp(3));
        badge.setBackground(Theme.createRoundRectDrawable(dp(10), 0x99000000));
        topFrame.addView(badge, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 14, 14, 0, 0));
        root.addView(topFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // ---- what was picked ----
        final String modelSource = catalog.sourceGiftTitle(traits.model);
        final String patternSource = catalog.sourceGiftTitle(traits.pattern);
        addRow(root, context, resourcesProvider, DemoText.modelLabel(), traits.model.name,
            note(StarGiftSheet.getRarityName(traits.model.rarity), modelSource));
        addRow(root, context, resourcesProvider, DemoText.backdropLabel(), traits.backdrop.name,
            catalog.isBuiltInOnly(traits.backdrop) ? DemoText.builtinBackdropNote() : null);
        addRow(root, context, resourcesProvider, DemoText.symbolLabel(), traits.pattern.name,
            note(StarGiftSheet.getRarityName(traits.pattern.rarity), patternSource));

        // ---- DEMO notice ----
        final TextView notice = new TextView(context);
        notice.setText(DemoText.resultNotice());
        notice.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        notice.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        notice.setGravity(Gravity.CENTER);
        root.addView(notice, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 24, 14, 24, 0));

        final TextView saved = new TextView(context);
        saved.setText(DemoText.savedLocally(storedCount));
        saved.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        saved.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        saved.setGravity(Gravity.CENTER);
        root.addView(saved, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 24, 4, 24, 0));

        // ---- actions ----
        final ButtonWithCounterView edit = new ButtonWithCounterView(context, true, resourcesProvider).setRound();
        edit.setText(DemoText.edit(), false);
        edit.setOnClickListener(v -> {
            sheet.dismiss();
            callbacks.onEdit();
        });
        root.addView(edit, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP, 16, 16, 16, 0));

        final ButtonWithCounterView repeat = new ButtonWithCounterView(context, false, resourcesProvider);
        repeat.setText(DemoText.repeat(), false);
        repeat.setOnClickListener(v -> {
            sheet.dismiss();
            callbacks.onRepeat();
        });
        root.addView(repeat, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, Gravity.TOP, 16, 4, 16, 0));

        final ButtonWithCounterView another = new ButtonWithCounterView(context, false, resourcesProvider);
        another.setText(DemoText.pickAnotherGift(), false);
        another.setOnClickListener(v -> {
            sheet.dismiss();
            callbacks.onPickAnother();
        });
        root.addView(another, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 44, Gravity.TOP, 16, 0, 16, 12));

        // tall content on small screens scrolls instead of being cut off
        final ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(false);
        scroll.addView(root, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT));
        sheet.setCustomView(scroll);
    }

    void show() {
        sheet.show();
    }

    @Nullable
    private static CharSequence note(@Nullable CharSequence rarity, @Nullable String sourceGiftTitle) {
        final boolean hasRarity = rarity != null && rarity.length() > 0;
        final boolean hasSource = sourceGiftTitle != null && !sourceGiftTitle.isEmpty();
        if (hasRarity && hasSource) {
            return rarity + " • " + DemoText.fromGift(sourceGiftTitle);
        } else if (hasRarity) {
            return rarity;
        } else if (hasSource) {
            return DemoText.fromGift(sourceGiftTitle);
        }
        return null;
    }

    private static void addRow(LinearLayout root, Context context, Theme.ResourcesProvider resourcesProvider, String label, String value, @Nullable CharSequence note) {
        final LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);

        final LinearLayout line = new LinearLayout(context);
        line.setOrientation(LinearLayout.HORIZONTAL);

        final TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        labelView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        line.addView(labelView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        final TextView valueView = new TextView(context);
        valueView.setText(value);
        valueView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        valueView.setTypeface(AndroidUtilities.bold());
        valueView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        valueView.setGravity(Gravity.RIGHT);
        line.addView(valueView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));
        row.addView(line, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        if (note != null && note.length() > 0) {
            final TextView noteView = new TextView(context);
            noteView.setText(note);
            noteView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            noteView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            noteView.setGravity(Gravity.RIGHT);
            row.addView(noteView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT, 0, 2, 0, 0));
        }

        root.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 20, 12, 20, 0));
    }
}
