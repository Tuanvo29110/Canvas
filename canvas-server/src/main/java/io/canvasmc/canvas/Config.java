package io.canvasmc.canvas;

import ca.spottedleaf.moonrise.common.util.MoonriseConstants;
import ca.spottedleaf.moonrise.patches.chunk_system.util.ParallelSearchRadiusIteration;
import io.canvasmc.canvas.chunk.FluidPostProcessingMode;
import io.canvasmc.canvas.config.AnnotationBasedYamlSerializer;
import io.canvasmc.canvas.config.ConfigHandlers;
import io.canvasmc.canvas.config.ConfigSerializer;
import io.canvasmc.canvas.config.Configuration;
import io.canvasmc.canvas.config.ConfigurationUtils;
import io.canvasmc.canvas.config.RuntimeModifier;
import io.canvasmc.canvas.config.SerializationBuilder;
import io.canvasmc.canvas.config.annotation.Comment;
import io.canvasmc.canvas.config.annotation.NamespacedKey;
import io.canvasmc.canvas.config.internal.ConfigurationManager;
import io.canvasmc.canvas.entity.EntityCollisionMode;
import io.canvasmc.canvas.util.YamlTextFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

@Configuration("canvas-server")
public class Config {
    public static boolean RUNNING_IN_IDE = Boolean.getBoolean("minecraft.running-in-ide");
    public static ComponentLogger LOGGER = ComponentLogger.logger("Canvas");
    public static Config INSTANCE;

    public Chunks chunks = new Chunks();
    public static class Chunks {
        @Comment("Use euclidean distance squared for chunk task ordering. Makes the world load in what appears a circle rather than a diamond")
        public boolean useEuclideanDistanceSquared = true;

        @Comment("The thread priority for Canvas' rewritten chunk system executor")
        public int threadPoolPriority = Thread.NORM_PRIORITY;

        @Comment({
            "Determines the fluid post processing mode.",
            "The worldgen processes creates a lot of unnecessary fluid post-processing tasks,",
            "which can overload the server thread and cause stutters.",
            "There are 3 accepted values",
            " - VANILLA - just normal vanilla, no changes",
            " - DISABLED - disables fluid post processing completely",
            " - FILTERED - applies a rough filter to filter out fluids that are definitely not going to flow"
        })
        public FluidPostProcessingMode fluidPostProcessingMode = FluidPostProcessingMode.VANILLA;

        @Comment({
            "Whether to turn fluid postprocessing into scheduled tick",
            "",
            "Fluid post-processing is very expensive when loading in new chunks, and this can affect",
            "MSPT significantly. This option delays fluid post-processing to scheduled tick to hopefully mitigate this issue."
        })
        public boolean fluidPostProcessingToScheduledTick = false;

        @Comment("Whether to enable aquifer optimizations to accelerate overworld worldgen")
        public boolean optimizeAquifer = false;

        @Comment("Whether to enable End Biome Cache to accelerate The End worldgen")
        public boolean useEndBiomeCache = false;

        @Comment("The cache capacity for the end biome cache. Only works with 'useEndBiomeCache' enabled")
        public int endBiomeCacheCapacity = 1024;

        @Comment("Whether to enable Beardifier optimizations to accelerate world generation")
        public boolean optimizeBeardifier = false;

        @Comment("Whether to enable optimizations to the noise based chunk generator")
        public boolean optimizeNoiseGeneration = false;

        public BiomeCache biomeCache = new BiomeCache();
        public static class BiomeCache {
            @Comment("Enables biome caching, which makes a biome lookup caching layer to reduce expensive biome calculations and queries")
            public boolean enabled = false;
            @Comment("Enables advancement-related biome checks for biome caching")
            public boolean advancement = false;
            @Comment("Enables biome caching for mob spawning biome lookups")
            public boolean mobSpawn = false;
        }

        @Comment({
            "Once one task is completed then the next task starts immediately, to prevent blocking threads while waiting to complete all tasks",
            "WARNING: May cause the sequence of future compose disorder"
        })
        public boolean useFasterStructureGenFutureSequencing = false;

        @Comment({
            "Makes chunk packet preparation and sending asynchronous to improve server performance.",
            "This can significantly reduce main thread load when many players are loading chunks."
        })
        public boolean asyncChunkSend = false;

        @Comment({
            "Changes the maximum view distance for the server, allowing clients to have",
            "render distances higher than 32"
        })
        public int maxViewDistance = 32;
    }

