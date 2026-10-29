package org.telegram.ui.Gifts;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

/**
 * State of the "Demo gifts" screen (DemoGiftsFragment): a demo Stars balance and the level of each demo gift.
 * Everything lives in local SharedPreferences. Nothing here talks to the Telegram account, the Stars balance
 * or the server.
 */
public class DemoGiftsStore {

    public static final long START_BALANCE = 500;
    public static final long MAX_BALANCE = 1_000_000;

    public static final String[] LEVEL_NAMES = { "Обычный", "Необычный", "Редкий", "Эпический", "Легендарный" };
    public static final int MAX_LEVEL = LEVEL_NAMES.length - 1;

    public static final class Gift {
        public final String id;
        public final String name;
        public final String emoji;
        public final int baseCost;

        private Gift(String id, String name, String emoji, int baseCost) {
            this.id = id;
            this.name = name;
            this.emoji = emoji;
            this.baseCost = baseCost;
        }

        /** Demo Stars needed to go from {@code level} to {@code level + 1}: each step costs twice as much as the previous one. */
        public long upgradeCost(int level) {
            return (long) baseCost << level;
        }
    }

    public static final Gift[] GIFTS = {
        new Gift("fox", "Плюшевый лис", "🦊", 25),
        new Gift("cup", "Золотой кубок", "🏆", 40),
        new Gift("candle", "Тёплая свеча", "🕯️", 15),
        new Gift("gem", "Кристалл", "💎", 60),
        new Gift("rocket", "Ракета", "🚀", 35),
        new Gift("clover", "Клевер удачи", "🍀", 20),
        new Gift("hat", "Цилиндр фокусника", "🎩", 30),
        new Gift("crystal_ball", "Хрустальный шар", "🔮", 45),
        new Gift("rose", "Роза", "🌹", 25),
        new Gift("bear", "Мишка", "🧸", 20)
    };

    public enum Result {
        OK,
        NOT_ENOUGH,
        MAX_LEVEL
    }

    private static final String PREFS = "demo_gifts";
    private static final String KEY_BALANCE = "balance";
    private static final String KEY_LEVEL = "level_";

    private DemoGiftsStore() {}

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static long getBalance() {
        return prefs().getLong(KEY_BALANCE, START_BALANCE);
    }

    /** Changes the demo balance; it never goes below 0 or above {@link #MAX_BALANCE}. */
    public static void addBalance(long delta) {
        final long balance = Math.max(0, Math.min(MAX_BALANCE, getBalance() + delta));
        prefs().edit().putLong(KEY_BALANCE, balance).apply();
    }

    public static int getLevel(Gift gift) {
        return Math.max(0, Math.min(MAX_LEVEL, prefs().getInt(KEY_LEVEL + gift.id, 0)));
    }

    /** Spends demo Stars and raises the level by one, if the demo balance is enough. */
    public static Result upgrade(Gift gift) {
        final int level = getLevel(gift);
        if (level >= MAX_LEVEL) {
            return Result.MAX_LEVEL;
        }
        final long cost = gift.upgradeCost(level);
        final long balance = getBalance();
        if (balance < cost) {
            return Result.NOT_ENOUGH;
        }
        prefs().edit()
            .putLong(KEY_BALANCE, balance - cost)
            .putInt(KEY_LEVEL + gift.id, level + 1)
            .apply();
        return Result.OK;
    }

    /** Back to the starting demo balance and level 0 for every gift. */
    public static void reset() {
        prefs().edit().clear().apply();
    }
}
