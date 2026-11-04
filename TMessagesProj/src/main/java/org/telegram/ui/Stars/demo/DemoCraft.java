package org.telegram.ui.Stars.demo;

import android.content.Context;

import org.telegram.ui.ActionBar.Theme;

/**
 * Entry point of the local DEMO craft ("DEMO Craft"). Opened from the gift screen's menu, next to "DEMO Upgrade".
 *
 * <p>Flow: pick a gift collection ({@link DemoUpgrade#pick}) -> pick the result of the craft (model / backdrop / symbol of
 * that collection) -> pick the outcome (success or failure) -> {@link DemoCraftSession}: the real gift screen's craft page,
 * where the gifts to put in are defined by hand. Nothing is sent: no craft request, nothing is spent or burned.
 */
public final class DemoCraft {

    private DemoCraft() {
    }

    /** The label of the entry in the gift screen's menu. */
    public static String menuTitle() {
        return DemoText.menuCraft();
    }

    public static void start(Context context, int account, Theme.ResourcesProvider resourcesProvider) {
        DemoUpgrade.pick(context, account, resourcesProvider, (gift, traits) -> new DemoCraftSession(context, account, resourcesProvider, gift, traits).start());
    }
}
