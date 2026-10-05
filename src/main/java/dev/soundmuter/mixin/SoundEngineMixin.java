package dev.soundmuter.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.soundmuter.SoundMuter;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
//? if >=1.21.10 {
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//?} else
/*import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;*/

@Mixin(SoundEngine.class)
public class SoundEngineMixin {
	//? if >=1.21.10 {
	@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void soundmuter$cancelMuted(SoundInstance instance, CallbackInfoReturnable<SoundEngine.PlayResult> cir) {
		if (SoundMuter.onPlay(instance)) {
			cir.setReturnValue(SoundEngine.PlayResult.NOT_STARTED);
		}
	}
	//?} else {
	/*@Inject(method = "play", at = @At("HEAD"), cancellable = true)
	private void soundmuter$cancelMuted(SoundInstance instance, CallbackInfo ci) {
		if (SoundMuter.onPlay(instance)) {
			ci.cancel();
		}
	}
	*///?}

	// play() calls the (float, SoundSource) overload directly, so the starting volume
	// never goes through calculateVolume(SoundInstance)
	@ModifyExpressionValue(
		method = "play",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F")
	)
	private float soundmuter$scaleStartVolume(float volume, @Local(argsOnly = true) SoundInstance instance) {
		return SoundMuter.adjustVolume(instance, volume);
	}

	// called every tick for each ticking sound (minecarts, bees, elytra...), keep it cheap
	@ModifyReturnValue(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"))
	private float soundmuter$scaleTickVolume(float volume, @Local(argsOnly = true) SoundInstance instance) {
		return SoundMuter.adjustVolume(instance, volume);
	}
}
