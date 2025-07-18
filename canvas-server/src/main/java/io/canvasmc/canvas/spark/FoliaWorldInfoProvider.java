package io.canvasmc.canvas.spark;

import io.papermc.paper.threadedregions.RegionizedServer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import me.lucko.spark.paper.common.platform.world.AbstractChunkInfo;
import me.lucko.spark.paper.common.platform.world.CountMap;
import me.lucko.spark.paper.common.platform.world.WorldInfoProvider;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.GameRule;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

public class FoliaWorldInfoProvider implements WorldInfoProvider {
    private final FoliaSparkPlugin plugin;
    private final Server server;

    public FoliaWorldInfoProvider(FoliaSparkPlugin plugin) {
        this.plugin = plugin;
        this.server = Bukkit.getServer();
    }

    @Override
    public CountsResult pollCounts() {
        int players = this.server.getOnlinePlayers().size();
        int entities = 0;
        int tileEntities = 0;
        int chunks = 0;

        for (World world : this.server.getWorlds()) {
            entities += world.getEntityCount();
            tileEntities += world.getTileEntityCount();
            chunks += world.getChunkCount();
        }

        return new CountsResult(players, entities, tileEntities, chunks);
    }

    @Override
    public ChunksResult<FoliaChunkInfo> pollChunks() {
        ChunksResult<FoliaChunkInfo> data = new ChunksResult<>();

        for (World world : this.server.getWorlds()) {
            Chunk[] chunks = world.getLoadedChunks();

            List<FoliaChunkInfo> list = new ArrayList<>(chunks.length);
            for (Chunk chunk : chunks) {
                if (chunk != null) {
                    list.add(new FoliaChunkInfo(chunk, world, this.plugin));
                }
            }

            data.put(world.getName(), list);
        }

        return data;
    }

    @Override
    public GameRulesResult pollGameRules() {
        GameRulesResult data = new GameRulesResult();

        boolean addDefaults = true; // add defaults in the first iteration
        for (World world : this.server.getWorlds()) {
            for (String gameRule : world.getGameRules()) {
                GameRule<?> ruleObj = GameRule.getByName(gameRule);
                if (ruleObj == null) {
                    continue;
                }

                if (addDefaults) {
                    Object defaultValue = world.getGameRuleDefault(ruleObj);
                    data.putDefault(gameRule, Objects.toString(defaultValue));
                }

                Object value = world.getGameRuleValue(ruleObj);
                data.put(gameRule, world.getName(), Objects.toString(value));
            }

            addDefaults = false;
        }

        return data;
    }

    @SuppressWarnings({"removal", "UnstableApiUsage"})
    @Override
    public Collection<DataPackInfo> pollDataPacks() {
        return this.server.getDataPackManager().getDataPacks().stream()
                .map(pack -> new DataPackInfo(
                        pack.getTitle(),
                        pack.getDescription(),
                        pack.getSource().name().toLowerCase(Locale.ROOT).replace("_", "")
                ))
                .collect(Collectors.toList());
    }

    static final class FoliaChunkInfo extends AbstractChunkInfo<EntityType> {
        private final CompletableFuture<CountMap<EntityType>> entityCounts;

        FoliaChunkInfo(Chunk chunk, World world, FoliaSparkPlugin plugin) {
            super(chunk.getX(), chunk.getZ());

            Executor executor = task -> RegionizedServer.getInstance().taskQueue.queueTickTaskQueue(((CraftWorld) world).getHandle(), getX(), getZ(), task);
            this.entityCounts = CompletableFuture.supplyAsync(() -> calculate(chunk), executor);
        }

        private CountMap<EntityType> calculate(Chunk chunk) {
            CountMap<EntityType> entityCounts = new CountMap.EnumKeyed<>(EntityType.class);
            for (Entity entity : chunk.getEntities()) {
                if (entity != null) {
                    entityCounts.increment(entity.getType());
                }
            }
            return entityCounts;
        }

        @Override
        public CountMap<EntityType> getEntityCounts() {
            try {
                return this.entityCounts.get(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Exception reading statistics for chunk " + getX() + ", " + getZ(), e);
            } catch (TimeoutException e) {
                throw new RuntimeException("Timed out waiting for statistics for chunk " + getX() + ", " + getZ(), e);
            }
        }

        @SuppressWarnings("deprecation")
        @Override
        public String entityTypeName(EntityType type) {
            return type.getName();
        }

    }

}
