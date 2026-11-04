package org.telegram.ui.Stars.demo;

import org.telegram.messenger.Utilities;
import org.telegram.tgnet.tl.TL_stars;

import java.util.ArrayList;

/**
 * What the real craft screen ({@code StarGiftSheet.CraftTopView}, driven by {@code StarGiftSheet.demoOpenCrafting}) asks
 * of the local DEMO. In the original both steps talk to the server (the list of the user's own gifts, and the craft
 * request); here they are answered locally and nothing is sent.
 */
public interface DemoCraftHost {

    /**
     * "+" on an empty slot: let the user define a gift to put into the craft and pass it to {@code onGift}
     * (never call it if the user cancels).
     *
     * @param taken how many slots are already filled
     */
    void onAddCraftGift(Utilities.Callback<TL_stars.StarGift> onGift, int taken);

    /**
     * The stock "Craft Gift" button was pressed with these gifts: return the collectible the craft gives, or null for a
     * failed craft (the stock "Crafting Failed" screen).
     */
    TL_stars.TL_starGiftUnique onCraft(ArrayList<TL_stars.StarGift> gifts);
}
