package io.canvasmc.canvas.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public class AsyncSaveAllCommand {
    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("save-async")
                .requires(commandSourceStack -> commandSourceStack.hasPermission(4, "canvas.admin.command.asyncsaveall"))
                .executes(commandContext -> saveAll(commandContext.getSource()))
        );
    }

    private static int saveAll(@NotNull CommandSourceStack source) {
        source.sendSuccess(() -> Component.translatable("commands.save.saving"), false);
        MinecraftServer server = source.getServer();
        for (final ServerLevel world : server.getAllLevels()) {
            world.regioniser.computeForAllRegions((region) -> {
                region.getData().shouldSaveNextTick = true;
            });
        }
        source.sendSuccess(() -> Component.literal("Marked all running regions for save asynchronously"), true);
        return 1;
    }

}
