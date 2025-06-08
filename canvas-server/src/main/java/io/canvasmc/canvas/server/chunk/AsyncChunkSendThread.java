package io.canvasmc.canvas.server.chunk;

import ca.spottedleaf.moonrise.common.util.TickThread;

public class AsyncChunkSendThread extends TickThread {

    protected AsyncChunkSendThread(Runnable task) {
        super(task);
    }
}
