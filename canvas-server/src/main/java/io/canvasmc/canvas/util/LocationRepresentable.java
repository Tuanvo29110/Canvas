package io.canvasmc.canvas.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public interface LocationRepresentable<T> {
    T get();
    BlockPos position();
    ServerLevel world();
}
