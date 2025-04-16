package io.github.moehreag.randomworldnames.mixin;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import io.github.moehreag.randomworldnames.RandomWorldNames;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CreateWorldScreen.GameTab.class)
public class CreateWorldScreenMixin {
	@Unique
	private static final ResourceLocation REGENERATE_LOCATION = RandomWorldNames.rl("regenerate");

	@Shadow
	@Final
	private EditBox nameEdit;

	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/CommonLayouts;labeledElement(Lnet/minecraft/client/gui/Font;Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/network/chat/Component;)Lnet/minecraft/client/gui/layouts/Layout;"), index = 1)
	private LayoutElement addGenerateNewNameButton(LayoutElement element) {
		LinearLayout horizontal = LinearLayout.horizontal().spacing(4);
		horizontal.addChild(element);
		Button regenerate = horizontal.addChild(SpriteIconButton.builder(
				Component.translatable("random_world_names.regenerate"), b ->
						CompletableFuture.supplyAsync(() ->
								RandomWorldNames.getInstance().getRandomWorldName(() ->
										Component.translatable("selectWorld.newWorld").getString()))
								.thenAccept(nameEdit::setValue), true)
				.size(element.getHeight(), element.getHeight())
				.sprite(REGENERATE_LOCATION, element.getHeight(), element.getHeight())
				.build());
		regenerate.setTooltip(Tooltip.create(Component.translatable("random_world_names.regenerate")));
		regenerate.setTooltipDelay(Duration.of(500, ChronoUnit.MILLIS));
		return horizontal;
	}
}
