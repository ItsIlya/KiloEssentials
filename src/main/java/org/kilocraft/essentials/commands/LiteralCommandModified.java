package org.kilocraft.essentials.commands;

import com.mojang.brigadier.tree.CommandNode;
import org.kilocraft.essentials.config.ConfigCache;
import org.kilocraft.essentials.config.KiloConfig;
import org.kilocraft.essentials.mixin.CommandManagerMixin;

import java.util.ArrayList;
import java.util.List;

public class LiteralCommandModified {
    private static String NMSCommandNamePrefix = "minecraft:";
    private static String keCommandPrefix = "ke_";

    /**
     * @see CommandManagerMixin
     *
     * This only works for Command literals and their sub commands
     */
    private static List<String> keCommandsToKeep = new ArrayList<String>(){{
        add("ke_gamemode");
        add("ke_reload");
        add("ke_locate");
        add("ke_op");
        add("ke_kill");
        add("ke_tp");
        add("ke_msg");
        add("ke_whisper");
        add("ke_tell");
        add("ke_time");
        add("ke_ban");
        add("ke_ban");
        add("ke_kick");
        add("ke_help");
    }};

    private static List<String> vanillaCommandsToRename = new ArrayList<String>(){{
        add("gamemode");
        add("kill");
        add("help");
        add("locate");
        add("op");
        add("deop");
        add("me");
        add("w");
        add("msg");
        add("whisper");
        add("tell");
        add("teammsg");
        add("tm");
        add("time");
        add("ban");
        add("kick");
        add("ban-ip");
        add("pardon");
        add("pardon-ip");
        add("save-all");
        add("stop");
    }};

    public static boolean isVanillaCommand(String nodeName) {
        return vanillaCommandsToRename.contains(nodeName);
    }

    public static boolean isCustomCommand(String nodeName) {
        return keCommandsToKeep.contains(nodeName);
    }

    public static String getNMSCommandName(String vanillaName) {
        return NMSCommandNamePrefix + vanillaName;
    }

    public static String getKECommandName(String defaultName) {
        return defaultName.replace(keCommandPrefix, "");
    }

    public static String getNMSCommandPrefix() {
        return NMSCommandNamePrefix;
    }

    public static boolean shouldUse(String name) {
        return !vanillaCommandsToRename.contains(name.replace(NMSCommandNamePrefix, ""))
                || isCustomCommand(keCommandPrefix + name);
    }

    public static <S> boolean canSourceUse(CommandNode<S> commandNode, S source) {
        boolean sugReqPerm = KiloConfig.getProvider().getMain().getBooleanSafely(
                ConfigCache.COMMANDS_SUGGESTIONS_REQUIRE_PERMISSION, true);

        if (!sugReqPerm)
            return shouldUse(commandNode.getName());

        return shouldUse(commandNode.getName()) && commandNode.canUse(source);
    }

}