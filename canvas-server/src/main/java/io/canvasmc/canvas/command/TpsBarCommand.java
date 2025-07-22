package io.canvasmc.canvas.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.canvasmc.canvas.Config;
import io.canvasmc.canvas.RegionizedTpsBar;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class TpsBarCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!Config.INSTANCE.enableTpsBar) return;
        dispatcher.register(
            literal("tpsbar")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(3, "canvas.command.tpsbar"))
                .executes((context) -> {
                    CommandSourceStack source = context.getSource();
                    final ServerPlayer player = source.getPlayer();
                    if (player == null || !source.isPlayer()) {
                        source.sendFailure(Component.literal("This must be run by a valid player entity"));
                        return 0;
                    }
                    player.setTpsBarEnabled(!player.localEntry.enabled());
                    return 1;
                }).then(argument("player", EntityArgument.player())
                    .executes((context) -> {
                        final ServerPlayer player = EntityArgument.getPlayer(context, "player");
                        player.setTpsBarEnabled(!player.localEntry.enabled());
                        return 1;
                    }).then(argument("placement", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("action_bar");
                            builder.suggest("boss_bar");
                            return builder.buildFuture();
                        })
                        .executes((context) -> {
                            CommandSourceStack source = context.getSource();
                            final ServerPlayer player = EntityArgument.getPlayer(context, "player");
                            switch (StringArgumentType.getString(context, "placement").toLowerCase()) {
                                case "action_bar" -> player.setTpsBarPlacement(RegionizedTpsBar.Placement.ACTION_BAR);
                                case "boss_bar" -> player.setTpsBarPlacement(RegionizedTpsBar.Placement.BOSS_BAR);
                                default -> {
                                    source.sendFailure(Component.literal("Not valid placement"));
                                }
                            }
                            return 1;
                        })
                    )
                )
        );
    }
}
