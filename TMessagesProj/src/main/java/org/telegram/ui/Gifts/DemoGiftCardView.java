package org.telegram.ui.Gifts;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.view.View;

import androidx.annotation.NonNull;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.Text;
import org.telegram.ui.Stars.StarGiftPatterns;

/**
 * Card of a demo gift, drawn in the style of Telegram's collectible gift cards (gradient backdrop, symbol pattern,
 * corner ribbon). It is always marked "ДЕМО": by the ribbon and by a diagonal watermark that nothing turns off.
 */
public class DemoGiftCardView extends View {

    public static final String DEMO_LABEL = "ДЕМО";

    private static final int[][] BACKDROPS = {
        { 0xFF6B7A8F, 0xFF3C4858 }, // Обычный
        { 0xFF58B368, 0xFF2A7A44 }, // Необычный
        { 0xFF5B9BE8, 0xFF2D5FB5 }, // Редкий
        { 0xFF9B6BE0, 0xFF5A2FA8 }, // Эпический
        { 0xFFF2B63D, 0xFFC2661A }  // Легендарный
    };

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();
    private final RectF rect = new RectF();
    private final Path clipPath = new Path();
    private RadialGradient gradient;
    private float gradientSize;

    private final GlyphDrawable pattern = new GlyphDrawable();
    private final GiftSheet.RibbonDrawable ribbon;
    private final TextPaint emojiPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint watermarkFillPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint watermarkStrokePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);

    private String emoji = "";
    private int level;
    private Text title, subtitle;

    public DemoGiftCardView(Context context) {
        super(context);
        ribbon = new GiftSheet.RibbonDrawable(this, 1.0f);
        ribbon.setText(11, DEMO_LABEL, true);
        emojiPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void set(DemoGiftsStore.Gift gift, int level) {
        this.emoji = gift.emoji;
        this.level = Math.max(0, Math.min(DemoGiftsStore.MAX_LEVEL, level));
        this.title = new Text(gift.name, 14, AndroidUtilities.bold());
        this.subtitle = new Text(DemoGiftsStore.LEVEL_NAMES[this.level] + " · уровень " + this.level + " из " + DemoGiftsStore.MAX_LEVEL, 12);
        gradient = null;
        final int[] colors = BACKDROPS[this.level];
        ribbon.setColors(colors[0], colors[1]);
        invalidate();
    }

    /** A short "pop" after a successful upgrade. */
    public void pulse() {
        animate().cancel();
        animate().scaleX(1.05f).scaleY(1.05f).setDuration(140).withEndAction(() -> animate().scaleX(1f).scaleY(1f).setDuration(220).start()).start();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (title == null || subtitle == null) {
            return;
        }
        final int[] colors = BACKDROPS[level];
        rect.set(dp(16), dp(6), getWidth() - dp(16), getHeight() - dp(6));
        final float w = rect.width();
        final float h = rect.height();
        final float cx = rect.centerX();

        final float size = (w + h) / 2f;
        if (gradient == null || gradientSize != size) {
            gradient = new RadialGradient(0, 0, gradientSize = size, new int[] { colors[0], colors[1] }, new float[] { 0, 1 }, Shader.TileMode.CLAMP);
        }
        matrix.reset();
        matrix.postTranslate(cx, rect.top + dp(60));
        gradient.setLocalMatrix(matrix);
        backgroundPaint.setShader(gradient);

        clipPath.rewind();
        clipPath.addRoundRect(rect, dp(14), dp(14), Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clipPath);
        canvas.drawPaint(backgroundPaint);

        // symbol pattern around the gift, the same helper that draws real collectible gift cards
        canvas.save();
        canvas.translate(cx, rect.top + dp(60));
        StarGiftPatterns.drawPattern(canvas, StarGiftPatterns.TYPE_DEFAULT, pattern, w, h, 1.0f, 0.75f);
        canvas.restore();

        // the gift itself
        emojiPaint.setTextSize(dp(56));
        final Paint.FontMetrics fm = emojiPaint.getFontMetrics();
        canvas.drawText(emoji, cx, rect.top + dp(60) - (fm.ascent + fm.descent) / 2f, emojiPaint);

        title.ellipsize(w - dp(24));
        title.draw(canvas, cx - title.getCurrentWidth() / 2f, rect.top + dp(118), 0xFFFFFFFF, 1f);
        subtitle.ellipsize(w - dp(24));
        subtitle.draw(canvas, cx - subtitle.getCurrentWidth() / 2f, rect.top + dp(138), Theme.multAlpha(0xFFFFFFFF, .8f), 1f);

        drawWatermark(canvas, w, h);
        canvas.restore();

        ribbon.setBounds((int) rect.right - dp(48 - 1.33f), (int) rect.top - dp(1.33f), (int) rect.right + dp(1.33f), (int) rect.top + dp(48 - 1.33f));
        ribbon.draw(canvas);
    }

    /** Diagonal "ДЕМО" across the whole card. Fixed size and opacity, there is no setting for it. */
    private void drawWatermark(Canvas canvas, float w, float h) {
        watermarkFillPaint.setTypeface(AndroidUtilities.bold());
        watermarkFillPaint.setTextAlign(Paint.Align.CENTER);
        watermarkFillPaint.setColor(Theme.multAlpha(0xFFFFFFFF, .30f));
        watermarkStrokePaint.setTypeface(AndroidUtilities.bold());
        watermarkStrokePaint.setTextAlign(Paint.Align.CENTER);
        watermarkStrokePaint.setStyle(Paint.Style.STROKE);
        watermarkStrokePaint.setStrokeWidth(dp(1.5f));
        watermarkStrokePaint.setColor(Theme.multAlpha(0xFF000000, .15f));

        final float diagonal = (float) Math.hypot(w, h);
        watermarkFillPaint.setTextSize(100);
        final float textSize = 100 * (diagonal * 0.5f) / watermarkFillPaint.measureText(DEMO_LABEL);
        watermarkFillPaint.setTextSize(textSize);
        watermarkStrokePaint.setTextSize(textSize);

        final float angle = (float) Math.toDegrees(Math.atan2(h, w));
        final Paint.FontMetrics fm = watermarkFillPaint.getFontMetrics();
        final float cx = rect.centerX();
        final float cy = rect.centerY();
        final float baseline = cy - (fm.ascent + fm.descent) / 2f;

        canvas.save();
        canvas.rotate(-angle, cx, cy);
        canvas.drawText(DEMO_LABEL, cx, baseline, watermarkStrokePaint);
        canvas.drawText(DEMO_LABEL, cx, baseline, watermarkFillPaint);
        canvas.restore();
    }

    /** A star glyph used as the pattern element: StarGiftPatterns only needs a Drawable it can place and fade. */
    private static class GlyphDrawable extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        GlyphDrawable() {
            paint.setColor(0xFFFFFFFF);
            paint.setTextAlign(Paint.Align.CENTER);
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            final Rect bounds = getBounds();
            paint.setTextSize(bounds.height());
            final Paint.FontMetrics fm = paint.getFontMetrics();
            canvas.drawText("★", bounds.centerX(), bounds.centerY() - (fm.ascent + fm.descent) / 2f, paint);
        }

        @Override
        public void setAlpha(int alpha) {
            paint.setAlpha(alpha);
        }

        @Override
        public void setColorFilter(ColorFilter colorFilter) {
            paint.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return PixelFormat.TRANSLUCENT;
        }
    }
}
