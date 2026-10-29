package org.telegram.ui.Gifts;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.Text;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;

/**
 * "Demo gifts": a local sandbox to look at gift cards and try the upgrade flow with a demo Stars balance.
 * Opened from the debug menu (long-press the version row in Settings twice). It makes no server requests,
 * never touches the real Stars balance, real gifts or the account, and is marked "ДЕМО" everywhere.
 */
public class DemoGiftsFragment extends UniversalFragment {

    private static final int ID_BALANCE = 1;
    private static final int ID_ADD_100 = 2;
    private static final int ID_ADD_1000 = 3;
    private static final int ID_SUB_100 = 4;
    private static final int ID_RESET = 5;
    private static final int ID_GIFT_BASE = 100;

    private DemoBannerView banner;
    private DemoGiftCardView[] cards;

    @Override
    public View createView(Context context) {
        banner = new DemoBannerView(context);
        cards = new DemoGiftCardView[DemoGiftsStore.GIFTS.length];
        for (int i = 0; i < cards.length; i++) {
            cards[i] = new DemoGiftCardView(context);
        }
        return super.createView(context);
    }

    @Override
    protected CharSequence getTitle() {
        return "Демо подарков · ДЕМО";
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (banner == null || cards == null) {
            return;
        }
        items.add(UItem.asCustom(banner, 56));
        items.add(UItem.asShadow("ДЕМО-режим. Подарки, тестовые Stars и улучшения хранятся только на этом устройстве. Настоящий баланс Stars, настоящие подарки и аккаунт Telegram не затрагиваются, запросов к серверу нет."));

        items.add(UItem.asHeader("Тестовый баланс"));
        items.add(UItem.asButton(ID_BALANCE, "Баланс", DemoGiftsStore.getBalance() + " ⭐ (демо)"));
        items.add(UItem.asButton(ID_ADD_100, "Добавить 100 ⭐ (демо)").accent());
        items.add(UItem.asButton(ID_ADD_1000, "Добавить 1000 ⭐ (демо)").accent());
        items.add(UItem.asButton(ID_SUB_100, "Убрать 100 ⭐ (демо)"));
        items.add(UItem.asButton(ID_RESET, "Сбросить демо").red());
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader("Демо-подарки"));
        for (int i = 0; i < DemoGiftsStore.GIFTS.length; i++) {
            final DemoGiftsStore.Gift gift = DemoGiftsStore.GIFTS[i];
            final int level = DemoGiftsStore.getLevel(gift);
            cards[i].set(gift, level);
            items.add(UItem.asCustom(cards[i], 168));
            if (level >= DemoGiftsStore.MAX_LEVEL) {
                items.add(UItem.asButton(ID_GIFT_BASE + i, "Максимальный уровень"));
            } else {
                items.add(UItem.asButton(ID_GIFT_BASE + i, "Улучшить до «" + DemoGiftsStore.LEVEL_NAMES[level + 1] + "» за " + gift.upgradeCost(level) + " ⭐ (демо)").accent());
            }
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case ID_BALANCE:
                return;
            case ID_ADD_100:
                DemoGiftsStore.addBalance(100);
                refresh();
                return;
            case ID_ADD_1000:
                DemoGiftsStore.addBalance(1000);
                refresh();
                return;
            case ID_SUB_100:
                DemoGiftsStore.addBalance(-100);
                refresh();
                return;
            case ID_RESET:
                confirmReset();
                return;
            default:
                break;
        }
        final int index = item.id - ID_GIFT_BASE;
        if (index >= 0 && index < DemoGiftsStore.GIFTS.length) {
            confirmUpgrade(index);
        }
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }

    private void refresh() {
        if (listView != null && listView.adapter != null) {
            listView.adapter.update(true);
        }
    }

    private void confirmReset() {
        if (getParentActivity() == null) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle("ДЕМО: сброс");
        builder.setMessage("Вернуть тестовый баланс к " + DemoGiftsStore.START_BALANCE + " ⭐ и сбросить уровни всех демо-подарков? Это затрагивает только демо-данные на этом устройстве.");
        builder.setPositiveButton("Сбросить", (dialog, which) -> {
            DemoGiftsStore.reset();
            refresh();
        });
        builder.setNegativeButton("Отмена", null);
        showDialog(builder.create());
    }

    private void confirmUpgrade(int index) {
        if (getParentActivity() == null) {
            return;
        }
        final DemoGiftsStore.Gift gift = DemoGiftsStore.GIFTS[index];
        final int level = DemoGiftsStore.getLevel(gift);
        if (level >= DemoGiftsStore.MAX_LEVEL) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "ДЕМО: у подарка «" + gift.name + "» уже максимальный уровень").show();
            return;
        }
        final long cost = gift.upgradeCost(level);
        final long balance = DemoGiftsStore.getBalance();
        if (balance < cost) {
            BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "ДЕМО: не хватает тестовых Stars. Нужно " + cost + " ⭐, на демо-балансе " + balance + " ⭐. Добавьте тестовые Stars выше.").show();
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity(), getResourceProvider());
        builder.setTitle("ДЕМО: улучшение подарка");
        builder.setMessage("«" + gift.name + "»: " + DemoGiftsStore.LEVEL_NAMES[level] + " → " + DemoGiftsStore.LEVEL_NAMES[level + 1] + ".\nБудет списано " + cost + " тестовых ⭐ с демо-баланса. Настоящие Stars не тратятся, на сервер ничего не отправляется.");
        builder.setPositiveButton("Улучшить", (dialog, which) -> doUpgrade(index));
        builder.setNegativeButton("Отмена", null);
        showDialog(builder.create());
    }

    private void doUpgrade(int index) {
        final DemoGiftsStore.Gift gift = DemoGiftsStore.GIFTS[index];
        final DemoGiftsStore.Result result = DemoGiftsStore.upgrade(gift);
        switch (result) {
            case OK:
                refresh();
                if (cards != null) {
                    cards[index].pulse();
                    cards[index].performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.gift, "ДЕМО: «" + gift.name + "» улучшен до уровня «" + DemoGiftsStore.LEVEL_NAMES[DemoGiftsStore.getLevel(gift)] + "»").show();
                break;
            case NOT_ENOUGH:
                BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "ДЕМО: не хватает тестовых Stars. Добавьте их выше.").show();
                break;
            case MAX_LEVEL:
                BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "ДЕМО: у подарка «" + gift.name + "» уже максимальный уровень").show();
                break;
        }
    }

    /** Red strip at the top of the screen: this is a demo, nothing here is real. */
    private static class DemoBannerView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private final Text text = new Text("ДЕМО · тестовые подарки и тестовые Stars", 13, AndroidUtilities.bold());

        DemoBannerView(Context context) {
            super(context);
            paint.setColor(0xFFE0453A);
        }

        @Override
        protected void onDraw(@NonNull Canvas canvas) {
            rect.set(dp(16), dp(8), getWidth() - dp(16), getHeight() - dp(8));
            canvas.drawRoundRect(rect, dp(12), dp(12), paint);
            text.ellipsize(rect.width() - dp(16));
            text.draw(canvas, rect.centerX() - text.getCurrentWidth() / 2f, rect.centerY(), 0xFFFFFFFF, 1f);
        }
    }
}
