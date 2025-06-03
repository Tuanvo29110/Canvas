package io.canvasmc.canvas.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.canvasmc.canvas.Config;
import net.minecraft.Util;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AsyncPlayerDataSaving {
    public static final ExecutorService IO_POOL = new ThreadPoolExecutor(
        1, 1, 0L, TimeUnit.MILLISECONDS,
        new LinkedBlockingQueue<>(),
        new ThreadFactoryBuilder()
            .setPriority(Thread.NORM_PRIORITY - 2)
            .setNameFormat("Canvas PlayerData IO Thread")
            .setUncaughtExceptionHandler(Util::onThreadException)
            .build(),
        new ThreadPoolExecutor.DiscardPolicy()
    );

    public static Optional<Future<?>> submit(Runnable runnable) {
        if (!Config.INSTANCE.asyncPlayerDataSave) {
            runnable.run();
            return Optional.empty();
        } else {
            return Optional.of(IO_POOL.submit(runnable));
        }
    }
}
