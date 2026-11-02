package org.telegram.ui.Stars.demo;

import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.Stars.StarGiftPreviewSheet;

/**
 * Builds the in-memory collectible that the DEMO shows with the stock renderer (the gift screen's result page and
 * roll). The object never leaves the device: it is not serialized to any request and has no slug or link.
 */
final class DemoUniqueGifts {

    private DemoUniqueGifts() {
    }

    /**
     * @param base   the base gift the user picked (its title is the collectible's name)
     * @param traits the picked model / symbol / backdrop (all of them belong to {@code base})
     * @param serial the local DEMO counter value, shown as "#serial" - not a real gift number
     * @param userId the user the card is shown for, so it looks like the original owner page (buttons and owner row)
     */
    static TL_stars.TL_starGiftUnique build(TL_stars.StarGift base, StarGiftPreviewSheet.Attributes traits, int serial, long userId) {
        final TL_stars.TL_starGiftUnique unique = new TL_stars.TL_starGiftUnique();
        // Negative id: cannot collide with, or be mistaken for, a real gift id; distinct per result so the roll plays again.
        unique.id = -(long) serial;
        unique.gift_id = base.id;
        unique.title = base.title;
        unique.num = serial;
        // Shown like the original owner page. Every action behind its buttons is blocked in DEMO mode.
        final TLRPC.TL_peerUser owner = new TLRPC.TL_peerUser();
        owner.user_id = userId;
        unique.owner_id = owner;
        unique.availability_issued = serial;
        unique.availability_total = Math.max(base.availability_total, serial);
        // The model first: the stock renderer takes the artwork from the first model attribute.
        unique.attributes.add(traits.model);
        unique.attributes.add(traits.pattern);
        unique.attributes.add(traits.backdrop);
        return unique;
    }

    static DemoGiftStore.Record toRecord(TL_stars.StarGift base, StarGiftPreviewSheet.Attributes traits, int serial) {
        final DemoGiftStore.Record record = new DemoGiftStore.Record();
        record.serial = serial;
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
