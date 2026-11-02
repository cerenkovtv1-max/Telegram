package org.telegram.ui.Stars.demo;

import static org.telegram.messenger.AndroidUtilities.dp;

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
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.SearchField;
import org.telegram.ui.Gifts.GiftSheet;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Step 1 of the DEMO: pick the base gift. A grid of the stock gift cards ({@link GiftSheet.GiftCell}, the same cell the
 * regular gift shop uses) with a search field. Every gift the client knows is listed; picking one only tells the DEMO
 * which name to put on the result, nothing is bought or sent.
 */
final class DemoGiftPickerSheet {

    interface OnPick {
        void onPick(TL_stars.StarGift gift);
    }

    private static final int COLUMNS = 3;
    private static final int CELL_HEIGHT_DP = 134;

    private final BottomSheet sheet;
    private final DemoGiftCatalog catalog;
    private final ArrayList<TL_stars.StarGift> shown = new ArrayList<>();
    private RecyclerListView listView;

    DemoGiftPickerSheet(Context context, Theme.ResourcesProvider resourcesProvider, int account, DemoGiftCatalog catalog, OnPick onPick) {
        this.catalog = catalog;
        this.shown.addAll(catalog.gifts);

        sheet = new BottomSheet(context, true, resourcesProvider);
        sheet.fixNavigationBar(Theme.getColor(Theme.key_dialogBackground, resourcesProvider));

        final LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);

        final TextView title = new TextView(context);
        title.setText(DemoText.pickGiftTitle());
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        title.setTypeface(AndroidUtilities.bold());
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack, resourcesProvider));
        title.setGravity(Gravity.CENTER);
        root.addView(title, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 18, 20, 4));

        final TextView counts = new TextView(context);
        counts.setText(DemoText.catalogLine(catalog.gifts.size(), catalog.models.size(), catalog.backdrops.size(), catalog.patterns.size()));
        counts.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        counts.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
        counts.setGravity(Gravity.CENTER);
        root.addView(counts, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 0, 20, 0));

        if (catalog.giftsNotUpgradable + catalog.giftsPreviewFailed > 0) {
            final TextView note = new TextView(context);
            note.setText(DemoText.noUpgradeData(catalog.giftsNotUpgradable + catalog.giftsPreviewFailed));
            note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            note.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, resourcesProvider));
            note.setGravity(Gravity.CENTER);
            root.addView(note, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER, 20, 2, 20, 0));
        }

        final SearchField search = new SearchField(context, false, resourcesProvider) {
            @Override
            public void onTextChange(String text) {
                applyFilter(text);
            }
        };
        search.setHint(DemoText.search());
        root.addView(search, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 58, Gravity.TOP, 0, 6, 0, 0));

        listView = new RecyclerListView(context, resourcesProvider);
        listView.setLayoutManager(new GridLayoutManager(context, COLUMNS));
        listView.setClipToPadding(false);
        listView.setPadding(dp(10), dp(4), dp(10), dp(16));
        listView.setAdapter(new GiftsAdapter(context, account, resourcesProvider));
        listView.setOnItemClickListener((view, position) -> {
            if (position < 0 || position >= shown.size()) {
                return;
            }
            final TL_stars.StarGift gift = shown.get(position);
            sheet.dismiss();
            onPick.onPick(gift);
        });
        // BottomSheet wraps its custom view, so the grid needs a definite height of its own.
        final int listHeight = Math.min(dp(520), (int) (AndroidUtilities.displaySize.y * 0.62f));
        root.addView(listView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, listHeight));

        sheet.setCustomView(root);
    }

    void show() {
        sheet.show();
    }

    private void applyFilter(String query) {
        final String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        shown.clear();
        for (TL_stars.StarGift gift : catalog.gifts) {
            if (q.isEmpty() || DemoText.giftLabel(gift).toLowerCase(Locale.ROOT).contains(q)) {
                shown.add(gift);
            }
        }
        if (listView != null && listView.getAdapter() != null) {
            listView.getAdapter().notifyDataSetChanged();
        }
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
            return shown.size();
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
            cell.setStarsGift(shown.get(position), false, false, false, false, false);
            cell.setSelected(false, false);
        }
    }
}
