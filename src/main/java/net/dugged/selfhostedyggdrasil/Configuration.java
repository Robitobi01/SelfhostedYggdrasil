package net.dugged.selfhostedyggdrasil;

import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID, category = "")
public final class Configuration {
    public static final Auth auth = new Auth();
    public static final Session session = new Session();

    private Configuration() {
    }

    public static final class Auth {
        @Config.Comment("Route authentication through the self-hosted session server.")
        public boolean enabled = false;
        @Config.Comment("Shared secret sent as the authString field of the join request.")
        public String sharedSecret = "";
        @Config.Comment("Join endpoint of the self-hosted session server.")
        public String joinUrl = "https://dugged.net/session/minecraft/join";
    }

    public static final class Session {
        @Config.RequiresMcRestart
        @Config.Comment("Player name used for the local session when self-hosted auth is enabled; leave empty to keep the normal session.")
        public String username = "";
        @Config.RequiresMcRestart
        @Config.Comment("Player UUID used as the local session UUID and access token; leave empty to keep the normal session.")
        public String uuid = "";
    }
}
