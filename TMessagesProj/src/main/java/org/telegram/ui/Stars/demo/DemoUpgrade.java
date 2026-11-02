package org.telegram.ui.Stars.demo;

import android.content.Context;
import android.widget.Toast;

import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Stars.StarGiftPreviewSheet;

/**
 * Entry point of the local DEMO gift editor ("DEMO Upgrade"). Opened from the gift screen's menu; completely separate
 * from the real upgrade flow ({@code StarGiftSheet.openUpgrade}), which it never calls and whose requests it never builds.
 *
 * <p>Flow: load the catalog (read-only) -> {@link DemoGiftPickerSheet} (gift) -> {@link DemoGiftEditorSheet}
 * (model / backdrop / symbol, then "Upgrade") -> {@link DemoResultSheet} (Edit / Repeat / Choose another gift).
 */
public final class DemoUpgrade {

    private DemoUpgrade() {
    }

    private static boolean loading;

    /** The label of the entry in the gift screen's menu. */
    public static String menuTitle() {
        return DemoText.menuItem();
    }

    public static void start(Context context, int account, Theme.ResourcesProvider resourcesProvider) {
        if (!DemoGiftConfig.ENABLED || context == null || loading) {
            return;
        }
        final DemoGiftCatalog catalog = DemoGiftCatalog.forAccount(account);
        if (catalog.isReady()) {
            open(context, resourcesProvider, account, catalog);
            return;
        }

        loading = true;
        final AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER, resourcesProvider);
        progress.setCanCancel(true);
        progress.setOnCancelListener(dialog -> {
            catalog.cancel();
            loading = false;
        });
        progress.showDelayed(150);
        catalog.load(new DemoGiftCatalog.Listener() {
            @Override
            public void onProgress(int done, int total) {
                // the spinner is enough; the counts are shown on the picker
            }

            @Override
            public void onReady(DemoGiftCatalog ready) {
                loading = false;
                progress.dismiss();
                open(context, resourcesProvider, account, ready);
            }
        });
    }

    private static void open(Context context, Theme.ResourcesProvider resourcesProvider, int account, DemoGiftCatalog catalog) {
        if (catalog.gifts.isEmpty()) {
            Toast.makeText(context, DemoText.giftsLoadFailed(), Toast.LENGTH_LONG).show();
            return;
        }
        if (!catalog.isUsable()) {
            // Gifts are known but their models / symbols (server-delivered stickers) are not: say so, do not fake them.
            Toast.makeText(
                context,
                DemoText.catalogUnavailable() + "\n" + DemoText.catalogLine(catalog.gifts.size(), catalog.models.size(), catalog.backdrops.size(), catalog.patterns.size()),
                Toast.LENGTH_LONG
            ).show();
            return;
        }
        new DemoGiftPickerSheet(context, resourcesProvider, account, catalog, gift -> openEditor(context, resourcesProvider, account, catalog, gift)).show();
    }

    private static void openEditor(Context context, Theme.ResourcesProvider resourcesProvider, int account, DemoGiftCatalog catalog, TL_stars.StarGift gift) {
        final StarGiftPreviewSheet.Attributes initial = catalog.defaultsFor(gift);
        if (initial == null) {
            Toast.makeText(context, DemoText.catalogUnavailable(), Toast.LENGTH_LONG).show();
            return;
        }
        new DemoGiftEditorSheet(context, resourcesProvider, account, catalog, gift, initial).show();
    }
}
