package org.telegram.ui.Stars.demo;

import org.telegram.messenger.LocaleController;
import org.telegram.tgnet.tl.TL_stars;

import java.util.Locale;

/**
 * User-visible strings of the local DEMO editor. Russian unless the app language is English (the DEMO is a recording
 * tool, so its labels are not part of the translated stock string table).
 */
public final class DemoText {

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

    // ---- entry / catalog ----

    public static String menuItem() {
        return "DEMO Upgrade";
    }

    static String pickGiftTitle() {
        return t("DEMO: выберите подарок", "DEMO: choose a gift");
    }

    static String search() {
        return t("Поиск", "Search");
    }

    static String giftsLine(int gifts) {
        return t("Подарков с улучшением: " + gifts, "Upgradable gifts: " + gifts);
    }

    static String skippedGifts(int count) {
        return t(
            "Скрыто подарков, которые нельзя улучшить: " + count,
            "Hidden gifts that cannot be upgraded: " + count
        );
    }

    static String giftsLoadFailed() {
        return t("Не удалось загрузить список подарков.", "Could not load the gift list.");
    }

    static String noTraits(String giftTitle) {
        return t(
            "Для «" + giftTitle + "» сервер не отдал модели, фоны или узоры. Показывать нечего: выдумывать их я не буду.",
            "The server returned no models, backdrops or symbols for “" + giftTitle + "”. Nothing to show, and none are made up."
        );
    }

    // ---- wizard ----

    static String stepModel() {
        return t("Модель", "Model");
    }

    static String stepBackdrop() {
        return t("Фон", "Backdrop");
    }

    static String stepSymbol() {
        return t("Узор", "Symbol");
    }

    static String nextBackdrop() {
        return t("Далее: фон", "Next: backdrop");
    }

    static String nextSymbol() {
        return t("Далее: узор", "Next: symbol");
    }

    static String done() {
        return t("Готово", "Done");
    }

    static String statusStep(int step, String giftTitle, boolean partial) {
        return t(
            "DEMO • шаг " + (step + 1) + " из 3 • только варианты «" + giftTitle + "»" + (partial ? " • неполный список" : ""),
            "DEMO • step " + (step + 1) + " of 3 • only “" + giftTitle + "” variants" + (partial ? " • partial list" : "")
        );
    }

    // ---- confirm page (stock "Upgrade Gift" page) ----

    public static String upgradeButton() {
        return t("Улучшить", "Upgrade");
    }

    public static String viewAllVariants() {
        return t("Просмотреть все варианты >", "View all variants >");
    }

    public static String withMyName() {
        return t("С моим именем", "With my name");
    }

    // ---- result page ----

    public static String hiddenFromProfile() {
        return t("Подарок скрыт из Вашего профиля.", "This gift is hidden from your profile.");
    }

    public static String hiddenFromProfileWithLink() {
        return t("Подарок скрыт из профиля. **Показывать >**", "This gift is hidden from your profile. **Show >**");
    }

    public static String edit() {
        return t("Изменить", "Edit");
    }

    public static String repeat() {
        return t("Повторить", "Repeat");
    }

    public static String pickAnotherGift() {
        return t("Выбрать другой подарок", "Choose another gift");
    }

    public static String blocked() {
        return t("В DEMO это действие недоступно", "Not available in DEMO");
    }

    public static String demoBadge() {
        return "";
    }

    public static String upgradedBulletinTitle() {
        return t("", "");
    }

    public static String upgradedBulletinText() {
        return t(
            "",
            ""
        );
    }

    /** The name of a base gift as the server sent it; the numeric id only when the server sent no title. */
    public static String giftLabel(TL_stars.StarGift gift) {
        if (gift != null && gift.title != null && !gift.title.isEmpty()) {
            return gift.title;
        }
        return gift == null ? "" : "#" + gift.id;
    }
}
