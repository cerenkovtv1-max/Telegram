package org.telegram.ui.Stars.demo;

import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.Stars.StarGiftPreviewSheet;

/**
 * Builds the in-memory collectible that the DEMO shows with the stock renderer ({@code StarGiftSheet.TopView}).
 * The object never leaves the device: it is not serialized to any request and has no owner, slug or link.
 */
final class DemoUniqueGifts {

    private DemoUniqueGifts() {
    }

    /**
     * @param base   the base gift the user picked (its title is the collectible's name)
     * @param traits the independently picked model / symbol / backdrop
     * @param serial the local DEMO counter value, shown as "#serial"
     */
    static TL_stars.TL_starGiftUnique build(TL_stars.StarGift base, StarGiftPreviewSheet.Attributes traits, int serial) {
        final TL_stars.TL_starGiftUnique unique = new TL_stars.TL_starGiftUnique();
        // Negative id: cannot collide with, or be mistaken for, a real gift id.
        unique.id = -(long) serial;
        unique.gift_id = base.id;
        unique.title = base.title;
        unique.num = serial;
        unique.attributes.add(traits.model);
        unique.attributes.add(traits.pattern);
        unique.attributes.add(traits.backdrop);
        return unique;
    }

    static DemoGiftStore.Record toRecord(DemoGiftCatalog catalog, TL_stars.StarGift base, StarGiftPreviewSheet.Attributes traits, int serial) {
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
        record.backdropBuiltIn = catalog.isBuiltInOnly(traits.backdrop);
        record.backdropCenter = traits.backdrop.center_color;
        record.backdropEdge = traits.backdrop.edge_color;
        record.backdropPattern = traits.backdrop.pattern_color;
        record.backdropText = traits.backdrop.text_color;
        return record;
    }
}
