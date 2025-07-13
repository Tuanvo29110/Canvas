package io.canvasmc.canvas.command;

import com.mojang.brigadier.CommandDispatcher;
import io.papermc.paper.threadedregions.RegionizedWorldData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TpsBarCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            literal("tpsbar")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(2, "canvas.admin.command.tpsbar"))
                .executes(context -> {
                    final CommandSourceStack source = context.getSource();
                    if (!source.isPlayer()) {
                        source.sendFailure(Component.literal("Must be run by a player if executing on self"));
                        return 0;
                    }
                    ServerPlayer player = source.getPlayer();
                    if (player == null) {
                        throw new IllegalStateException("player cannot be null");
                    }
                    RegionizedWorldData worldData = player.level().getCurrentWorldData();
                    if (worldData.tpsBarTask.players.contains(player)) {
                        player.tpsbar = false;
                        worldData.tpsBarTask.players.remove(player);
                    } else {
                        player.tpsbar = true;
                        worldData.tpsBarTask.players.add(player);
                    }
                    return 1;
                }).then(argument("player", EntityArgument.player()).executes(context -> {
                    ServerPlayer player = context.getArgument("player", ServerPlayer.class);
                    // we cannot guarantee that the player is within the same region as the source
                    // so we schedule this to the targets scheduler for the correct thread context
                    player.getBukkitEntity().taskScheduler.schedule((_) -> {
                        RegionizedWorldData worldData = player.level().getCurrentWorldData();
                        if (player.tpsbar) {
                            player.tpsbar = false;
                            worldData.tpsBarTask.players.remove(player);
                        } else {
                            player.tpsbar = true;
                            worldData.tpsBarTask.players.add(player);
                        }
                    }, null, 1L);
                    return 1;
                }))
        );
    }
}
