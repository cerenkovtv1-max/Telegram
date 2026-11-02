package org.telegram.ui.Stars.demo;

/**
 * Local DEMO gift editor ("DEMO Upgrade").
 *
 * <p>The DEMO is a visual simulator. It never spends Stars, never sends an upgrade / transfer / sell / convert / craft /
 * payment request, never changes real gifts, the profile, the balance or any server or blockchain state, and its
 * results are kept in their own local storage ({@link DemoGiftStore}), separate from real gifts. The only network
 * traffic it causes is the stock read-only loading of the gift list and of each gift's attribute preview
 * ({@code payments.getStarGifts}, {@code payments.getStarGiftUpgradePreview}), which the regular upgrade screen already does.
 */
public final class DemoGiftConfig {

    private DemoGiftConfig() {
    }

    /** Master switch. When false the DEMO entry point is not shown and the gift screen behaves exactly as stock. */
    public static final boolean ENABLED = true;
}
