package io.canvasmc.canvas;

import io.canvasmc.canvas.config.*;
import io.canvasmc.canvas.config.annotation.Comment;
import io.canvasmc.canvas.config.internal.ConfigurationManager;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.Util;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Configuration("canvas_server")
public class Config {
    public static boolean RUNNING_IN_IDE = Boolean.getBoolean("minecraft.running-in-ide");
    public static ComponentLogger LOGGER = ComponentLogger.logger("Canvas");
    public static Config INSTANCE;

    @Comment(value = {
        "Folia is incompatible with spark normally. Canvas fixes it and implements a new spark plugin for Folia support",
        "If this option is enabled, Canvas will use the modified spark plugin internally for Folia support, otherwise it",
        "will use Papers spark implementation, which may have issues."
    })
    public boolean useOurSparkPlugin = true;

    public Chunks chunks = new Chunks();
    public static class Chunks {
        @Comment("Use euclidean distance squared for chunk task ordering. Makes the world load in what appears a circle rather than a diamond")
        public boolean useEuclideanDistanceSquared = true;

        @Comment("The thread priority for Canvas' rewritten chunk system executor")
        public int threadPoolPriority = Thread.NORM_PRIORITY;
    }

    private static <T extends Config> @NotNull ConfigSerializer<T> buildSerializer(Configuration config, Class<T> configClass) {
        ConfigurationUtils.extractKeys(configClass);
        Set<String> changes = new LinkedHashSet<>();
        return new AnnotationBasedYamlSerializer<>(SerializationBuilder.<T>newBuilder()
            .header(new String[]{
                "This is the main Canvas configuration file",
                "All configuration options here are made for vanilla-compatibility",
                "and not for performance. Settings must be configured specific",
                "to your hardware and server type. If you have questions",
                "join our discord at https://canvasmc.io/discord",
                "As a general rule of thumb, do NOT change a setting if",
                "you don't know what it does! If you don't know, ask!"
            })
            .handler(ConfigHandlers.ExperimentalProcessor::new)
            .handler(ConfigHandlers.CommentProcessor::new)
            .validator(ConfigHandlers.RangeProcessor::new)
            .validator(ConfigHandlers.NegativeProcessor::new)
            .validator(ConfigHandlers.PositiveProcessor::new)
            .validator(ConfigHandlers.NonNegativeProcessor::new)
            .validator(ConfigHandlers.NonPositiveProcessor::new)
            .validator(ConfigHandlers.PatternProcessor::new)
            .runtimeModifier("debug.*", new RuntimeModifier<>(boolean.class, (original) -> RUNNING_IN_IDE || original))
            .post(context -> {
                INSTANCE = context.configuration();
                if (RUNNING_IN_IDE) {
                    LOGGER.info("Running Minecraft development server in IDE.");
                }
                for (final String change : changes) {
                    LOGGER.info(change);
                }
            })
            .build(config, configClass), changes::add
        );
    }

    public static Config init() {
        long startNanos = Util.getNanos();
        ConfigurationManager.register(Config.class, Config::buildSerializer);
        LOGGER.info("Finished Canvas config init in {}ms", TimeUnit.MILLISECONDS.convert(Util.getNanos() - startNanos, TimeUnit.NANOSECONDS));
        return INSTANCE;
    }
}
