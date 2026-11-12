package dev.targethud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class TargetHudSettingsScreen extends Screen {
	private enum Mode {
		BAR("Полоска"),
		BACKGROUND("Фон"),
		TEXT("Текст");

		final String label;

		Mode(String label) {
			this.label = label;
		}

		Mode next() {
			return values()[(ordinal() + 1) % values().length];
		}
	}

	private Mode mode = Mode.BAR;
	private boolean dragging;
	private double dragOffX;
	private double dragOffY;

	public TargetHudSettingsScreen() {
		super(Text.literal("TargetHUD"));
	}

	@Override
	protected void init() {
		TargetHudConfig c = TargetHudConfig.INSTANCE;

		int colW = Math.min(140, (this.width - 30) / 2);
		int x1 = 10;
		int x2 = x1 + colW + 10;
		int y = 24;
		int step = 22;

		// ----- левая колонка -----
		addDrawableChild(new ValueSlider(x1, y, colW, 20, "Размер", 0.5, 3.0, c.scale,
				v -> String.format(Locale.ROOT, "x%.2f", v),
				v -> c.scale = Math.round(v * 20) / 20f));
		y += step;
		addDrawableChild(toggle(x1, y, colW, "Худ", () -> c.enabled, v -> c.enabled = v));
		y += step;
		addDrawableChild(toggle(x1, y, colW, "Голова", () -> c.showHead, v -> c.showHead = v));
		y += step;
		addDrawableChild(toggle(x1, y, colW, "Текст HP", () -> c.showHpText, v -> c.showHpText = v));
		y += step;
		addDrawableChild(toggle(x1, y, colW, "Только игроки", () -> c.playersOnly, v -> c.playersOnly = v));
		y += step;
		addDrawableChild(ButtonWidget.builder(Text.literal("Сбросить всё"), b -> {
			TargetHudConfig.INSTANCE = new TargetHudConfig();
			clearAndInit();
		}).dimensions(x1, y, colW, 20).build());

		// ----- правая колонка: цвет -----
		y = 24;
		addDrawableChild(ButtonWidget.builder(Text.literal("Цвет: " + mode.label), b -> {
			mode = mode.next();
			clearAndInit();
		}).dimensions(x2, y, colW, 20).build());
		y += step;
		channelSlider(x2, y, colW, "R", 16);
		y += step;
		channelSlider(x2, y, colW, "G", 8);
		y += step;
		channelSlider(x2, y, colW, "B", 0);
		y += step;
		channelSlider(x2, y, colW, "Прозрачность", 24);
		y += step;

		int[] presets = {0x8A4DFF, 0xFF3B3B, 0x3BD16F, 0x3B8CFF, 0xFF9F1A, 0xFF4DA6, 0xFFFFFF};
		int pw = colW / presets.length;
		for (int i = 0; i < presets.length; i++) {
			final int rgb = presets[i];
			addDrawableChild(ButtonWidget.builder(Text.literal("■").styled(s -> s.withColor(rgb)), b -> {
				setColor((getColor() & 0xFF000000) | rgb);
				clearAndInit();
			}).dimensions(x2 + i * pw, y, pw - 2, 20).build());
		}
		y += step;

		addDrawableChild(ButtonWidget.builder(Text.literal("Готово"), b -> close())
				.dimensions(x2, y, colW, 20).build());
	}

	private ButtonWidget toggle(int x, int y, int w, String label, Supplier<Boolean> get, Consumer<Boolean> set) {
		return ButtonWidget.builder(toggleText(label, get.get()), b -> {
			set.accept(!get.get());
			b.setMessage(toggleText(label, get.get()));
		}).dimensions(x, y, w, 20).build();
	}

	private static Text toggleText(String label, boolean on) {
		return Text.literal(label + ": " + (on ? "ВКЛ" : "ВЫКЛ"));
	}

	private void channelSlider(int x, int y, int w, String label, int shift) {
		addDrawableChild(new ValueSlider(x, y, w, 20, label, 0, 255, (getColor() >> shift) & 0xFF,
				v -> String.valueOf((int) Math.round(v)),
				v -> setColor((getColor() & ~(0xFF << shift)) | (((int) Math.round(v)) << shift))));
	}

	private int getColor() {
		TargetHudConfig c = TargetHudConfig.INSTANCE;
		return switch (mode) {
			case BAR -> c.barColor;
			case BACKGROUND -> c.backgroundColor;
			case TEXT -> c.textColor;
		};
	}

	private void setColor(int argb) {
		TargetHudConfig c = TargetHudConfig.INSTANCE;
		switch (mode) {
			case BAR -> c.barColor = argb;
			case BACKGROUND -> c.backgroundColor = argb;
			case TEXT -> c.textColor = argb;
		}
	}

	// лёгкое затемнение без размытия (двойной blur в 1.21 вызывает краш)
	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		context.fill(0, 0, this.width, this.height, 0x66000000);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		ctx.drawCenteredTextWithShadow(this.textRenderer, "TargetHUD — настройки", this.width / 2, 8, 0xFFFFFFFF);
		ctx.drawCenteredTextWithShadow(this.textRenderer, "Перетаскивай худ мышкой или пальцем", this.width / 2, this.height - 14, 0xFFAAAAAA);

		if (this.client != null && this.client.player != null) {
			TargetHudRenderer.draw(ctx, this.client, this.client.player, this.client.player.getHealth(), 1f);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (super.mouseClicked(mouseX, mouseY, button)) return true;
		if (button == 0 && this.client != null && this.client.player != null) {
			TargetHudRenderer.Layout l = TargetHudRenderer.layoutFor(this.client, this.client.player, this.client.player.getHealth());
			if (l.contains(mouseX, mouseY)) {
				dragging = true;
				dragOffX = mouseX - l.x();
				dragOffY = mouseY - l.y();
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (dragging && button == 0) {
			TargetHudConfig c = TargetHudConfig.INSTANCE;
			c.xRatio = (float) Math.max(0.0, Math.min(1.0, (mouseX - dragOffX) / this.width));
			c.yRatio = (float) Math.max(0.0, Math.min(1.0, (mouseY - dragOffY) / this.height));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		dragging = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void removed() {
		TargetHudConfig.save();
	}

	/** Слайдер с произвольным диапазоном и подписью. */
	private static class ValueSlider extends SliderWidget {
		private final String label;
		private final double min;
		private final double max;
		private final Function<Double, String> format;
		private final Consumer<Double> onChange;

		ValueSlider(int x, int y, int w, int h, String label, double min, double max, double current,
					Function<Double, String> format, Consumer<Double> onChange) {
			super(x, y, w, h, Text.empty(), (current - min) / (max - min));
			this.label = label;
			this.min = min;
			this.max = max;
			this.format = format;
			this.onChange = onChange;
			updateMessage();
		}

		private double current() {
			return min + this.value * (max - min);
		}

		@Override
		protected void updateMessage() {
			if (label == null || format == null) return;
			setMessage(Text.literal(label + ": " + format.apply(current())));
		}

		@Override
		protected void applyValue() {
			onChange.accept(current());
		}
	}
}
