package io.canvasmc.canvas.chunk.block;

import it.unimi.dsi.fastutil.Hash;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public class BlockPosStrategy implements Hash.Strategy<BlockPos> {
    @Override
    public int hashCode(@NotNull BlockPos pos) {
        int hash = pos.getX();
        hash = 31 * hash + pos.getY();
        hash = 31 * hash + pos.getZ();
        return hash;
    }

    @Override
    public boolean equals(BlockPos a, BlockPos b) {
        return a == b || (a != null && b != null &&
                          a.getX() == b.getX() &&
                          a.getY() == b.getY() &&
                          a.getZ() == b.getZ());
    }
}
