package net.dugged.selfhostedyggdrasil;

import net.minecraftforge.common.config.Config;

@Config(modid = Tags.MOD_ID)
public class Configuration {
    public static boolean enabled = false;
    // Additional credential passed to the configured join endpoint.
    public static String authString = "";
    public static String joinURL = "https://dugged.net/session/minecraft/join";
    @Config.RequiresMcRestart
    public static String username = "";
    @Config.RequiresMcRestart
    public static String uuid = "";
}
