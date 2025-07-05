package io.canvasmc.canvas.event.region;

import io.canvasmc.canvas.threadedregions.ThreadedWorldRegion;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;
import org.jetbrains.annotations.NotNull;

public class RegionDestroyEvent extends ServerEvent {

    private static final HandlerList HANDLER_LIST = new HandlerList();
    private final ThreadedWorldRegion region;

    public RegionDestroyEvent(ThreadedWorldRegion region) {
        this.region = region;
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return HANDLER_LIST;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return HANDLER_LIST;
    }

    public ThreadedWorldRegion getRegion() {
        return region;
    }
}
