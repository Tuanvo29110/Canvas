package io.canvasmc.canvas.chunk.flowsched;

public interface Task {

    void run(Runnable releaseLocks);

    void propagateException(Throwable t);

    LockToken[] lockTokens();

    int priority();

}
