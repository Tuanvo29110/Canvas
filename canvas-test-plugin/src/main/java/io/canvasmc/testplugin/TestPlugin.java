package io.canvasmc.testplugin;

import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import java.util.List;
import net.kyori.adventure.util.TriState;
import net.minecraft.util.RandomSource;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.WorldInfo;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

public class TestPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("Enabling test plugin for Canvas");
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getCommandMap().register("rtp", new BukkitCommand("rtp") {
            @Override
            public boolean execute(@NotNull final CommandSender sender, @NotNull final String commandLabel, final @NotNull String @NotNull [] args) {
                if (sender instanceof Player player) {
                    RandomSource randomSource = RandomSource.create();
                    int blockX = build(randomSource);
                    int blockZ = build(randomSource);
                    player.teleportAsync(
                        new Location(
                            player.getWorld(), blockX, 90, blockZ, player.getYaw(), player.getPitch()
                        )
                    );
                    return true;
                }
                return false;
            }
        });
        getServer().createWorld(
            // safe blank world to test hoppers
            new WorldCreator("hoppers")
                .environment(World.Environment.NORMAL)
                .bonusChest(false)
                .generateStructures(false)
                .biomeProvider(new BiomeProvider() {
                    @Override
                    public @NotNull Biome getBiome(@NotNull final WorldInfo worldInfo, final int x, final int y, final int z) {
                        return Biome.THE_VOID;
                    }

                    @Override
                    public @NotNull List<Biome> getBiomes(@NotNull final WorldInfo worldInfo) {
                        return List.of(Biome.THE_VOID);
                    }
                })
                .hardcore(false)
                .keepSpawnLoaded(TriState.FALSE)
                .type(WorldType.FLAT)
        );
    }

    public int build(@NotNull RandomSource randomSource) {
        int number = randomSource.nextInt(100000);
        return randomSource.nextBoolean() ? -number : number;
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling test plugin for Canvas");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent playerRespawnEvent) {
        getLogger().info("PlayerRespawnEvent called!");
        // uncomment when testing respawn location modification
        // playerRespawnEvent.setRespawnLocation(new Location(
        //     Bukkit.getWorld("world"), 0, 9000, 0
        // ));
        // uncomment when testing player kicking during respawn
        // playerRespawnEvent.getPlayer().kick(Component.text("Bye bye"));
    }

    @EventHandler
    public void onTeleportPlayer(PlayerTeleportEvent playerTeleportEvent) {
        getLogger().info("PlayerTeleportEvent called!");
    }

    @EventHandler
    public void onTeleportEntity(EntityTeleportEvent playerTeleportEvent) {
        getLogger().info("EntityTeleportEvent called!");
    }

    @EventHandler
    public void onTeleportEndGateway(PlayerTeleportEndGatewayEvent playerTeleportEndGatewayEvent) {
        getLogger().info("PlayerTeleportEndGatewayEvent called!");
    }
}
