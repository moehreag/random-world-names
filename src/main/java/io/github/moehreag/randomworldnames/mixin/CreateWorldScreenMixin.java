package io.github.moehreag.randomworldnames.mixin;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.moehreag.randomworldnames.RandomWorldNames;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(CreateWorldScreen.GameTab.class)
public class CreateWorldScreenMixin {
	@Unique
	private static final ResourceLocation REGENERATE_LOCATION = new ResourceLocation("random-world-names", "textures/gui/sprites/regenerate.png");

	@Shadow
	@Final
	private EditBox nameEdit;

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/layouts/GridLayout$RowHelper;addChild(Lnet/minecraft/client/gui/layouts/LayoutElement;Lnet/minecraft/client/gui/layouts/LayoutSettings;)Lnet/minecraft/client/gui/layouts/LayoutElement;", ordinal = 1))
	private <T extends LayoutElement> T addGenerateNewNameButton(GridLayout.RowHelper instance, T widget, LayoutSettings layoutSettings, Operation<T> original) {
		GridLayout.RowHelper horizontal = new GridLayout().spacing(4).createRowHelper(2);
		horizontal.addChild(widget);
		Button regenerate = horizontal.addChild(new Button(0, 0, widget.getHeight(), widget.getHeight(),
				Component.translatable("randomworldnames.regenerate"), b ->
				CompletableFuture.supplyAsync(() ->
								RandomWorldNames.getInstance().getRandomWorldName(() ->
										Component.translatable("selectWorld.newWorld").getString()))
						.thenAccept(nameEdit::setValue), Supplier::get) {
			@Override
			protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
				super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
				guiGraphics.blit(REGENERATE_LOCATION, getX(), getY(), 0, 0, getWidth(), getHeight(), getWidth(), getHeight());
			}

			@Override
			public void renderString(GuiGraphics guiGraphics, Font font, int color) {
			}
		});
		regenerate.setTooltip(Tooltip.create(Component.translatable("randomworldnames.regenerate")));
		regenerate.setTooltipDelay(500);

		original.call(instance, horizontal.getGrid(), layoutSettings);
		return widget;
	}
}
