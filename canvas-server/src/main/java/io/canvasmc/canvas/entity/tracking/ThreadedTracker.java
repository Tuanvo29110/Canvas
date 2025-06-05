package io.canvasmc.canvas.entity.tracking;

import ca.spottedleaf.moonrise.common.misc.NearbyPlayers;
import ca.spottedleaf.moonrise.common.util.TickThread;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.entity_tracker.EntityTrackerEntity;
import io.canvasmc.canvas.Config;
import io.canvasmc.canvas.region.ServerRegions;
import io.canvasmc.canvas.util.NamedAgnosticThreadFactory;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.FullChunkStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ThreadedTracker {
    private static final ThreadPoolExecutor processor = new ThreadPoolExecutor(
        Config.INSTANCE.entities.entityTracking.maxProcessors,
        Integer.MAX_VALUE,
        Config.INSTANCE.entities.entityTracking.keepAlive, TimeUnit.SECONDS,
        new LinkedBlockingQueue<>(),
        new NamedAgnosticThreadFactory<>("EntityTracking", TrackerThread::new, Thread.NORM_PRIORITY - 2)
    );
    public static ThreadedTracker INSTANCE = new ThreadedTracker(Config.INSTANCE.entities.entityTracking.enableThreadedTracking);
    public final boolean enableThreading;
    public static final AtomicBoolean canceled = new AtomicBoolean(false);

    ThreadedTracker(boolean enableThreading) {
        this.enableThreading = enableThreading || Config.INSTANCE.ticking.enableThreadedRegionizing; // if we are regionized, enable threading
    }

    public static ThreadPoolExecutor getProcessor() {
        return processor;
    }

    public boolean tick(@NotNull ServerLevel world) {
        if (this.enableThreading) {
            if (canceled.get()) return true;
            final NearbyPlayers nearbyPlayers = world.moonrise$getNearbyPlayers();
            final Entity[] trackerEntitiesRaw = ServerRegions.getTickData((ServerLevel) world).trackerEntities.getRawDataUnchecked(); // Canvas - Threaded Regions

            processor.execute(() -> {
                for (final Entity entity : trackerEntitiesRaw) {
                    if (entity == null) continue;

                    final ChunkMap.TrackedEntity trackedInstance = ((EntityTrackerEntity) entity).moonrise$getTrackedEntity();
                    if (trackedInstance == null) {
                        continue;
                    }

                    trackedInstance.moonrise$tick(nearbyPlayers.getChunk(entity.chunkPosition()));
                    @Nullable FullChunkStatus chunkStatus = ((ca.spottedleaf.moonrise.patches.chunk_system.entity.ChunkSystemEntity)entity).moonrise$getChunkStatus(); // Canvas
                    if ((trackedInstance).moonrise$hasPlayers()
                        || (chunkStatus == null || chunkStatus.isOrAfter(FullChunkStatus.ENTITY_TICKING))) {
                        trackedInstance.serverEntity.sendChanges();
                    }
                }
            });
            return true;
        }
        return false;
    }

    public static class TrackerThread extends TickThread {
        public TrackerThread(final ThreadGroup group, final Runnable runnable, final String name) {
            super(group, runnable, name);
        }
    }
}
