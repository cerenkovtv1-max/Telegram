package org.telegram.ui.Stars.demo;

/**
 * Success chance of a DEMO craft: the same for every gift and every collection, set only by how many gifts are put in.
 * (One gift 15%, two 45%, three 65%, four 85%.)
 */
public final class DemoCraftChances {

    private DemoCraftChances() {
    }

    /** Index = number of gifts in the craft (0..4), value = success chance in permille. */
    public static final int[] SUCCESS_PERMILLE = {0, 150, 450, 650, 850};

    /** The success chance of a craft with this many gifts, in permille. */
    public static int successPermille(int gifts) {
        return SUCCESS_PERMILLE[Math.max(0, Math.min(gifts, SUCCESS_PERMILLE.length - 1))];
    }

    /** What one more gift adds to the chance when {@code taken} slots are already filled: the "+N%" chip on the offered gifts. */
    public static int addedPermille(int taken) {
        final int from = Math.max(0, Math.min(taken, SUCCESS_PERMILLE.length - 2));
        return SUCCESS_PERMILLE[from + 1] - SUCCESS_PERMILLE[from];
    }
}
