package dev.targethud;

import com.mojang.brigadier.Command;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.ActionResult;
import org.lwjgl.glfw.GLFW;

public class TargetHudMod implements ClientModInitializer {
	public static KeyBinding openSettingsKey;
	private static boolean openScreenRequest;

	@Override
	public void onInitializeClient() {
		TargetHudConfig.load();

		// Right Shift открывает настройки (клавишу можно сменить в управлении)
		openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.targethud.settings",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				"category.targethud"
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			TargetTracker.tick(client);

			while (openSettingsKey.wasPressed()) {
				openScreenRequest = true;
			}
			// экран открываем только когда чат/меню закрыты
			if (openScreenRequest && client.currentScreen == null && client.player != null) {
				openScreenRequest = false;
				client.setScreen(new TargetHudSettingsScreen());
			}
		});

		// цель = тот, кого ударил
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient && entity instanceof LivingEntity) {
				TargetTracker.onAttack(entity);
			}
			return ActionResult.PASS;
		});

		HudRenderCallback.EVENT.register(TargetHudRenderer::onHudRender);

		// команда /targethud — открыть настройки (удобно, если нет клавиатуры)
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
				dispatcher.register(ClientCommandManager.literal("targethud").executes(ctx -> {
					openScreenRequest = true;
					return Command.SINGLE_SUCCESS;
				})));
	}
}
