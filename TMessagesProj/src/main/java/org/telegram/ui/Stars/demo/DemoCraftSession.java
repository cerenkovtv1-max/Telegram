package org.telegram.ui.Stars.demo;

import android.content.Context;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Stars.StarGiftPreviewSheet;
import org.telegram.ui.Stars.StarGiftSheet;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * One run of the DEMO craft for one gift collection: the real gift screen ({@link StarGiftSheet}) in DEMO mode, opened
 * straight on its stock craft page (four slots, success chance, the cube animation, the "Crafting Failed" screen).
 *
 * <p>The user decides everything the server would: which gifts go in (each "+" slot: a random gift of the collection or a
 * hand-picked model / backdrop / symbol), what comes out (the picked result traits) and whether the craft succeeds. The
 * gifts are local objects that never leave the device and nothing is burned or created for real.
 */
final class DemoCraftSession implements DemoSheetHost, DemoCraftHost {

    private final Context context;
    private final int account;
    private final Theme.ResourcesProvider resourcesProvider;
    private final TL_stars.StarGift gift;
    private final DemoGiftCatalog.GiftTraits traits;

    /** What a successful craft gives. */
    private StarGiftPreviewSheet.Attributes result;
    private static final int OUTCOME_SUCCESS = 0;
    private static final int OUTCOME_FAILURE = 1;
    /** Random, using the success chance the craft screen shows (by the number of gifts, see DemoCraftChances). */
    private static final int OUTCOME_BY_CHANCE = 2;
    private int outcome = OUTCOME_SUCCESS;

    /** How many local gifts the "+" sheet offers at a time. */
    private static final int OFFERED_GIFTS = 12;
    /** The gifts of the last craft, put back into the slots by "Repeat". */
    private final ArrayList<TL_stars.StarGift> lastSlots = new ArrayList<>();

    private StarGiftSheet sheet;
    private boolean closed;

    DemoCraftSession(Context context, int account, Theme.ResourcesProvider resourcesProvider, TL_stars.StarGift gift, DemoGiftCatalog.GiftTraits traits) {
        this.context = context;
        this.account = account;
        this.resourcesProvider = resourcesProvider;
        this.gift = gift;
        this.traits = traits;
    }

    private long selfId() {
        return UserConfig.getInstance(account).getClientUserId();
    }

    // ------------------------------------------------------------------------------------------------ set-up steps

    /** Step 1 of 2: what the craft gives. Step 2: its outcome. Then the craft screen opens. */
    void start() {
        new DemoGiftEditorSheet(context, resourcesProvider, account, gift, traits, result != null ? result : traits.defaults(), DemoText.purposeCraftResult(), picked -> {
            result = picked;
            askOutcome();
        }).show();
    }

    private void askOutcome() {
        new AlertDialog.Builder(context, resourcesProvider)
            .setTitle(DemoText.craftOutcomeTitle())
            .setItems(new CharSequence[]{DemoText.craftOutcomeSuccess(), DemoText.craftOutcomeFail(), DemoText.craftOutcomeByChance()}, (dialog, which) -> {
                outcome = which == 1 ? OUTCOME_FAILURE : which == 2 ? OUTCOME_BY_CHANCE : OUTCOME_SUCCESS;
                open();
            })
            .show();
    }

    /** Opens the real gift screen on a local copy of the collection's base gift and goes straight to its craft page. */
    private void open() {
        closed = false;
        sheet = new StarGiftSheet(context, account, selfId(), resourcesProvider);
        sheet.setDemoHost(this, gift);
        sheet.set(DemoSession.savedBaseGift(account, gift), null);
        sheet.show();
        final StarGiftSheet opened = sheet;
        AndroidUtilities.runOnUIThread(() -> {
            if (!closed) {
                opened.demoOpenCrafting(this, gift, new ArrayList<>(lastSlots));
            }
        });
    }

    // ------------------------------------------------------------------------------------------------ DemoCraftHost

