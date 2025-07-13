package io.canvasmc.canvas.spark;

import me.lucko.spark.paper.common.platform.PlatformInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.bukkit.Bukkit;
import org.bukkit.Server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class FoliaPlatformInfo implements PlatformInfo {
    private final Server server;

    public FoliaPlatformInfo(Server server) {
        this.server = server;
    }

    @Override
    public Type getType() {
        return Type.SERVER;
    }

    @Override
    public String getName() {
        return "Bukkit";
    }

    @Override
    public String getBrand() {
        return this.server.getName();
    }

    @Override
    public String getVersion() {
        return this.server.getVersion();
    }

    @Override
    public String getMinecraftVersion() {
        return this.server.getMinecraftVersion();
    }
}
