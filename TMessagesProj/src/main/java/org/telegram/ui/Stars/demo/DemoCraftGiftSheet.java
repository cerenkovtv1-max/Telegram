package org.telegram.ui.Stars.demo;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.LocaleController.formatPluralString;
import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Gifts.GiftSheet;

import java.util.ArrayList;

/**
 * The "+" slot of the DEMO craft: a sheet laid out like the original "Select Gift" screen (title, the "add up to N gifts
 * from the same collection" line, the "Your Gifts" header and a grid of the stock gift cards with the "+N%" craft-chance
 * chip), fed with local gifts of the collection instead of the user's own gifts. Two DEMO actions sit above the grid:
 * hand-pick a variant (model / backdrop / symbol) and roll a fresh set of gifts. Nothing is requested from the server.
 */
final class DemoCraftGiftSheet {

    interface Offer {
        /** A fresh set of local gifts of the collection to choose from. */
        ArrayList<TL_stars.StarGift> roll();
    }

    interface OnPick {
        void onPick(TL_stars.StarGift gift);
    }

    private static final int COLUMNS = 3;
    private static final int CELL_HEIGHT_DP = 134;

    private final BottomSheet sheet;
    private final Offer offer;
    private final ArrayList<TL_stars.StarGift> offered = new ArrayList<>();
    private RecyclerListView listView;

    DemoCraftGiftSheet(Context context, Theme.ResourcesProvider resourcesProvider, int account, int taken, Offer offer, OnPick onPick, Runnable onCustom) {
        this.offer = offer;
        this.offered.addAll(offer.roll());

        sheet = new BottomSheet(context, true, resourcesProvider);
        sheet.fixNavigationBar(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

        final LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        // the original wording (stock strings): "Select Gift" / "You need to add up to N gifts from the same collection..."
        final TextView title = new TextView(context);
        title.setText(getString(R.string.GiftCraftSelectTitle));
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        title.setGravity(Gravity.CENTER);
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 18, 20, 4));

        final TextView hint = new TextView(context);
        hint.setText(AndroidUtilities.replaceTags(formatPluralString("GiftCraftSelect", Math.max(1, 4 - taken))));
        hint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        hint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        hint.setGravity(Gravity.CENTER);
        root.addView(hint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 0, 20, 6));

        // DEMO actions
        final LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER);
        actions.addView(actionText(context, resourcesProvider, DemoText.craftAddPick(), () -> {
            sheet.dismiss();
            onCustom.run();
        }), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 8, 4, 8, 4));
        actions.addView(actionText(context, resourcesProvider, DemoText.craftSelectRefresh(), () -> {
            offered.clear();
            offered.addAll(this.offer.roll());
            if (listView != null && listView.getAdapter() != null) {
                listView.getAdapter().notifyDataSetChanged();
            }
        }), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 8, 4, 8, 4));
        root.addView(actions, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 0, 0, 0, 0));

        final TextView header = new TextView(context);
        header.setText(getString(R.string.GiftCraftSelectYour));
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        header.setTypeface(AndroidUtilities.bold());
        header.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader, resourcesProvider));
        root.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 22, 10, 22, 2));

        listView = new RecyclerListView(context, resourcesProvider);
        listView.setLayoutManager(new GridLayoutManager(context, COLUMNS));
        listView.setClipToPadding(false);
        listView.setPadding(dp(10), dp(4), dp(10), dp(16));
        listView.setAdapter(new GiftsAdapter(context, account, resourcesProvider));
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= offered.size()) {
                return;
            }
            final TL_stars.StarGift picked = offered.get(position);
            sheet.dismiss();
            onPick.onPick(picked);
        });
        // BottomSheet wraps its custom view, so the grid needs a definite height of its own.
        final int listHeight = Math.min(dp(440), (int) (AndroidUtilities.displaySize.y * 0.52f));
        root.addView(listView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, listHeight));

        sheet.setCustomView(root);
    }

    private static TextView actionText(Context context, Theme.ResourcesProvider resourcesProvider, CharSequence text, Runnable onClick) {
        final TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        view.setTypeface(AndroidUtilities.bold());
        view.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText4, resourcesProvider));
        view.setPadding(dp(10), dp(8), dp(10), dp(8));
        view.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector, resourcesProvider), Theme.RIPPLE_MASK_ALL));
        view.setOnClickListener(v -> onClick.run());
        return view;
    }

    void show() {
        sheet.show();
    }

    private final class GiftsAdapter extends RecyclerListView.SelectionAdapter {
        private final Context context;
        private final int account;
        private final Theme.ResourcesProvider resourcesProvider;

        GiftsAdapter(Context context, int account, Theme.ResourcesProvider resourcesProvider) {
            this.context = context;
            this.account = account;
            this.resourcesProvider = resourcesProvider;
        }

        @Override
        public boolean isEnabled(RecyclerView.ViewHolder holder) {
            return true;
        }

        @Override
        public int getItemCount() {
            return offered.size();
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final GiftSheet.GiftCell cell = new GiftSheet.GiftCell(context, account, resourcesProvider);
            cell.setLayoutParams(new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(CELL_HEIGHT_DP)));
            return new RecyclerListView.Holder(cell);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            final GiftSheet.GiftCell cell = (GiftSheet.GiftCell) holder.itemView;
            // the last flag is the original's "in crafting": the "+N%" chip with this gift's craft chance
            cell.setStarsGift(offered.get(position), false, false, false, false, true);
            cell.setSelected(false, false);
        }
    }
}
