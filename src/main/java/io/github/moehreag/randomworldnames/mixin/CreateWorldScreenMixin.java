package io.github.moehreag.randomworldnames.mixin;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;

import io.github.moehreag.randomworldnames.RandomWorldNames;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(CreateWorldScreen.GameTab.class)
public class CreateWorldScreenMixin {
	@Unique
	private static final Identifier REGENERATE_LOCATION = Identifier.of("random-world-names", "regenerate");

	@Shadow
	@Final
	private TextFieldWidget worldNameField;

	@ModifyArg(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/LayoutWidgets;createLabeledWidget(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/client/gui/widget/Widget;Lnet/minecraft/text/Text;)Lnet/minecraft/client/gui/widget/LayoutWidget;"), index = 1)
	private Widget addGenerateNewNameButton(Widget widget) {
		DirectionalLayoutWidget horizontal = DirectionalLayoutWidget.horizontal().spacing(4);
		horizontal.add(widget);
		ButtonWidget regenerate = horizontal.add(TextIconButtonWidget.builder(
				Text.translatable("randomworldnames.regenerate"), b ->
						CompletableFuture.supplyAsync(() ->
								RandomWorldNames.getInstance().getRandomWorldName(() ->
										Text.translatable("selectWorld.newWorld").getString()))
								.thenAccept(worldNameField::setText), true)
				.dimension(widget.getHeight(), widget.getHeight())
				.texture(REGENERATE_LOCATION, widget.getHeight(), widget.getHeight())
				.build());
		regenerate.setTooltip(Tooltip.of(Text.translatable("randomworldnames.regenerate")));
		regenerate.setTooltipDelay(Duration.of(500, ChronoUnit.MILLIS));
		return horizontal;
	}
}