    public Networking networking = new Networking();
    public static class Networking {
        @Comment({
            "The clientbound set entity motion packet can often cause high network (Netty) usage and consumes (on larger production servers)",
            "up to 60% of your network usage. Disabling this has minimal side effects, such as squids and glow squids swimming upright until attacked."
        })
        public boolean disableClientboundSetEntityMotionPacket = false;
    }

    @Comment("Configurations for enabling virtual threads for different thread pool executors")
    public VirtualThreads virtualThreads = new VirtualThreads();
    public static class VirtualThreads {
        @Comment("Enables virtual thread usage for the async scheduler executor")
        public boolean asyncScheduler = false;

        @Comment("Enables virtual thread usage for the chat executor")
        public boolean chatExecutor = false;

        @Comment("Enables virtual thread usage for the authenticator pool")
        public boolean authenticatorPool = false;

        @Comment("Enables virtual thread usage for the text filter executor")
        public boolean serverTextFilter = false;

        @Comment("Enables virtual thread usage for the text filter executor")
        public boolean tabCompleteExecutor = false;

        @Comment("Enables virtual thread usage for the profile lookup executor")
        public boolean profileLookupExecutor = false;

        @Comment("Enables virtual thread usage for the command sending pool")
        public boolean commandSendingPool = false;
    }

    public Entities entities = new Entities();
    public static class Entities {
        @Comment("When enabled, hides flames on entities with fire resistance")
        public boolean hideFlamesOnEntitiesWithFireResistance = false;

        @Comment("Filters entity movement packets to reduce the amount of useless move packets sent")
        public boolean reduceUselessMovePackets = false;
    }

    @Comment("Check if a cactus can survive before growing. Heavily optimizes cacti farms")
    public boolean cactusCheckSurvivalBeforeGrowth = false;

    @Comment("Whether to cache expensive CraftEntityType#minecraftToBukkit call")
    public boolean enableCachedMTBEntityTypeConvert = false;

    @Comment("Enables creation of tile entity snapshots on retrieving blockstates")
    public boolean tileEntitySnapshotCreation = false;

    @Comment("Determines if end crystals should explode in a chain reaction, similar to how tnt works when exploded")
    public boolean chainEndCrystalExplosions = false;

    @Comment("Disables falling on farmland turning it back to dirt")
    public boolean disableFarmlandTrampling = false;

    @Comment("Makes farmland always moist, never drying out, even if it isn't near water")
    public boolean farmlandAlwaysMoist = false;

    public AsyncLocator asyncLocator = new AsyncLocator();
    public static class AsyncLocator {
        @Comment("The amount of threads allocated to the async locator")
        public int threads = 1;

        @Comment("The keepalive time in seconds for the async locator")
        public int keepalive = 60;
    }

    @Comment("Disables Minecraft Chat Signing to prevent player reporting")
    public boolean enableNoChatReports = false;

    @Comment("Restores vanilla loading and unloading behavior broken by Folia")
    public boolean restoreVanillaEnderPearlBehavior = false;

    @Comment("Enables snowballs being able to knockback players")
    public boolean snowballCanKnockback = false;

    @Comment("Enables eggs being able to knockback players")
    public boolean eggCanKnockback = false;

    @Comment({
        "The entity collision mode for the server",
        "",
        "Acceptable values:",
        " - VANILLA - default, all entities have collisions",
        " - ONLY_PUSHABLE_PLAYERS_LARGE - only players are pushable by entities, we search in a large radius(8 chunks)",
        "        for colliding players. This is primarily used for if servers have very large entities via the scale attribute",
        "        or custom entities plugin",
        " - ONLY_PUSHABLE_PLAYERS_SMALL - only players are pushable by entities, we search in a small radius(2 chunks)",
        "        for colliding players. This is used for if the server will have no large entities exceeding 2 chunks of width",
        " - NO_COLLISIONS - all entities have no collisions"
    })
    public EntityCollisionMode entityCollisionMode = EntityCollisionMode.VANILLA;

    // TODO - check these on minecraft updates
    public Fixes fixes = new Fixes();
    public static class Fixes {
        @Comment({
            "Fixes MC-298464 - https://bugs.mojang.com/browse/MC/issues/MC-298464",
            "Memory leak in hoglin farm due to CHANGED_DIMENSION entity removal"
        })
        public boolean mc298464 = false;

        @Comment({
            "Fixes MC-223153 - https://bugs.mojang.com/browse/MC/issues/MC-223153",
            "Block of Raw Copper uses stone sounds instead of copper sounds"
        })
        public boolean mc223153 = false;

