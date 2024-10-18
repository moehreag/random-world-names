package io.github.moehreag.randomworldnames.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.moehreag.randomworldnames.RandomWorldNames;
import net.minecraft.client.gui.screen.world.WorldCreator;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldCreator.class)
public class WorldCreatorMixin {

	@WrapOperation(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/world/WorldCreator;NEW_WORLD_NAME:Lnet/minecraft/text/Text;"))
	private Text getRandomWorldName(Operation<Text> original) {
		return RandomWorldNames.getInstance().getRandomWorldName(original::call, Text::literal);
	}
}
