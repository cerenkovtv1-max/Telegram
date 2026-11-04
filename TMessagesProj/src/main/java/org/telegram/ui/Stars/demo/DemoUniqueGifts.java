package org.telegram.ui.Stars.demo;

import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.Stars.StarGiftPreviewSheet;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Builds the in-memory collectible that the DEMO shows with the stock renderer (the gift screen's result page and
 * roll). The object never leaves the device: it is not serialized to any request and has no slug or link.
 */
final class DemoUniqueGifts {

    /** Total shown for a base gift that has no limited supply of its own. */
    private static final int DEFAULT_TOTAL = 100_000;

    private DemoUniqueGifts() {
    }

    /**
     * @param base   the base gift the user picked (its title is the collectible's name)
     * @param traits the picked model / symbol / backdrop (all of them belong to {@code base})
     * @param serial the local DEMO counter value; only makes the result's id unique, it is not shown
     * @param userId the user the card is shown for, so it looks like the original owner page (buttons and owner row)
     */
    static TL_stars.TL_starGiftUnique build(TL_stars.StarGift base, StarGiftPreviewSheet.Attributes traits, int serial, long userId) {
        final TL_stars.TL_starGiftUnique unique = new TL_stars.TL_starGiftUnique();
        // Negative id: cannot collide with, or be mistaken for, a real gift id; distinct per result so the roll plays again.
        unique.id = -(long) serial;
        unique.gift_id = base.id;
        unique.title = base.title;

        // A random number and issued count within the supply: local demo data, not a real gift number.
        final ThreadLocalRandom random = ThreadLocalRandom.current();
        final int total = base.availability_total > 0 ? base.availability_total : DEFAULT_TOTAL;
        final int number = 1 + random.nextInt(total);
        unique.num = number;
        unique.availability_total = total;
        unique.availability_issued = number + random.nextInt(total - number + 1);

        // Shown like the original owner page. Every action behind its buttons is blocked in DEMO mode.
        final TLRPC.TL_peerUser owner = new TLRPC.TL_peerUser();
        owner.user_id = userId;
        unique.owner_id = owner;

        // The model first: the stock renderer takes the artwork from the first model attribute.
        unique.attributes.add(traits.model);
        unique.attributes.add(traits.pattern);
        unique.attributes.add(traits.backdrop);
        return unique;
    }

    /** Per-gift craft chance range, permille: four gifts add up to 100% at most, as the stock screen expects. */
    private static final int CHANCE_MIN_PERMILLE = 80;
    private static final int CHANCE_MAX_PERMILLE = 250;

    /**
     * What one gift adds to the success chance of a craft (the "+N%" chip on the gift and the sum on the craft screen).
     * In the original the server sends this number per gift and the client only adds them up; the server's formula is not
     * in the app, so this is an approximation: the rarer the gift's model, symbol and backdrop, the more it adds
     * ({@code 8%} for the most common up to {@code 25%} for the rarest).
     */
    static int craftChancePermille(StarGiftPreviewSheet.Attributes traits) {
        double score = 0;
        final TL_stars.StarGiftAttribute[] parts = {traits.model, traits.pattern, traits.backdrop};
        for (TL_stars.StarGiftAttribute part : parts) {
            if (part != null) {
                score += 10.0 / Math.max(1, Math.min(100, part.getRarityPermille()));
            }
        }
        final double share = 1.0 - Math.exp(-score / 6.0);
        return (int) Math.round(CHANCE_MIN_PERMILLE + (CHANCE_MAX_PERMILLE - CHANCE_MIN_PERMILLE) * share);
    }

    /** A collectible to put into a DEMO craft: same as {@link #build}, plus the per-gift craft chance the craft screen shows. */
    static TL_stars.TL_starGiftUnique buildSlot(TL_stars.StarGift base, StarGiftPreviewSheet.Attributes traits, int serial, long userId) {
        final TL_stars.TL_starGiftUnique unique = build(base, traits, serial, userId);
        unique.craft_chance_permille = craftChancePermille(traits);
        return unique;
    }

    /** The collectible a successful DEMO craft gives: {@link #build}, marked as crafted (the stock "crafted" ribbon). */
    static TL_stars.TL_starGiftUnique buildCrafted(TL_stars.StarGift base, StarGiftPreviewSheet.Attributes traits, int serial, long userId) {
        final TL_stars.TL_starGiftUnique unique = build(base, traits, serial, userId);
        unique.crafted = true;
        return unique;
    }

    static DemoGiftStore.Record toRecord(TL_stars.StarGift base, StarGiftPreviewSheet.Attributes traits, TL_stars.TL_starGiftUnique unique) {
        final DemoGiftStore.Record record = new DemoGiftStore.Record();
        record.serial = (int) -unique.id;
        record.number = unique.num;
        record.createdAt = System.currentTimeMillis();
        record.giftId = base.id;
        record.giftTitle = DemoText.giftLabel(base);
        record.modelName = traits.model.name;
        record.modelDocumentId = traits.model.document != null ? traits.model.document.id : 0;
        record.patternName = traits.pattern.name;
        record.patternDocumentId = traits.pattern.document != null ? traits.pattern.document.id : 0;
        record.backdropName = traits.backdrop.name;
        record.backdropId = traits.backdrop.backdrop_id;
        record.backdropCenter = traits.backdrop.center_color;
        record.backdropEdge = traits.backdrop.edge_color;
        record.backdropPattern = traits.backdrop.pattern_color;
        record.backdropText = traits.backdrop.text_color;
        return record;
    }
}