        @Comment({
            "Fixes MC-119417 - https://bugs.mojang.com/browse/MC/issues/MC-119417",
            "A spectator can occupy a bed if they enter it and then are switched to spectator mode"
        })
        public boolean mc119417 = false;

        @Comment({
            "Fixes MC-200418 - https://bugs.mojang.com/browse/MC/issues/MC-200418",
            "Cured baby zombie villagers stay as jockey variant"
        })
        public boolean mc200418 = false;

        @Comment({
            "Fixes MC-200418 - https://bugs.mojang.com/browse/MC/issues/MC-94054",
            "Cave spiders spin around when walking"
        })
        public boolean mc94054 = false;

        @Comment({
            "Fixes MC-245394 - https://bugs.mojang.com/browse/MC/issues/MC-245394",
            "The sounds of raid horns blaring aren't controlled by the correct sound slider"
        })
        public boolean mc245394 = false;

        @Comment({
            "Fixes MC-231743 - https://bugs.mojang.com/browse/MC/issues/MC-231743",
            "minecraft.used:minecraft.POTTABLE_PLANT doesn't increase when placing plants into flower pots"
        })
        public boolean mc231743 = false;

        @Comment({
            "Fixes MC-227337 - https://bugs.mojang.com/browse/MC/issues/MC-227337",
            "When a shulker bullet hits an entity, the explodes sound is not played and particles are not produced"
        })
        public boolean mc227337 = false;

        @Comment({
            "Fixes MC-221257 - https://bugs.mojang.com/browse/MC/issues/MC-221257",
            "Shulker bullets don't produce bubble particles when moving through water"
        })
        public boolean mc221257 = false;

        @Comment({
            "Fixes MC-206922 - https://bugs.mojang.com/browse/MC/issues/MC-206922",
            "Items dropped by entities that are killed by lightning instantly disappear"
        })
        public boolean mc206922 = false;

        @Comment({
            "Fixes MC-155509 - https://bugs.mojang.com/browse/MC/issues/MC-155509",
            "Puffed pufferfish can hurt the player while dying"
        })
        public boolean mc155509 = false;

        @Comment({
            "Fixes MC-132878 - https://bugs.mojang.com/browse/MC/issues/MC-132878",
            "Armor stands destroyed by explosions/lava/fire don't produce particles"
        })
        public boolean mc132878 = false;

        @Comment({
            "Fixes MC-129909 - https://bugs.mojang.com/browse/MC/issues/MC-129909",
            "Players in spectator mode continue to consume foods and liquids shortly after switching game modes",
            "This also fixes MC-81773 - https://bugs.mojang.com/browse/MC/issues/MC-81773",
            "Bows and tridents drawn in survival/creative/adventure mode can be released in spectator mode"
        })
        public boolean mc129909 = false;

        @Comment({
            "Fixes MC-121706 - https://bugs.mojang.com/browse/MC/issues/MC-121706",
            "Skeletons and illusioners aren't looking up / down at their target while strafing"
        })
        public boolean mc121706 = false;

        @Comment({
            "Fixes MC-119754 - https://bugs.mojang.com/browse/MC/issues/MC-119754",
            "Firework boosting on elytra continues in spectator mode"
        })
        public boolean mc119754 = false;

        @Comment({
            "Fixes MC-100991 - https://bugs.mojang.com/browse/MC/issues/MC-100991",
            "Killing entities with a fishing rod doesn't count as a kill"
        })
        public boolean mc100991 = false;

        @Comment({
            "Fixes MC-69216 - https://bugs.mojang.com/browse/MC/issues/MC-69216",
            "Switching to spectator mode while fishing keeps rod cast"
        })
        public boolean mc69216 = false;

        @Comment({
            "Fixes MC-30391 - https://bugs.mojang.com/browse/MC/issues/MC-30391",
            "Chickens, blazes and the wither emit particles when landing from a height, despite falling slowly"
        })
        public boolean mc30391 = false;

        @Comment({
            "Fixes MC-2025 - https://bugs.mojang.com/browse/MC/issues/MC-2025",
            "Mobs going out of fenced areas/suffocate in blocks when loading chunks"
        })
        public boolean mc2025 = false;

        @Comment({
            "Fixes MC-183990 - https://bugs.mojang.com/browse/MC/issues/MC-183990",
            "Group AI of some mobs breaks when their target dies"
        })
        public boolean mc183990 = false;

        @Comment({
            "Fixes MC-136249 - https://bugs.mojang.com/browse/MC/issues/MC-136249",
            "Wearing boots enchanted with depth strider decreases the strength of the riptide enchantment"
        })
        public boolean mc136249 = false;

