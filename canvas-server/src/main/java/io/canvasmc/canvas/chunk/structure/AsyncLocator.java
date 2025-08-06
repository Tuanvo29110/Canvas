package io.canvasmc.canvas.chunk.structure;

import ca.spottedleaf.moonrise.common.util.TickThread;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.mojang.datafixers.util.Pair;
import io.canvasmc.canvas.Config;
import io.canvasmc.canvas.util.LocationRepresentable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.jetbrains.annotations.NotNull;

public class AsyncLocator {

    private static final ExecutorService SERVICE;

    static {
        int threads = Config.INSTANCE.asyncLocator.threads;
        SERVICE = new ThreadPoolExecutor(
            1,
            threads,
            Config.INSTANCE.asyncLocator.keepalive,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder()
                .setThreadFactory(
                    r -> new AsyncLocatorThread(r, "Canvas Async Locator") {
                        @Override
                        public void run() {
                            r.run();
                        }
                    }
                )
                .setNameFormat("Canvas Async Locator - %d")
                .setPriority(Thread.NORM_PRIORITY - 2)
                .build()
        );
    }

    private AsyncLocator() {
    }

    public static void shutdownExecutorService() {
        if (SERVICE != null) {
            SERVICE.shutdown();
        }
    }

    public static @NotNull LocateTask<BlockPos> locate(
        @NotNull ServerLevel level,
        TagKey<Structure> structureTag,
        BlockPos pos,
        int searchRadius,
        boolean skipKnownStructures
    ) {
        CompletableFuture<LocationRepresentable<BlockPos>> completableFuture = new CompletableFuture<>();
        Future<?> future = SERVICE.submit(
            () -> doLocateLevel(completableFuture, level, structureTag, pos, searchRadius, skipKnownStructures)
        );
        return new LocateTask<>(level.getServer(), completableFuture, future);
    }

    public static @NotNull LocateTask<Pair<BlockPos, Holder<Structure>>> locate(
        @NotNull ServerLevel level,
        HolderSet<Structure> structureSet,
        BlockPos pos,
        int searchRadius,
        boolean skipKnownStructures
    ) {
        CompletableFuture<LocationRepresentable<Pair<BlockPos, Holder<Structure>>>> completableFuture = new CompletableFuture<>();
        Future<?> future = SERVICE.submit(
            () -> doLocateChunkGenerator(completableFuture, level, structureSet, pos, searchRadius, skipKnownStructures)
        );
        return new LocateTask<>(level.getServer(), completableFuture, future);
    }

    private static void doLocateLevel(
        @NotNull CompletableFuture<LocationRepresentable<BlockPos>> completableFuture,
        @NotNull ServerLevel level,
        TagKey<Structure> structureTag,
        BlockPos pos,
        int searchRadius,
        boolean skipExistingChunks
    ) {
        BlockPos foundPos = null;
        try {
            foundPos = level.findNearestMapStructure(structureTag, pos, searchRadius, skipExistingChunks);
            final BlockPos finalFoundPos = foundPos;
            completableFuture.complete(new LocationRepresentable<>() {
                @Override
                public BlockPos get() {
                    return finalFoundPos;
                }

                @Override
                public BlockPos position() {
                    return get();
                }

                @Override
                public ServerLevel world() {
                    return level;
                }
            });
        } catch (Exception e) {
            MinecraftServer.LOGGER.error("Unable to locate structure async", e);
        }
    }

    private static void doLocateChunkGenerator(
        @NotNull CompletableFuture<LocationRepresentable<Pair<BlockPos, Holder<Structure>>>> completableFuture,
        @NotNull ServerLevel level,
        HolderSet<Structure> structureSet,
        BlockPos pos,
        int searchRadius,
        boolean skipExistingChunks
    ) {
        Pair<BlockPos, Holder<Structure>> foundPair = null;
        try {
            foundPair = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, structureSet, pos, searchRadius, skipExistingChunks);
            final Pair<BlockPos, Holder<Structure>> finalFoundPair = foundPair;
            completableFuture.complete(new LocationRepresentable<>() {
                @Override
                public Pair<BlockPos, Holder<Structure>> get() {
                    return finalFoundPair;
                }

                @Override
                public BlockPos position() {
                    return get().getFirst();
                }

                @Override
                public ServerLevel world() {
                    return level;
                }
            });
        } catch (Exception e) {
            MinecraftServer.LOGGER.error("Unable to locate structure async", e);
        }
    }

    public static class AsyncLocatorThread extends TickThread {
        private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);

        public AsyncLocatorThread(Runnable run, String name) {
            super(null, run, name, THREAD_COUNTER.incrementAndGet());
        }

        @Override
        public void run() {
            super.run();
        }
    }

    public record LocateTask<T>(MinecraftServer server, CompletableFuture<LocationRepresentable<T>> completableFuture,
                                Future<?> taskFuture) {
        public LocateTask<T> then(Consumer<LocationRepresentable<T>> action) {
            completableFuture.thenAccept(action);
            return this;
        }

        // Note: nullability handler must be thread-safe. this is run on the executor service, not on region
        public LocateTask<T> thenOnRegionOrNull(Consumer<LocationRepresentable<T>> action, Runnable nullabilityHandler) {
            completableFuture.thenAccept(pos -> {
                try {
                    if (pos == null || pos.get() == null) {
                        nullabilityHandler.run();
                        return;
                    }
                    io.papermc.paper.threadedregions.RegionizedServer.getInstance().taskQueue.queueTickTaskQueue(
                        pos.world(), pos.position().getX(), pos.position().getZ(),
                        () -> action.accept(pos)
                    );
                } catch (Throwable thrown) {
                    MinecraftServer.LOGGER.error("Unable to run callback", thrown);
                }
            });
            return this;
        }

        public void cancel() {
            taskFuture.cancel(true);
            completableFuture.cancel(false);
        }
    }
}
