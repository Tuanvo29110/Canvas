package io.canvasmc.canvas.spark;

import com.google.common.collect.ImmutableMap;
import me.lucko.spark.paper.common.monitor.ping.PlayerPingProvider;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import java.util.Map;

public class FoliaPlayerPingProvider implements PlayerPingProvider {

    private final Server server;

    public FoliaPlayerPingProvider(Server server) {
        this.server = server;
    }

    @Override
    public Map<String, Integer> poll() {
        ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        for (Player player : this.server.getOnlinePlayers()) {
            builder.put(player.getName(), player.getPing());
        }
        return builder.build();
    }
}
