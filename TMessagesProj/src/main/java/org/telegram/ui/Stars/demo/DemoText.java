package org.telegram.ui.Stars.demo;

import org.telegram.messenger.LocaleController;
import org.telegram.tgnet.tl.TL_stars;

import java.util.Locale;

/**
 * User-visible strings of the local DEMO editor. Russian unless the app language is English (the DEMO is a recording
 * tool, so its labels are not part of the translated stock string table).
 */
final class DemoText {

    private DemoText() {
    }

    private static boolean english() {
        try {
            final Locale locale = LocaleController.getInstance().getCurrentLocale();
            return locale != null && "en".equalsIgnoreCase(locale.getLanguage());
        } catch (Throwable ignore) {
            return false;
        }
    }

    private static String t(String ru, String en) {
        return english() ? en : ru;
    }

    static String menuItem() {
        return "DEMO Upgrade";
    }

    static String loadingCatalog() {
        return t("Загрузка каталога…", "Loading catalog…");
    }

    static String catalogUnavailable() {
        return t(
            "Каталог недоступен: сервер не отдал подарки или их атрибуты. Войдите в аккаунт, проверьте сеть и повторите.",
            "Catalog unavailable: the server returned no gifts or attributes. Sign in, check the network and try again."
        );
    }

    static String pickGiftTitle() {
        return t("DEMO: выберите подарок", "DEMO: choose a gift");
    }

    static String search() {
        return t("Поиск", "Search");
    }

    static String upgrade() {
        return t("Улучшить", "Upgrade");
    }

    static String upgradeDemo() {
        return t("Улучшить (DEMO)", "Upgrade (DEMO)");
    }

    static String edit() {
        return t("Изменить", "Edit");
    }

    static String repeat() {
        return t("Повторить", "Repeat");
    }

    static String pickAnotherGift() {
        return t("Выбрать другой подарок", "Choose another gift");
    }

    static String statusSelected() {
        return t("DEMO • выбрано вручную", "DEMO • picked manually");
    }

    static String demoBadge() {
        return "DEMO";
    }

    static String resultNotice() {
        return t(
            "DEMO • локальный результат. Это не настоящий NFT: Stars не списаны, подарки и аккаунт не изменены.",
            "DEMO • local result. This is not a real NFT: no Stars were spent, gifts and account are untouched."
        );
    }

    static String resultSubtitle() {
        return t("DEMO • локальный результат", "DEMO • local result");
    }

    static String savedLocally(int count) {
        return t(
            "Сохранено локально (отдельно от настоящих подарков): " + count,
            "Saved locally (separate from real gifts): " + count
        );
    }

    static String modelLabel() {
        return t("Модель", "Model");
    }

    static String backdropLabel() {
        return t("Фон", "Backdrop");
    }

    static String symbolLabel() {
        return t("Символ", "Symbol");
    }

    static String fromGift(String giftTitle) {
        return t("из «" + giftTitle + "»", "from “" + giftTitle + "”");
    }

    static String builtinBackdropNote() {
        return t("встроенный фон, серверный ID неизвестен", "built-in backdrop, server id unknown");
    }

    static String catalogLine(int gifts, int models, int backdrops, int symbols) {
        return t(
            "Подарков: " + gifts + " • моделей: " + models + " • фонов: " + backdrops + " • символов: " + symbols,
            "Gifts: " + gifts + " • models: " + models + " • backdrops: " + backdrops + " • symbols: " + symbols
        );
    }

    static String noUpgradeData(int count) {
        return t(
            "Без данных об атрибутах (нет улучшения или сервер не ответил): " + count,
            "No attribute data (not upgradable or no server reply): " + count
        );
    }

    static String giftsLoadFailed() {
        return t("Не удалось загрузить список подарков.", "Could not load the gift list.");
    }

    /** The name of a base gift as the server sent it; the numeric id only when the server sent no title. */
    static String giftLabel(TL_stars.StarGift gift) {
        if (gift != null && gift.title != null && !gift.title.isEmpty()) {
            return gift.title;
        }
        return gift == null ? "" : "#" + gift.id;
    }
}
