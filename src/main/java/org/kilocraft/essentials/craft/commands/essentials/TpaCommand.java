package org.kilocraft.essentials.craft.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.indicode.fabric.permissions.Thimble;
import net.minecraft.command.arguments.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.dedicated.ServerCommandOutput;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.Style;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import org.kilocraft.essentials.api.server.Server;

import javax.xml.transform.Source;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Indigo Amann
 */
public class TpaCommand {
    private static Map<ServerPlayerEntity, Pair<Pair<ServerPlayerEntity, Boolean>, Long>> tpMap = new HashMap<>();
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        {
            LiteralArgumentBuilder<ServerCommandSource> tpa = CommandManager.literal("tpa");
            tpa.requires(source -> source.hasPermissionLevel(2));
            tpa.requires(source -> {
                try {
                    return source.getPlayer() != null;
                } catch (CommandSyntaxException e) {
                    return false;
                }
            });
            LiteralArgumentBuilder<ServerCommandSource> tpahere = CommandManager.literal("tpahere");
            tpahere.requires(source -> source.hasPermissionLevel(2));
            tpahere.requires(source -> {
                try {
                    return source.getPlayer() != null;
                } catch (CommandSyntaxException e) {
                    return false;
                }
            });
            ArgumentBuilder playerA = CommandManager.argument("player", EntityArgumentType.player());
            ArgumentBuilder playerB = CommandManager.argument("player", EntityArgumentType.player());
            playerA.executes(context -> executeRequest(context, false));
            playerB.executes(context -> executeRequest(context, true));
            tpa.then(playerA);
            tpahere.then(playerB);
            dispatcher.register(tpa);
            dispatcher.register(tpahere);
        }
        {
            LiteralArgumentBuilder accept = CommandManager.literal("tpaccept");
            ArgumentBuilder player = CommandManager.argument("player", EntityArgumentType.player());
            player.executes(context -> executeResponse(context, true));
            accept.then(player);
            dispatcher.register(accept);
        }
        {
            LiteralArgumentBuilder deny = CommandManager.literal("tpdeny");
            ArgumentBuilder player = CommandManager.argument("player", EntityArgumentType.player());
            player.executes(context -> executeResponse(context, false));
            deny.then(player);
            dispatcher.register(deny);
        }
        {
            LiteralArgumentBuilder<ServerCommandSource> cancel = CommandManager.literal("tpcancel");
            cancel.requires(source -> source.hasPermissionLevel(1));
            cancel.executes(TpaCommand::cancelRequest);
            dispatcher.register(cancel);
        }
    }
    private static int executeRequest(CommandContext<ServerCommandSource> context, boolean here) throws CommandSyntaxException {
        ServerPlayerEntity victim = EntityArgumentType.getPlayer(context, "player");
        ServerPlayerEntity sender = context.getSource().getPlayer();
        tpMap.put(sender, new Pair<>(new Pair<>(victim, here), new Date().getTime()));
        victim.sendMessage(new LiteralText("").append(sender.getDisplayName()).append(new LiteralText(" has requested " + (here ? "that you teleport to them" : "to teleport to you") + ". ").formatted(Formatting.GOLD)).append(
                new LiteralText("[ACCEPT] ").setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpaccept " + sender.getGameProfile().getName()))
                .setColor(Formatting.GREEN))).append(
                new LiteralText("[DENY] ").setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpdeny " + sender.getGameProfile().getName()))
                        .setColor(Formatting.RED))));
        sender.sendMessage(new LiteralText("Your request was sent. ").formatted(Formatting.GOLD).append(new LiteralText("[CANCEL]").setStyle(new Style()
        .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tpcancel"))
        .setColor(Formatting.RED))));
        return 0;
    }
    private static int executeResponse(CommandContext<ServerCommandSource> context, boolean accepted) throws CommandSyntaxException {
        ServerPlayerEntity sender = EntityArgumentType.getPlayer(context, "player");
        ServerPlayerEntity victim = context.getSource().getPlayer();
        if (hasTPRequest(sender, victim)) {
            if (accepted) {
                sender.sendMessage(new LiteralText("").append(new LiteralText("Your teleportation request to ").formatted(Formatting.GOLD)).append(victim.getDisplayName()).append(new LiteralText(" was ").formatted(Formatting.GOLD).append(accepted ? new LiteralText("ACCEPTED").formatted(Formatting.GREEN) : new LiteralText("DENIED").formatted(Formatting.RED))));
                boolean toSender = useTPRequest(sender);
                ServerPlayerEntity tpTo = (toSender ? sender : victim);
                (toSender ? victim : sender).teleport(tpTo.getServerWorld(), tpTo.x, tpTo.y, tpTo.z, tpTo.yaw, tpTo.pitch);
            } else {
                sender.sendMessage(new LiteralText("Your teleportation requrest was denied.").formatted(Formatting.RED));
                victim.sendMessage(new LiteralText("The request was denied.").formatted(Formatting.GREEN));
                tpMap.remove(sender);
            }
        } else {
            victim.sendMessage(new LiteralText("").append(sender.getDisplayName()).append(new LiteralText(" is not requesting a telepoert.").formatted(Formatting.RED)));
        }
        return 0;
    }
    private static int cancelRequest(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity sender = context.getSource().getPlayer();
        if (hasAnyTPRequest(sender)) {
            tpMap.get(sender).getLeft().getLeft().sendMessage(new LiteralText("").append(sender.getDisplayName()).append(new LiteralText(" cancelled their teleportation request.").formatted(Formatting.GOLD)));
            tpMap.remove(sender);
            sender.sendMessage(new LiteralText("Your teleportation request was cancelled.").formatted(Formatting.GOLD));
        } else {
            sender.sendMessage(new LiteralText("You don't have an active teleportation request.").formatted(Formatting.RED));
        }
        return 0;
    }
    private static boolean hasAnyTPRequest(ServerPlayerEntity source) {
        if (tpMap.containsKey(source)) {
            if (new Date().getTime() - tpMap.get(source).getRight() > 60000) {
                tpMap.remove(source);
            }
            else return true;
        }
        return false;
    }
    private static boolean hasTPRequest(ServerPlayerEntity source, ServerPlayerEntity victim) {
        if (tpMap.containsKey(source)) {
            if (tpMap.get(source).getLeft().getLeft().equals(victim)) {
                if (new Date().getTime() - tpMap.get(source).getRight() > 60000) {
                    tpMap.remove(source);
                }
                else return true;
            }
        }
        return false;
    }
    private static boolean useTPRequest(ServerPlayerEntity source) {
        boolean toSender = tpMap.get(source).getLeft().getRight();
        tpMap.remove(source);
        return toSender;
    }
}
