package net.dugged.selfhostedyggdrasil.mixins;

import net.dugged.selfhostedyggdrasil.Configuration;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {
    @Inject(method = "getSession", at = @At("HEAD"), cancellable = true)
    private void selfhostedyggdrasil$updateSession(final CallbackInfoReturnable<Session> cir) {
        if (!Configuration.auth.enabled || Configuration.session.username.isEmpty()
                || Configuration.session.uuid.isEmpty()) {
            return;
        }

        cir.setReturnValue(new Session(Configuration.session.username, Configuration.session.uuid,
                Configuration.session.uuid, "mojang"));
    }
}
