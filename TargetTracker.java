package dev.targethud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Хранит текущую цель худа: на кого смотришь или кого последним ударил.
 */
public final class TargetTracker {
	private static final long HOLD_MS = 3000L;

	private static LivingEntity target;
	private static LivingEntity renderEntity;
	private static long lastSeenMs;
	private static float shownHealth;
	private static float alpha;

	private TargetTracker() {
	}

	public static boolean accepts(MinecraftClient mc, Entity e) {
		if (!(e instanceof LivingEntity)) return false;
		if (e == mc.player || e instanceof ArmorStandEntity) return false;
		if (TargetHudConfig.INSTANCE.playersOnly && !(e instanceof PlayerEntity)) return false;
		return true;
	}

	public static void setTarget(LivingEntity le) {
		if (target != le) {
			target = le;
			shownHealth = le.getHealth();
		}
		lastSeenMs = System.currentTimeMillis();
	}

	public static void onAttack(Entity e) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (accepts(mc, e)) {
			setTarget((LivingEntity) e);
		}
	}

	public static void tick(MinecraftClient mc) {
		if (mc.player == null || mc.world == null) {
			target = null;
			renderEntity = null;
			alpha = 0f;
			return;
		}

		Entity looked = mc.targetedEntity;
		if (looked != null && accepts(mc, looked)) {
			setTarget((LivingEntity) looked);
		}

		if (target != null) {
			boolean expired = System.currentTimeMillis() - lastSeenMs > HOLD_MS;
			if (expired || target.isRemoved() || !target.isAlive()) {
				target = null;
			}
		}
	}

	/** Вызывается каждый кадр: плавное появление и плавная полоска HP. */
	public static void frame(float dt) {
		boolean active = target != null;
		alpha += (active ? 1f : -1f) * dt * 6f;
		alpha = Math.max(0f, Math.min(1f, alpha));

		if (active) {
			renderEntity = target;
			float hp = target.getHealth();
			shownHealth += (hp - shownHealth) * Math.min(1f, dt * 10f);
		} else if (alpha <= 0f) {
			renderEntity = null;
		}
	}

	public static LivingEntity getRenderEntity() {
		return renderEntity;
	}

	public static float getShownHealth() {
		return shownHealth;
	}

	public static float getAlpha() {
		return alpha;
	}
}
