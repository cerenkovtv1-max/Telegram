package org.telegram.ui.Stars.demo;

import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.Stars.StarGiftSheet;

/**
 * What the real gift screen ({@link StarGiftSheet}) asks of the local DEMO when it runs in DEMO mode.
 *
 * <p>In DEMO mode the sheet keeps its own screens, transitions and roll animation, but every step that would talk to the
 * server (get the payment form, pay, receive the upgraded gift) is replaced by a call here, and every real action
 * (transfer, sell, convert, wear, show/hide, craft, ...) is blocked. Nothing in this interface sends a request.
 */
public interface DemoSheetHost {

    /** "Upgrade" was pressed on the base-gift page: let the user pick the model / backdrop / symbol. */
    void onUpgradePressed(StarGiftSheet sheet);

    /**
     * The final DEMO "Upgrade" button was pressed: return the locally built collectible that the roll should land on
     * (the host also records it in the separate DEMO store), or null if nothing can be built.
     */
    TL_stars.TL_starGiftUnique onConfirmUpgrade();

    /** Back to the picker with the same gift and the current choice. */
    void onEdit(StarGiftSheet sheet);

    /** Upgrade again with the same gift and traits. */
    void onRepeat(StarGiftSheet sheet);

    /** Back to the list of gifts. */
    void onPickAnotherGift(StarGiftSheet sheet);

    /** The sheet was closed. */
    void onDismissed();
}
