package io.canvasmc.canvas.command.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.canvasmc.canvas.command.CommandInstance;
import io.canvasmc.canvas.entity.tracking.ThreadedTracker;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.commands.Commands.literal;

public class TrackingControlCommand implements CommandInstance {

    @Override
    public LiteralCommandNode<CommandSourceStack> register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        return dispatcher.register(
            literal("entitytracking").requires(commandSourceStack -> commandSourceStack.hasPermission(3, "canvas.debug.command.entitytracking"))
                .executes(context -> {
                    ThreadedTracker.canceled.set(!ThreadedTracker.canceled.get());
                    if (!context.getSource().isPlayer() || context.getSource().getPlayer() == null)
                        return 0;
                    ServerPlayer serverPlayer = context.getSource().getPlayer();
                    // search all tick data instances for the player in tracking
                    for (final ServerLevel level : context.getSource().getServer().getAllLevels()) {
                        if (level.levelTickData.trackerEntities.contains(serverPlayer)) {
                            serverPlayer.sendSystemMessage(Component.literal("has player in world " + level));
                        }
                        level.regioniser.computeForAllRegionsUnsynchronised((region) -> {
                            if (region.getData().tickData.trackerEntities.contains(serverPlayer)) {
                                serverPlayer.sendSystemMessage(Component.literal("has player in region " + region.getData().tickHandle));
                            }
                        });
                    }
                    return 1;
                })
        );
    }

    @Override
    public boolean isDebug() {
        return true;
    }
}
