package io.canvasmc.canvas.threadedregions;

import java.util.List;
import java.util.function.Consumer;

/**
 * Interface for accessing and interacting with region data managed by the ThreadedRegionizer.
 *
 * <p>This interface allows plugins or subsystems to query or iterate over {@link ThreadedWorldRegion}s</p>
 *
 * <p>Regions are guaranteed to be spatially unique, but may change size or contents over time
 * due to merging, splitting, or chunk addition/removal.</p>
 */
public interface ServerRegionizer {
    /**
     * Gets the region that owns the given chunk coordinate, using synchronized locking to ensure thread safety.
     * This method may be more expensive but guarantees the returned region is current and consistent.
     *
     * @param chunkX The X coordinate of the chunk.
     * @param chunkZ The Z coordinate of the chunk.
     * @return The {@link ThreadedWorldRegion} responsible for the chunk, or {@code null} if none exists.
     */
    ThreadedWorldRegion getRegionAtSynchronized(int chunkX, int chunkZ);

    /**
     * Gets the region that owns the given chunk coordinate without acquiring any locks.
     * This method is faster but may return a stale or inconsistent result if regions are actively mutating.
     *
     * @param chunkX The X coordinate of the chunk.
     * @param chunkZ The Z coordinate of the chunk.
     * @return The {@link ThreadedWorldRegion} responsible for the chunk, or {@code null} if none exists.
     */
    ThreadedWorldRegion getRegionAtUnsynchronized(int chunkX, int chunkZ);

    /**
     * Iterates over all regions currently managed by the regionizer in a thread-safe manner.
     * The provided consumer is called once for each region.
     *
     * @param regionConsumer A consumer to apply to each region, guaranteed to run with internal locking held.
     */
    void computeForAllRegionsSynchronized(Consumer<ThreadedWorldRegion> regionConsumer);

    /**
     * Iterates over all regions currently managed by the regionizer without acquiring any locks.
     * This method is faster but may observe inconsistent or stale data if regions are mutating concurrently.
     *
     * @param regionConsumer A consumer to apply to each region.
     */
    void computeForAllRegionsUnsynchronized(Consumer<ThreadedWorldRegion> regionConsumer);

    /**
     * Gets an immutable snapshot list of all regions currently managed by the regionizer.
     * This may include inactive or empty regions and is not guaranteed to be updated in real-time.
     *
     * @return A list of all {@link ThreadedWorldRegion}s.
     */
    List<ThreadedWorldRegion> getAllRegions();
}
