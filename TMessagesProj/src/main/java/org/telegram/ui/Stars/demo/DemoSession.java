package org.telegram.ui.Stars.demo;

import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Stars.StarGiftPreviewSheet;
import org.telegram.ui.Stars.StarGiftSheet;

import java.util.ArrayList;

/**
 * One run of the DEMO for one gift: the real gift screen ({@link StarGiftSheet}) in DEMO mode, with this class as its
 * host ({@link DemoSheetHost}).
 *
 * <p>What the user sees is the original sequence: the "Saved Gift" page of the base gift -> "Upgrade" -> (DEMO only: the
 * model / backdrop / symbol picker of THIS gift) -> the stock "Upgrade Gift" page showing the picked traits ->
 * "Upgrade (DEMO)" -> the stock roll, table and result page. The base gift object is a local copy that never leaves the
 * device; the sheet's server steps are replaced by the calls below, which send nothing.
 */
final class DemoSession implements DemoSheetHost {

    private static final long REPEAT_DELAY_MS = 800;

    private final Context context;
    private final int account;
    private final Theme.ResourcesProvider resourcesProvider;
    private final TL_stars.StarGift gift;
    private final DemoGiftCatalog.GiftTraits traits;

    private StarGiftPreviewSheet.Attributes chosen;
    private StarGiftSheet sheet;
    private boolean closed;

    DemoSession(Context context, int account, Theme.ResourcesProvider resourcesProvider, TL_stars.StarGift gift, DemoGiftCatalog.GiftTraits traits) {
        this.context = context;
        this.account = account;
        this.resourcesProvider = resourcesProvider;
        this.gift = gift;
        this.traits = traits;
        this.chosen = traits.defaults();
    }

    /** A local copy of an upgradable gift the user owns: exactly the data the original "Saved Gift" page reads. */
    static TL_stars.TL_savedStarGift savedBaseGift(int account, TL_stars.StarGift gift) {
        final long selfId = UserConfig.getInstance(account).getClientUserId();
        final TL_stars.TL_savedStarGift saved = new TL_stars.TL_savedStarGift();
        saved.gift = gift;
        saved.date = ConnectionsManager.getInstance(account).getCurrentTime();
        saved.can_upgrade = true;
        saved.upgrade_stars = 0;
        final TLRPC.TL_peerUser from = new TLRPC.TL_peerUser();
        from.user_id = selfId;
        saved.from_id = from;
        saved.flags |= 2; // "from" present: an own gift, which gives the same page as the original
        return saved;
    }

    /** Opens the real gift screen on a local "saved gift" of the picked base gift. */
    void open() {
        final long selfId = UserConfig.getInstance(account).getClientUserId();
        sheet = new StarGiftSheet(context, account, selfId, resourcesProvider);
        sheet.setDemoHost(this, gift);
        sheet.set(savedBaseGift(account, gift), null);
        sheet.show();
    }

    private void openPicker() {
        new DemoGiftEditorSheet(context, resourcesProvider, account, gift, traits, chosen, picked -> {
            chosen = picked;
            if (!closed && sheet != null) {
                sheet.demoShowUpgradePage(picked, traits.rollPool());
            }
        }).show();
    }

    // ------------------------------------------------------------------------------------------------ DemoSheetHost

    @Override
    public void onUpgradePressed(StarGiftSheet s) {
        openPicker();
    }

    @Override
    public TL_stars.TL_starGiftUnique onConfirmUpgrade() {
        if (chosen == null || chosen.model == null || chosen.pattern == null || chosen.backdrop == null) {
            return null;
        }
        final DemoGiftStore store = DemoGiftStore.get();
        final int serial = store.nextSerial();
        final TL_stars.TL_starGiftUnique unique = DemoUniqueGifts.build(gift, chosen, serial, UserConfig.getInstance(account).getClientUserId());
        store.add(DemoUniqueGifts.toRecord(gift, chosen, unique));
        return unique;
    }

    @Override
    public void onEdit(StarGiftSheet s) {
        s.demoRestoreBaseGift();
        openPicker();
    }

    @Override
    public void onRepeat(StarGiftSheet s) {
        s.demoRestoreBaseGift();
        s.demoShowUpgradePage(chosen, traits.rollPool());
        // the stock page is shown first, then the upgrade runs by itself (same as pressing the button)
        AndroidUtilities.runOnUIThread(() -> {
            if (!closed) {
                s.demoRunUpgrade();
            }
        }, REPEAT_DELAY_MS);
    }

    @Override
    public void onPickAnotherGift(StarGiftSheet s) {
        s.dismiss();
        AndroidUtilities.runOnUIThread(() -> DemoUpgrade.start(context, account, resourcesProvider), 250);
    }

    @Override
    public ArrayList<TL_stars.StarGiftAttribute> allVariants() {
        return traits.all;
    }

    @Override
    public void onDismissed() {
        closed = true;
    }
}
