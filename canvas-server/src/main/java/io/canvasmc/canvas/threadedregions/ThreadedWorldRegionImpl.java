package io.canvasmc.canvas.threadedregions;

import io.papermc.paper.threadedregions.RegionizedWorldData;
import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.TickRegions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import org.bukkit.Location;
import org.bukkit.World;

public class ThreadedWorldRegionImpl implements ThreadedWorldRegion {
    private final ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData> internal;

    public ThreadedWorldRegionImpl(ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData> internal) {
        this.internal = internal;
    }

    @Override
    public Location getCenterChunkPos() {
        ChunkPos nullableChunk = this.internal.getCenterChunk();
        if (nullableChunk == null) {
            return null;
        }
        BlockPos centerPos = nullableChunk.getMiddleBlockPosition(0);
        return new Location(this.internal.regioniser.world.getWorld(), centerPos.getX(), centerPos.getY(), centerPos.getZ());
    }

    @Override
    public double getDeadSectionPercent() {
        return internal.getDeadSectionPercent();
    }

    @Override
    public World getWorld() {
        return internal.regioniser.world.getWorld();
    }

    @Override
    public WorldRegionData getTickData() {
        RegionizedWorldData worldData = internal.getData().getRegionizedData(internal.regioniser.world.worldRegionData);
        if (worldData == null) {
            throw new IllegalStateException("Cannot request tick data when region tick data hasn't been created yet");
        }
        return worldData.apiHandle;
    }

    @Override
    public boolean hasNoAliveSections() {
        return this.internal.hasNoAliveSections();
    }

    @Override
    public boolean isTicking() {
        return this.internal.isTicking();
    }

    @Override
    public boolean isDead() {
        return this.internal.isDead();
    }

    @Override
    public String toString() {
        return this.internal.toString();
    }
}
