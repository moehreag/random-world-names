package io.github.moehreag.randomworldnames.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.moehreag.randomworldnames.RandomWorldNames;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldCreationUiState.class)
public class WorldCreatorMixin {

	@WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screens/worldselection/WorldCreationUiState;DEFAULT_WORLD_NAME:Lnet/minecraft/network/chat/Component;"))
	private Component getRandomWorldName(Operation<Component> original) {
		return RandomWorldNames.getInstance().getRandomWorldName(original::call, Component::literal);
	}
}
