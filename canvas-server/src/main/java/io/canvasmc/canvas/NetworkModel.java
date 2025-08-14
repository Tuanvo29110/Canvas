package io.canvasmc.canvas;

import java.util.Locale;

public enum NetworkModel {
    IO_URING("io_uring"),
    KQUEUE("kqueue"),
    EPOLL("epoll"),
    NIO("nio");

    private final String name;

    NetworkModel(String name) {
        this.name = name;
    }

    // Note: required for configuration to allow lowercase entries
    public static NetworkModel lenientParse(String name) {
        if (name == null) return EPOLL;

        return switch (name.toLowerCase(Locale.ROOT)) {
            case "io_uring" -> IO_URING;
            case "kqueue" -> KQUEUE;
            case "nio" -> NIO;
            default -> EPOLL;
        };
    }

    public static NetworkModel fromProperty() {
        return Config.INSTANCE.networking.nativeTransportType;
    }

    public String getName() {
        return this.name;
    }
}