        @Comment({
            "Fixes MC-258859 - https://bugs.mojang.com/browse/MC/issues/MC-258859",
            "Steep surface rule condition only works on the north and east faces of slopes"
        })
        public boolean mc258859 = false;
    }

    @Comment({
        "Enables better XP orb merging and removes the XP pickup delay",
        "Can be very useful for heavy XP farms",
        "This completely changes how orbs are merged, allowing for 1 single orb",
        "to contain an infinite amount of experience and is fully collected instantly",
        "rather than 1 xp per tick like with Vanilla. This is because we change the",
        "criteria for orbs to be merged, and instead of increasing the count, we",
        "increase the value of the orb. This way orbs are collected instantly, there",
        "will be no \"ghost orbs\", and all xp merging is as efficient as possible"
    })
    public boolean fastOrbs = false;

    @Comment({
        "Enables a regionized TPS-Bar implementation for Canvas",
        "This function is per-player, with this as a global setting to disable it",
        "To enable the tps-bar per-player, use the '/tpsbar' command"
    })
    public boolean enableTpsBar = true;

    @Comment(value = {
        "The default respawn dimension for the server.",
        "This can assist for servers that need this changed to a different world",
        "due to setup reasoning, like needing to send the players to the spawn world",
        "or the wilderness world, etc.",
        "This needs a NamedspacedKey string pattern, like 'namespace:key' that points",
        "to the dimension you want to use. The default is 'minecraft:overworld'",
        "",
        "This also applies to the end portal and nether portal, in replacement of the overworld",
        "For example, if you set this to 'minecraft:the_nether', all entities entering the",
        "end portal from the end will respawn in the nether rather than the overworld"
    })
    @NamespacedKey
    public String defaultRespawnDimensionKey = "minecraft:overworld";

    public ResourceKey<Level> fetchRespawnDimensionKey() {
        return ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(this.defaultRespawnDimensionKey));
    }

    public Containers containers = new Containers();
    public static class Containers {
        @Comment("The amount of rows for the barrel block")
        @Range(from = 1L, to = 6L)
        public int barrelRows = 3;
        @Comment("Whether to use 6 rows for the player ender chest, rather than the normal 3")
        public boolean enderChestSixRows = false;
        @Comment({
            "Whether to use a permission based system for defining the size of ender chests per player",
            "Valid permissions:",
            " - purpur.enderchest.rows.six",
            " - purpur.enderchest.rows.five",
            " - purpur.enderchest.rows.four",
            " - purpur.enderchest.rows.three",
            " - purpur.enderchest.rows.two",
            " - purpur.enderchest.rows.one"
        })
        public boolean enderChestPermissionRows = false;
    }

    @Comment("Disables leaf decaying")
    public boolean disableLeafDecay = false;

    public Mace mace = new Mace();
    public static class Mace {
        @Comment("Removes the fall distance amplifier with maces")
        public boolean ignoreFallDistance = false;
        @Comment("The limit before fall distance scaling stops working for mace damage bonuses")
        public double fallDistanceLimit = -1.0D;
    }

    @Comment("Makes item entities immune to explosion damage sources")
    public boolean itemEntitiesImmuneToExplosions = false;

    @Comment("Makes item entities immune to lightning damage sources")
    public boolean itemEntitiesImmuneToLightning = false;

    @Comment({
        "Defines a percentage of which the server will apply to the velocity applied to",
        "item entities dropped on death. 0 means it has no velocity, 1 is default."
    })
    public double itemEntitySpreadFactor = 1.0D;

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
            .validator(ConfigHandlers.NamespacedKeyProcessor::new)
            .runtimeModifier("debug.*", new RuntimeModifier<>(boolean.class, (original) -> RUNNING_IN_IDE || original))
            .post(context -> {
                INSTANCE = context.configuration();
                // build and print config tree.
                YamlTextFormatter formatter = new YamlTextFormatter(4);
                LOGGER.info(Component.text("Printing configuration tree:").appendNewline().append(formatter.apply(context.contents())));
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
        long startNanos = System.nanoTime();
        ConfigurationManager.register(Config.class, Config::buildSerializer);
        LOGGER.info("Finished Canvas config init in {}ms", TimeUnit.MILLISECONDS.convert(Util.getNanos() - startNanos, TimeUnit.NANOSECONDS));
        // init parallel search radius iteration early
        //noinspection ResultOfMethodCallIgnored
        ParallelSearchRadiusIteration.getSearchIteration(MoonriseConstants.MAX_VIEW_DISTANCE);
        return INSTANCE;
    }
}
