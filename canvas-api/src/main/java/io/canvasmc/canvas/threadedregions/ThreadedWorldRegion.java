package io.canvasmc.canvas.threadedregions;

import org.bukkit.Location;
import org.bukkit.World;

public interface ThreadedWorldRegion {
    /**
     * Get the location of the center chunk pos
     * <br>
     * This is equivalent to the middle of the center chunk at y=0
     *
     * @return the center location
     */
    Location getCenterChunkPos();

    /**
     * Get the dead section percent of this tick region
     * Note: </br>
     * 1.Dead percent is mean the percent of the unloaded chunk count of this tick region, which is also used for determine
     * that the tick region should or not check for splitting
     *
     * @return The dead section percent
     */
    double getDeadSectionPercent();

    /**
     * Get the world this region belongs to
     *
     * @return the world
     */
    World getWorld();

    /**
     * Get the tick data associated with this region
     * <br>
     * Please only use and access this on the owning thread context to avoid parallelism issues
     *
     * @return the tick data
     */
    WorldRegionData getTickData();

    /**
     * Get if the region dead section percent is 100%, meaning it has no alive sections
     * <br>
     * Note: Dead percent is mean the percent of the unloaded chunk count of this tick region, which is also used for determine
     * that the tick region should or not check for splitting
     *
     * @return if the region has no alive sections
     */
    boolean hasNoAliveSections();

    /**
     * Gets if the regions current state is TICKING
     *
     * @return if the region is ticking
     */
    boolean isTicking();

    /**
     * Gets if the regions current state is DEAD
     *
     * @return if the region is dead
     */
    boolean isDead();
}
