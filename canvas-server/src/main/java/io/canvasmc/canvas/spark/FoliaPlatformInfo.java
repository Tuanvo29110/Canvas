package io.canvasmc.canvas.spark;

import me.lucko.spark.paper.common.platform.PlatformInfo;
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
        try {
            return this.server.getMinecraftVersion();
        } catch (NoSuchMethodError e) {
            // ignore
        }

        Class<? extends Server> serverClass = this.server.getClass();
        try {
            Field minecraftServerField = serverClass.getDeclaredField("console");
            minecraftServerField.setAccessible(true);

            Object minecraftServer = minecraftServerField.get(this.server);
            Class<?> minecraftServerClass = minecraftServer.getClass();

            Method getVersionMethod = minecraftServerClass.getDeclaredMethod("getVersion");
            getVersionMethod.setAccessible(true);

            return (String) getVersionMethod.invoke(minecraftServer);
        } catch (Exception e) {
            // ignore
        }

        return serverClass.getPackage().getName().split("\\.")[3];
    }
}
