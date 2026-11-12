package dev.targethud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;

import java.util.Locale;

public final class TargetHudRenderer {
	private static final int PAD = 5;
	private static final int HEAD = 26;
	private static final int BAR_H = 4;
	private static final int BASE_H = 42;

	private static long lastNanos = 0L;

	private TargetHudRenderer() {
	}

	/** Размеры и позиция худа на экране (в пикселях GUI, уже с учётом масштаба). */
	public record Layout(float x, float y, int w, int h, float scale) {
		public boolean contains(double mx, double my) {
			return mx >= x && my >= y && mx <= x + w * scale && my <= y + h * scale;
		}
	}

	public static void onHudRender(DrawContext ctx, RenderTickCounter tickCounter) {
		TargetHudConfig cfg = TargetHudConfig.INSTANCE;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (!cfg.enabled || mc.player == null || mc.options.hudHidden) return;
		// в окне настроек худ рисует сам экран
		if (mc.currentScreen instanceof TargetHudSettingsScreen) return;

		long now = System.nanoTime();
		float dt = lastNanos == 0L ? 0.016f : (now - lastNanos) / 1_000_000_000f;
		lastNanos = now;
		dt = Math.min(dt, 0.1f);

		TargetTracker.frame(dt);

		LivingEntity e = TargetTracker.getRenderEntity();
		float alpha = TargetTracker.getAlpha();
		if (e == null || alpha <= 0.02f) return;

		draw(ctx, mc, e, TargetTracker.getShownHealth(), alpha);
	}

	public static String hpText(float health, float max) {
		return "HP " + fmt(health) + " / " + fmt(max);
	}

	private static String fmt(float v) {
		return String.format(Locale.ROOT, "%.1f", v).replace('.', ',');
	}

	public static Layout layoutFor(MinecraftClient mc, LivingEntity e, float health) {
		float max = Math.max(1f, e.getMaxHealth());
		return layout(mc, e.getName().getString(), hpText(Math.max(0f, Math.min(health, max)), max));
	}

	public static Layout layout(MinecraftClient mc, String name, String hp) {
		TargetHudConfig c = TargetHudConfig.INSTANCE;
		TextRenderer tr = mc.textRenderer;

		int textX = PAD + (c.showHead ? HEAD + 6 : 0);
		int contentW = tr.getWidth(name);
		if (c.showHpText) contentW = Math.max(contentW, tr.getWidth(hp));
		int w = Math.max(100, textX + contentW + PAD);
		int h = BASE_H;

		int sw = mc.getWindow().getScaledWidth();
		int sh = mc.getWindow().getScaledHeight();
		float px = c.xRatio * sw;
		float py = c.yRatio * sh;
		px = Math.max(0f, Math.min(px, Math.max(0f, sw - w * c.scale)));
		py = Math.max(0f, Math.min(py, Math.max(0f, sh - h * c.scale)));
		return new Layout(px, py, w, h, c.scale);
	}

	public static void draw(DrawContext ctx, MinecraftClient mc, LivingEntity e, float health, float alpha) {
		TargetHudConfig c = TargetHudConfig.INSTANCE;
		TextRenderer tr = mc.textRenderer;

		String name = e.getName().getString();
		float max = Math.max(1f, e.getMaxHealth());
		health = Math.max(0f, Math.min(health, max));
		String hp = hpText(health, max);
		Layout l = layout(mc, name, hp);

		MatrixStack m = ctx.getMatrices();
		m.push();
		m.translate(l.x(), l.y(), 0f);
		m.scale(l.scale(), l.scale(), 1f);

		// фон
		roundedRect(ctx, 0, 0, l.w(), l.h(), 4, withAlpha(c.backgroundColor, alpha, 0));

		int textX = PAD;
		if (c.showHead) {
			drawHead(ctx, tr, e, PAD, PAD);
			textX = PAD + HEAD + 6;
		}

		// ник и HP
		ctx.drawText(tr, name, textX, c.showHpText ? 6 : 11, withAlpha(c.textColor, alpha, 4), true);
		if (c.showHpText) {
			ctx.drawText(tr, hp, textX, 18, withAlpha(c.textColor, alpha * 0.7f, 4), true);
		}

		// полоска здоровья
		int bx1 = PAD;
		int bx2 = l.w() - PAD;
		int by1 = l.h() - 9;
		int by2 = by1 + BAR_H;
		roundedRect(ctx, bx1, by1, bx2, by2, 2, withAlpha(c.barColor, alpha * 0.25f, 0));
		int fillW = Math.round((bx2 - bx1) * (health / max));
		if (fillW > 0) {
			roundedRect(ctx, bx1, by1, bx1 + fillW, by2, 2, withAlpha(c.barColor, alpha, 0));
		}

		m.pop();
	}

	private static void drawHead(DrawContext ctx, TextRenderer tr, LivingEntity e, int x, int y) {
		if (e instanceof AbstractClientPlayerEntity p) {
			PlayerSkinDrawer.draw(ctx, p.getSkinTextures(), x, y, HEAD);
		} else {
			roundedRect(ctx, x, y, x + HEAD, y + HEAD, 3, 0xFF3A3A44);
			ctx.drawCenteredTextWithShadow(tr, "?", x + HEAD / 2, y + HEAD / 2 - 4, 0xFFFFFFFF);
		}
	}

	/** Прямоугольник со скруглёнными углами (радиус в пикселях). */
	private static void roundedRect(DrawContext ctx, int x1, int y1, int x2, int y2, int r, int color) {
		r = Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2));
		if (r <= 0) {
			ctx.fill(x1, y1, x2, y2, color);
			return;
		}
		ctx.fill(x1, y1 + r, x2, y2 - r, color);
		for (int i = 0; i < r; i++) {
			double dy = r - i - 0.5;
			int dx = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
			ctx.fill(x1 + dx, y1 + i, x2 - dx, y1 + i + 1, color);
			ctx.fill(x1 + dx, y2 - i - 1, x2 - dx, y2 - i, color);
		}
	}

	/** Умножает альфу цвета на a. minAlpha нужен для текста: у MC альфа <= 3 рисуется как непрозрачная. */
	private static int withAlpha(int argb, float a, int minAlpha) {
		a = Math.max(0f, Math.min(1f, a));
		int aa = Math.round(((argb >>> 24) & 0xFF) * a);
		aa = Math.max(minAlpha, aa);
		return (aa << 24) | (argb & 0xFFFFFF);
	}
}