    @Override
    public void onAddCraftGift(Utilities.Callback<TL_stars.StarGift> onGift, int taken) {
        new DemoCraftGiftSheet(context, resourcesProvider, account, taken, () -> rollOffer(taken), picked -> {
            if (!closed) {
                onGift.run(picked);
            }
        }, () -> new DemoGiftEditorSheet(context, resourcesProvider, account, gift, traits, randomTraits(), DemoText.purposeCraftInput(), attributes -> {
            if (!closed && attributes != null) {
                onGift.run(newSlotGift(attributes, taken));
            }
        }).show()).show();
    }

    /** A fresh set of local gifts of the collection (random model, backdrop and symbol each) for the "+" sheet. */
    private ArrayList<TL_stars.StarGift> rollOffer(int taken) {
        final ArrayList<TL_stars.StarGift> offer = new ArrayList<>();
        for (int i = 0; i < OFFERED_GIFTS; ++i) {
            offer.add(newSlotGift(randomTraits(), taken));
        }
        return offer;
    }

    /** A local gift whose "+N%" chip is what one more gift adds when {@code taken} slots are filled (the same for every gift). */
    private TL_stars.StarGift newSlotGift(StarGiftPreviewSheet.Attributes attributes, int taken) {
        return DemoUniqueGifts.buildSlot(gift, attributes, DemoGiftStore.get().nextSerial(), selfId(), DemoCraftChances.addedPermille(taken));
    }

    /** A random model, backdrop and symbol, all of this collection. */
    private StarGiftPreviewSheet.Attributes randomTraits() {
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        return new StarGiftPreviewSheet.Attributes(
            traits.backdrops.get(random.nextInt(traits.backdrops.size())),
            traits.patterns.get(random.nextInt(traits.patterns.size())),
            traits.models.get(random.nextInt(traits.models.size()))
        );
    }

    @Override
    public TL_stars.TL_starGiftUnique onCraft(ArrayList<TL_stars.StarGift> gifts) {
        lastSlots.clear();
        lastSlots.addAll(gifts);
        if (result == null || result.model == null || result.pattern == null || result.backdrop == null) {
            return null;
        }
        final boolean success;
        if (outcome == OUTCOME_BY_CHANCE) {
            // the same number the craft screen shows (by the number of gifts), in permille
            success = ThreadLocalRandom.current().nextInt(1000) < DemoCraftChances.successPermille(gifts.size());
        } else {
            success = outcome == OUTCOME_SUCCESS;
        }
        if (!success) {
            return null; // a failed craft: the stock "Crafting Failed" screen
        }
        final DemoGiftStore store = DemoGiftStore.get();
        final TL_stars.TL_starGiftUnique unique = DemoUniqueGifts.buildCrafted(gift, result, store.nextSerial(), selfId());
        store.add(DemoUniqueGifts.toRecord(gift, result, unique));
        return unique;
    }

    // ------------------------------------------------------------------------------------------------ DemoSheetHost

    @Override
    public void onUpgradePressed(StarGiftSheet s) {
        // the base-gift page is never shown in the craft: the sheet opens on the craft page
    }

    @Override
    public TL_stars.TL_starGiftUnique onConfirmUpgrade() {
        return null;
    }

    /** "Edit": back to the choice of the result and the outcome, on a fresh screen. */
    @Override
    public void onEdit(StarGiftSheet s) {
        s.dismiss();
        AndroidUtilities.runOnUIThread(this::start, 250);
    }

    /** "Repeat": the craft page again, with the same gifts in the slots, the same result and the same outcome. */
    @Override
    public void onRepeat(StarGiftSheet s) {
        s.demoRestoreBaseGift();
        s.demoOpenCrafting(this, gift, new ArrayList<>(lastSlots));
    }

    @Override
    public void onPickAnotherGift(StarGiftSheet s) {
        s.dismiss();
        AndroidUtilities.runOnUIThread(() -> DemoCraft.start(context, account, resourcesProvider), 250);
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
