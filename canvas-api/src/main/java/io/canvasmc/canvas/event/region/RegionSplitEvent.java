package io.canvasmc.canvas.event.region;

import io.canvasmc.canvas.region.Region;
import org.bukkit.event.HandlerList;
import org.bukkit.event.server.ServerEvent;
import org.jetbrains.annotations.NotNull;
import java.util.List;

public class RegionSplitEvent extends ServerEvent {
    private static final HandlerList handlers = new HandlerList();
    private final Region from;
    private final List<Region> into;

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }

    public RegionSplitEvent(Region from, List<Region> into) {
        this.from = from;
        this.into = into;
    }

    public Region getFrom() {
        return from;
    }

    public List<Region> getInto() {
        return into;
    }
}
