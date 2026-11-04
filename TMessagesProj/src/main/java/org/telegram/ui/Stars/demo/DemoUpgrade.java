package org.telegram.ui.Stars.demo;

import android.content.Context;
import android.widget.Toast;

import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;

/**
 * Entry point of the local DEMO gift editor ("DEMO Upgrade"). Opened from the gift screen's menu; completely separate
 * from the real upgrade flow ({@code StarGiftSheet.openUpgrade}), which it never reaches in DEMO mode.
 *
 * <p>Flow: load the gift list (read-only) -> {@link DemoGiftPickerSheet} (pick a gift) -> load that gift's own traits
 * (read-only) -> {@link DemoSession}: the real gift screen in DEMO mode.
 */
public final class DemoUpgrade {

    private DemoUpgrade() {
    }

    private static boolean loading;

    /** The label of the entry in the gift screen's menu. */
    public static String menuTitle() {
        return DemoText.menuItem();
    }

    /** What a flow gets once the user has picked a gift and that gift's own traits are loaded. */
    public interface OnGiftReady {
        void onReady(TL_stars.StarGift gift, DemoGiftCatalog.GiftTraits traits);
    }

    public static void start(Context context, int account, Theme.ResourcesProvider resourcesProvider) {
        pick(context, account, resourcesProvider, (gift, traits) -> new DemoSession(context, account, resourcesProvider, gift, traits).open());
    }

    /**
     * Load the gift list (read-only), let the user pick a gift in {@link DemoGiftPickerSheet}, load that gift's own
     * traits (read-only), close the picker and hand both to {@code onReady}. Shared by the DEMO upgrade and the DEMO craft.
     */
    public static void pick(Context context, int account, Theme.ResourcesProvider resourcesProvider, OnGiftReady onReady) {
        if (!DemoGiftConfig.ENABLED || context == null || loading) {
            return;
        }
        final DemoGiftCatalog catalog = DemoGiftCatalog.forAccount(account);
        if (catalog.isReady()) {
            openPicker(context, resourcesProvider, account, catalog, onReady);
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
        catalog.load(ready -> {
            loading = false;
            progress.dismiss();
            openPicker(context, resourcesProvider, account, ready, onReady);
        });
    }

    private static void openPicker(Context context, Theme.ResourcesProvider resourcesProvider, int account, DemoGiftCatalog catalog, OnGiftReady onReady) {
        if (catalog.gifts.isEmpty()) {
            Toast.makeText(context, DemoText.giftsLoadFailed(), Toast.LENGTH_LONG).show();
            return;
        }
        final DemoGiftPickerSheet[] picker = new DemoGiftPickerSheet[1];
        picker[0] = new DemoGiftPickerSheet(context, resourcesProvider, account, catalog, gift -> openGift(context, resourcesProvider, account, catalog, gift, () -> picker[0].dismiss(), onReady));
        picker[0].show();
    }

    /** Loads the picked gift's own traits, then hands the gift and its traits on. */
    private static void openGift(Context context, Theme.ResourcesProvider resourcesProvider, int account, DemoGiftCatalog catalog, TL_stars.StarGift gift, Runnable whenOpened, OnGiftReady onReady) {
        if (loading) {
            return;
        }
        loading = true;
        final AlertDialog progress = new AlertDialog(context, AlertDialog.ALERT_TYPE_SPINNER, resourcesProvider);
        progress.showDelayed(150);
        catalog.loadTraits(gift, traits -> {
            loading = false;
            progress.dismiss();
            if (traits == null || !traits.isUsable()) {
                // Nothing to show for this gift: say so, do not fall back to other gifts' traits.
                Toast.makeText(context, DemoText.noTraits(DemoText.giftLabel(gift)), Toast.LENGTH_LONG).show();
                return;
            }
            whenOpened.run();
            onReady.onReady(gift, traits);
        });
    }
}
