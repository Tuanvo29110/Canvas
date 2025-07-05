package io.canvasmc.canvas.threadedregions;

import io.papermc.paper.threadedregions.ThreadedRegionizer;
import io.papermc.paper.threadedregions.TickRegions;
import io.papermc.paper.threadedregions.TickRegions.TickRegionData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ServerRegionizerImpl implements ServerRegionizer {
    private final ThreadedRegionizer<TickRegionData, TickRegions.TickRegionSectionData> regionizer;

    public ServerRegionizerImpl(final ThreadedRegionizer regionizer) {
        this.regionizer = (ThreadedRegionizer<TickRegionData, TickRegions.TickRegionSectionData>) regionizer;
    }

    @Override
    public ThreadedWorldRegion getRegionAtSynchronized(final int chunkX, final int chunkZ) {
        ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData> region =
            this.regionizer.getRegionAtSynchronised(chunkX, chunkZ);
        return region == null ? null : region.apiHandle;
    }

    @Override
    public ThreadedWorldRegion getRegionAtUnsynchronized(final int chunkX, final int chunkZ) {
        ThreadedRegionizer.ThreadedRegion<TickRegions.TickRegionData, TickRegions.TickRegionSectionData> region =
            this.regionizer.getRegionAtUnsynchronised(chunkX, chunkZ);
        return region == null ? null : region.apiHandle;
    }

    @Override
    public void computeForAllRegionsSynchronized(final Consumer<ThreadedWorldRegion> regionConsumer) {
        this.regionizer.computeForAllRegions((region) -> {
            regionConsumer.accept(region.apiHandle);
        });
    }

    @Override
    public void computeForAllRegionsUnsynchronized(final Consumer<ThreadedWorldRegion> regionConsumer) {
        this.regionizer.computeForAllRegionsUnsynchronised((region) -> {
            regionConsumer.accept(region.apiHandle);
        });
    }

    @Override
    public List<ThreadedWorldRegion> getAllRegions() {
        ObjectArrayList<ThreadedWorldRegion> regions = new ObjectArrayList<>();
        computeForAllRegionsUnsynchronized(regions::add);
        return regions;
    }
}
