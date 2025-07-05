package io.canvasmc.canvas.threadedregions;

import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

/**
 * Represents a Folia/Canvas region
 */
public interface WorldRegionData {

    /**
     * Gets the world this region belongs to.
     *
     * @return the associated {@link World}
     */
    World getWorld();

    /**
     * Retrieves the center {@link Chunk}
     *
     * @return center chunk of this region
     */
    Chunk getCenterChunk();

    /**
     * Lists all owned chunks
     *
     * @return owned chunks within this region
     */
    List<Chunk> getOwnedChunks();

    /**
     * Lists currently ticking chunks in this region.
     *
     * @return list of ticking chunks
     */
    List<Chunk> getTickingChunks();

    /**
     * Number of chunks owned by this region.
     *
     * @return total owned chunks count
     */
    int getChunkCount();

    /**
     * Lists all local players
     *
     * @return players present in this region
     */
    List<Player> getLocalPlayers();

    /**
     * Count of players currently inside this region.
     *
     * @return number of players
     */
    int getPlayerCount();

    /**
     * Count of entities currently loaded in the region
     *
     * @return number of entities
     */
    int getEntityCount();

    /**
     * Gets the {@link ThreadedWorldRegion} for this region data
     *
     * @return the region
     */
    ThreadedWorldRegion getRegionizerRegion();
}
