package net.dugged.selfhostedyggdrasil.mixins;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.HttpAuthenticationService;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.exceptions.AuthenticationUnavailableException;
import com.mojang.authlib.exceptions.InvalidCredentialsException;
import com.mojang.authlib.exceptions.UserMigratedException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService;
import com.mojang.authlib.yggdrasil.response.Response;
import net.dugged.selfhostedyggdrasil.Configuration;
import net.dugged.selfhostedyggdrasil.SelfhostedYggdrasilRequest;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.net.URL;

@Mixin(value = YggdrasilMinecraftSessionService.class, remap = false)
public abstract class MixinYggdrasilMinecraftSessionService {
    @Shadow
    public abstract YggdrasilAuthenticationService getAuthenticationService();

    @Shadow
    @Final
    private Gson gson;

    @Inject(method = "joinServer", at = @At("HEAD"), cancellable = true)
    private void selfhostedyggdrasil$joinServer(final GameProfile profile, final String authenticationToken,
                                                final String serverId, final CallbackInfo ci)
            throws AuthenticationException {
        if (!Configuration.enabled) {
            return;
        }

        final SelfhostedYggdrasilRequest request = new SelfhostedYggdrasilRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;
        request.authString = Configuration.authString;
        makeRequest(HttpAuthenticationService.constantURL(Configuration.joinURL), request);
        ci.cancel();
    }

    // Reimplementation of the protected authlib request helper.
    @Unique
    private void makeRequest(final URL url, final Object input) throws AuthenticationException {
        try {
            final String jsonResult = input == null
                    ? getAuthenticationService().performGetRequest(url)
                    : getAuthenticationService().performPostRequest(url, gson.toJson(input), "application/json");
            final Response result = gson.fromJson(jsonResult, Response.class);
            if (result == null) {
                return;
            }
            if (StringUtils.isNotBlank(result.getError())) {
                if ("UserMigratedException".equals(result.getCause())) {
                    throw new UserMigratedException(result.getErrorMessage());
                } else if ("ForbiddenOperationException".equals(result.getError())) {
                    throw new InvalidCredentialsException(result.getErrorMessage());
                } else {
                    throw new AuthenticationException(result.getErrorMessage());
                }
            }
        } catch (final IOException | IllegalStateException | JsonParseException e) {
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        }
    }
}
