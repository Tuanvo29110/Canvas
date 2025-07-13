package io.canvasmc.testplugin;

import com.destroystokyo.paper.event.player.PlayerTeleportEndGatewayEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class TestPlugin extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getLogger().info("Enabling test plugin for Canvas");
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling test plugin for Canvas");
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent playerRespawnEvent) {
        getLogger().info("PlayerRespawnEvent called!");
        // uncomment when testing respawn location modification
        // playerRespawnEvent.setRespawnLocation(new Location(
        //     Bukkit.getWorld("world"), 0, 9000, 0
        // ));
        // uncomment when testing player kicking during respawn
        // playerRespawnEvent.getPlayer().kick(Component.text("Bye bye"));
    }

    @EventHandler
    public void onTeleportPlayer(PlayerTeleportEvent playerTeleportEvent) {
        getLogger().info("PlayerTeleportEvent called!");
    }

    @EventHandler
    public void onTeleportEntity(EntityTeleportEvent playerTeleportEvent) {
        getLogger().info("EntityTeleportEvent called!");
    }

    @EventHandler
    public void onTeleportEndGateway(PlayerTeleportEndGatewayEvent playerTeleportEndGatewayEvent) {
        getLogger().info("PlayerTeleportEndGatewayEvent called!");
    }
}
