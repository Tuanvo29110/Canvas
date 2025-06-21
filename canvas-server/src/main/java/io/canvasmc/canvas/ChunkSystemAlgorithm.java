package io.canvasmc.canvas;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import io.netty.util.internal.PlatformDependent;
import java.util.function.BiFunction;
import org.jetbrains.annotations.NotNull;
import oshi.util.tuples.Pair;

public enum ChunkSystemAlgorithm {
    MOONRISE((configWorkerThreads, configIoThreads) -> {
        int defaultWorkerThreads = Runtime.getRuntime().availableProcessors() / 2;
        if (defaultWorkerThreads <= 4) {
            defaultWorkerThreads = defaultWorkerThreads <= 3 ? 1 : 2;
        } else {
            defaultWorkerThreads = defaultWorkerThreads / 2;
        }
        defaultWorkerThreads = Integer.getInteger(PlatformHooks.get().getBrand() + ".WorkerThreadCount", Integer.valueOf(defaultWorkerThreads));

        int workerThreads = configWorkerThreads;

        if (workerThreads <= 0) {
            workerThreads = defaultWorkerThreads;
        }
        return new Pair<>(workerThreads, Math.max(1, configIoThreads));
    }),
    C2ME_AGGRESSIVE((configWorkerThreads, configIoThreads) -> {
        int eval;
        if (configWorkerThreads <= 0) {
            // evaluate default
            boolean isWindows = PlatformDependent.isWindows();
            int cpus = Runtime.getRuntime().availableProcessors();
            double memGb = Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 1024.0;

            double cpuBased = isWindows ? (cpus / 1.6) : (cpus / 1.3);
            double memBased = (memGb - 0.5) / 0.6;

            eval = (int) Math.max(1, Math.min(cpuBased, memBased));
        } else eval = configWorkerThreads;
        return new Pair<>(eval, Math.max(1, configIoThreads));
    }),
    C2ME((configWorkerThreads, configIoThreads) -> {
        int eval;
        if (configWorkerThreads <= 0) {
            // evaluate default
            boolean isWindows = PlatformDependent.isWindows();
            boolean isJ9vm = PlatformDependent.isJ9Jvm();
            int cpus = Runtime.getRuntime().availableProcessors();
            double memGb = Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 1024.0;

            double cpuBased = isWindows ? (cpus / 1.6 - 2) : (cpus / 1.2 - 2);

            double memOffset = isJ9vm ? 0.2 : 0.6;
            double memDivisor = isJ9vm ? 0.4 : 0.6;
            double memBased = (memGb - memOffset) / memDivisor;

            eval = (int) Math.max(1, Math.min(cpuBased, memBased));
        } else eval = configWorkerThreads;
        return new Pair<>(eval, Math.max(1, configIoThreads));
    });

    private final BiFunction<Integer, Integer, Pair<Integer, Integer>> eval;

    ChunkSystemAlgorithm(BiFunction<Integer, Integer, Pair<Integer, Integer>> eval) {
        this.eval = eval;
    }

    public int evalWorkers(final int configWorkerThreads, final int configIoThreads) {
        return eval.apply(configWorkerThreads, configIoThreads).getA();
    }

    public int evalIO(final int configWorkerThreads, final int configIoThreads) {
        return eval.apply(configWorkerThreads, configIoThreads).getB();
    }

    public @NotNull String asDebugString() {
        return this + "(" + evalWorkers(-1, -1) + ")";
    }
}
