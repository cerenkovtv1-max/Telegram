package org.telegram.ui.QrShare;

import java.util.ArrayList;

/**
 * Read-only view of the collectible-gift backdrop table that {@link Backdrops} keeps package-private, for the local
 * DEMO gift editor ({@code org.telegram.ui.Stars.demo}). Nothing here is written back and no request is made.
 *
 * <p>The table is either the built-in one (names + colours only, no server ids) or the palette the server last sent
 * ({@link Backdrops#IDS} != null). {@link Entry#serverId} is {@code null} exactly when no server id is known; ids are
 * never invented.
 */
public final class DemoBackdropsBridge {

    private DemoBackdropsBridge() {
    }

    public static final class Entry {
        public final String name;
        public final int center, edge, pattern, text;
        /** The server id of this backdrop, or null when only the built-in table (no ids) is available. */
        public final Integer serverId;

        Entry(String name, int center, int edge, int pattern, int text, Integer serverId) {
            this.name = name;
            this.center = center;
            this.edge = edge;
            this.pattern = pattern;
            this.text = text;
            this.serverId = serverId;
        }
    }

    /** A snapshot of the table as it is right now. */
    public static ArrayList<Entry> snapshot() {
        final String[] names = Backdrops.NAMES;
        final int[] center = Backdrops.CENTER;
        final int[] edge = Backdrops.EDGE;
        final int[] pattern = Backdrops.PATTERN;
        final int[] text = Backdrops.TEXT;
        final int[] ids = Backdrops.IDS;

        int n = names.length;
        n = Math.min(n, center.length);
        n = Math.min(n, edge.length);
        n = Math.min(n, pattern.length);
        n = Math.min(n, text.length);

        final ArrayList<Entry> result = new ArrayList<>(n);
        for (int i = 0; i < n; ++i) {
            final Integer serverId = ids != null && i < ids.length ? ids[i] : null;
            result.add(new Entry(names[i], center[i], edge[i], pattern[i], text[i], serverId));
        }
        return result;
    }
}
