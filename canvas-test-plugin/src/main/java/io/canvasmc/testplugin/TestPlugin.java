package io.canvasmc.testplugin;

import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import java.util.List;
import java.util.Objects;
import io.canvasmc.canvas.event.WorldPreLoadEvent;
import net.kyori.adventure.util.TriState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.craftbukkit.entity.CraftPlayer;
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
        getServer().getCommandMap().register("blockentitytest", new BukkitCommand("blockentitytest") {
            @Override
            public boolean execute(@NotNull final CommandSender sender, @NotNull final String commandLabel, final @NotNull String @NotNull [] args) {
                if (sender instanceof Player bukkitPlayer) {
                    ServerPlayer serverPlayer = ((CraftPlayer) bukkitPlayer).getHandle();
                    ServerLevel world = serverPlayer.level();
                    final int chunkX = serverPlayer.chunkPosition().x;
                    final int chunkZ = serverPlayer.chunkPosition().z;
                    ChunkAccess nmsChunk = world.getChunk(chunkX, chunkZ);
                    int startX = (chunkX << 4) - 2;
                    int endX = (chunkX << 4) + 16 + 1;
                    int startZ = (chunkZ << 4) - 2;
                    int endZ = (chunkZ << 4) + 16 + 1;
                    int minY = world.getWorld().getMinHeight();
                    int maxY = world.getWorld().getMaxHeight();

                    for (int x = startX; x <= endX; x++) {
                        for (int z = startZ; z <= endZ; z++) {
                            for (int y = minY; y < maxY; y++) {
                                world.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
                            }
                        }
                    }

                    int baseX = chunkX << 4;
                    int baseZ = chunkZ << 4;

                    BlockData hopperData = Bukkit.createBlockData(Material.HOPPER);
                    BlockData chestData = Bukkit.createBlockData(Material.CHEST);

                    for (int x = baseX; x < baseX + 16; x++) {
                        for (int z = baseZ; z < baseZ + 16; z++) {
                            world.getWorld().setBlockData(x, minY, z, chestData);
                            BlockEntity be1 = nmsChunk.getFromBuckets(x, minY, z);
                            if (be1 == null) throw new IllegalStateException("Failed to fetch block entity from buckets");
                            BlockPos chestPos = new BlockPos(x, minY, z);
                            if (!be1.getBlockPos().equals(chestPos)) throw new IllegalStateException("Mismatched block entity position");

                            for (int y = minY + 1; y < maxY; y++) {
                                world.getWorld().setBlockData(x, y, z, hopperData);
                                BlockEntity be2 = nmsChunk.getFromBuckets(x, y, z);
                                if (be2 == null) throw new IllegalStateException("Failed to fetch block entity from buckets");
                                BlockPos aPos = new BlockPos(x, y, z);
                                if (!be2.getBlockPos().equals(aPos)) throw new IllegalStateException("Mismatched block entity position");
                            }
                        }
                    }
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
        getServer().createWorld(
            // safe blank world to test hoppers
            new WorldCreator("world_api_test")
                .environment(World.Environment.NORMAL)
                .bonusChest(false)
                .hardcore(false)
                .type(WorldType.AMPLIFIED)
        );
        getServer().getGlobalRegionScheduler().runDelayed(this, (task) -> {
            World apiTest = Bukkit.getWorld("world_api_test");
            Bukkit.unloadWorldAsync(Objects.requireNonNull(apiTest, "World cannot be null"), true).thenAccept((success) -> {
                if (success) {
                    getLogger().info("Successfully unloaded the world load/unload api test");
                } else {
                    getLogger().info("Couldn't unload the world load/unload api test");
                }
            });
        }, 20 * 20); // 20 seconds
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

    @EventHandler
    public void onWorldPreLoad(@NotNull WorldPreLoadEvent worldPreLoadEvent) {
        getLogger().info("WorldPreLoadEvent called with stage " + worldPreLoadEvent.getStage() + "!");
    }
}
