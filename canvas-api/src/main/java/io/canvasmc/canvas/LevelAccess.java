package io.canvasmc.canvas;

import io.canvasmc.canvas.region.Region;
import io.canvasmc.canvas.scheduler.WrappedTickLoop;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.Unmodifiable;
import java.util.List;
import java.util.function.Consumer;

public interface LevelAccess extends WrappedTickLoop {
    /**
     * Gets the Bukkit {@link World}
     * @return the world associated with this {@link LevelAccess} instance
     */
    World getWorld();
    /**
     * Schedules a task to the level thread
     * @param runnable the tick task
     */
    void scheduleOnThread(Runnable runnable);
    /**
     * Schedules a task to be ran before the next tick
     * @param runnable the tick task
     */
    void scheduleForPreNextTick(Runnable runnable);
    /**
     * Schedules a task to be ran after the next tick, or once the current tick is complete
     * @param runnable the tick task
     */
    void scheduleForPostNextTick(Runnable runnable);
    /**
     * If the level is actively ticking
     * @return true if the level is processing ticks
     */
    boolean isTicking();
    /**
     * Gets the scheduler impl for the level thread
     */
    BukkitScheduler getBukkitScheduler();

    // threaded regions
    /**
     * Gets all regions currently in this world
     * <br>
     * If {@link Server#isRegionized()} is false, this will be empty
     * @return regions in the world
     */
    @Unmodifiable
    List<Region> getAllRegions();

    /**
     * Runs a consumer on each region currently in the world
     * <br>
     * If {@link Server#isRegionized()} is false, this will do nothing
     * @param forEach the consumer
     */
    void forEachRegion(Consumer<Region> forEach);
}
