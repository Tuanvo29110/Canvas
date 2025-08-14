package io.canvasmc.canvas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.papermc.paper.threadedregions.RegionizedWorldData;
import io.papermc.paper.threadedregions.TickData;
import io.papermc.paper.threadedregions.commands.CommandUtil;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static net.kyori.adventure.text.Component.text;

public class RegionizedTpsBar {
    private static final ThreadLocal<DecimalFormat> TWO_DECIMAL_PLACES = ThreadLocal.withInitial(() -> {
        return new DecimalFormat("#,##0.00");
    });
    private static final ThreadLocal<DecimalFormat> ONE_DECIMAL_PLACES = ThreadLocal.withInitial(() -> {
        return new DecimalFormat("#,##0.0");
    });
    private final RegionizedWorldData worldData;
    private final boolean canTick;
    private long nextTick = System.nanoTime();

    public RegionizedTpsBar(RegionizedWorldData worldData) {
        this.worldData = worldData;
        this.canTick = Config.INSTANCE.enableTpsBar;
    }

    public static @NotNull Component gradient(final String textContent, final @Nullable Consumer<Style.Builder> style, final TextColor... colors) {
        final Gradient gradient = new Gradient(colors);
        final TextComponent.Builder builder = text();
        if (style != null) {
            builder.style(style);
        }
        final char[] content = textContent.toCharArray();
        gradient.length(content.length);
        for (final char c : content) {
            builder.append(text(c, gradient.nextColor()));
        }
        return builder.build();
    }

    public RegionizedWorldData getWorldData() {
        return worldData;
    }

    public void tick() {
        if (this.canTick && this.nextTick <= System.nanoTime()) { // use system nano time, more reliable with runtime tick rate changes
            // update tps maps
            long startTime = System.nanoTime();
            TickData.TickReportData tickReportData = this.worldData.regionData.getRegionSchedulingHandle().getTickReport5s(System.nanoTime());
            TickData.SegmentedAverage tpsAverage = tickReportData.tpsData();
            TickData.SegmentedAverage msptAverage = tickReportData.timePerTickData();
            final double util = tickReportData.utilisation() * 100;
            final double tps = tpsAverage.segmentAll().average();
            final double mspt = msptAverage.segmentAll().average() / 1.0E6;
            String tpsTruncated = TWO_DECIMAL_PLACES.get().format(tps);
            String msptTruncated = TWO_DECIMAL_PLACES.get().format(mspt);
            String utilizationTruncated = ONE_DECIMAL_PLACES.get().format(util) + "%";
            // build component
            final Component textComponent =
                gradient("TPS", (builder) -> builder.decorate(TextDecoration.BOLD), NamedTextColor.BLUE, NamedTextColor.AQUA)
                    .append(Component.text(": ", NamedTextColor.WHITE))
                    .append(Component.text(tpsTruncated, this.worldData.regionData.getRegionSchedulingHandle().ticksToSprint > 0 ? CommandUtil.SPRINTING_COLOR : CommandUtil.getColourForTPS(tps)))
                    .append(Component.text("  -  ", NamedTextColor.WHITE))
                    .append(gradient("MSPT", (builder) -> builder.decorate(TextDecoration.BOLD), NamedTextColor.BLUE, NamedTextColor.AQUA))
                    .append(Component.text(": ", NamedTextColor.WHITE))
                    .append(Component.text(msptTruncated, this.worldData.regionData.getRegionSchedulingHandle().ticksToSprint > 0 ? CommandUtil.SPRINTING_COLOR : CommandUtil.getColourForMSPT(mspt)))
                    .append(Component.text("  -  ", NamedTextColor.WHITE))
                    .append(gradient("Util", (builder) -> builder.decorate(TextDecoration.BOLD), NamedTextColor.BLUE, NamedTextColor.AQUA))
                    .append(Component.text(": ", NamedTextColor.WHITE))
                    .append(Component.text(utilizationTruncated, this.worldData.regionData.getRegionSchedulingHandle().ticksToSprint > 0 ? CommandUtil.SPRINTING_COLOR : CommandUtil.getUtilisationColourRegion(util / 100)));
            // update players
            for (final ServerPlayer localPlayer : this.worldData.getLocalPlayers()) {
                final Entry entry = localPlayer.localEntry;
                if (entry.enabled()) {
                    switch (entry.placement()) {
                        case BOSS_BAR -> {
                            localPlayer.tpsBar.name(textComponent);
                            localPlayer.tpsBar.progress(Math.min((float) mspt / 50, 1.00F)); // this is a percentage, the mspt is out of 50, so divide by 50 and the max value be 1(100%)
                        }
                        case ACTION_BAR -> localPlayer.getBukkitEntity().sendActionBar(
                            textComponent
                        );
                    }
                }
            }
            this.nextTick = startTime + 1_000_000_000;
        }
    }

    public enum Placement {
        ACTION_BAR, BOSS_BAR;
        public static final Codec<Placement> CODEC = Codec.STRING.comapFlatMap((string) -> DataResult.success(Placement.valueOf(string)), Enum::name);
    }

    private static final class Gradient {

        private final boolean negativePhase;
        private final TextColor[] colors;
        private int index = 0;
        private int colorIndex = 0;
        private float factorStep = 0;
        private float phase;

        public Gradient(final @NonNull TextColor... colors) {
            this(0, colors);
        }

        public Gradient(final float phase, final @NonNull TextColor @NotNull ... colors) {
            if (colors.length < 2) {
                throw new IllegalArgumentException("Gradients must have at least two colors! colors=" + Arrays.toString(colors));
            }
            if (phase > 1.0 || phase < -1.0) {
                throw new IllegalArgumentException(String.format("Phase must be in range [-1, 1]. '%s' is not valid.", phase));
            }
            this.colors = colors;
            if (phase < 0) {
                this.negativePhase = true;
                this.phase = 1 + phase;
                Collections.reverse(Arrays.asList(this.colors));
            } else {
                this.negativePhase = false;
                this.phase = phase;
            }
        }

        public void length(final int size) {
            this.colorIndex = 0;
            this.index = 0;
            final int sectorLength = size / (this.colors.length - 1);
            this.factorStep = 1.0f / sectorLength;
            this.phase = this.phase * sectorLength;
        }

        public @NonNull TextColor nextColor() {
            if (this.factorStep * this.index > 1) {
                this.colorIndex++;
                this.index = 0;
            }

            float factor = this.factorStep * (this.index++ + this.phase);
            // loop around if needed
            if (factor > 1) {
                factor = 1 - (factor - 1);
            }
            if (this.negativePhase && this.colors.length % 2 != 0) {
                // flip the gradient segment for to allow for looping phase -1 through 1
                return this.interpolate(this.colors[this.colorIndex + 1], this.colors[this.colorIndex], factor);
            } else {
                return this.interpolate(this.colors[this.colorIndex], this.colors[this.colorIndex + 1], factor);
            }
        }

        private @NonNull TextColor interpolate(final @NonNull TextColor color1, final @NonNull TextColor color2, final float factor) {
            return TextColor.color(
                Math.round(color1.red() + factor * (color2.red() - color1.red())),
                Math.round(color1.green() + factor * (color2.green() - color1.green())),
                Math.round(color1.blue() + factor * (color2.blue() - color1.blue()))
            );
        }
    }

    public record Entry(boolean enabled, Placement placement) {
        public static final Entry FALLBACK = new Entry(false, Placement.BOSS_BAR);
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
            instance -> instance.group(
                    Codec.BOOL.optionalFieldOf("enabled", false).forGetter(Entry::enabled),
                    Placement.CODEC.optionalFieldOf("placement", Placement.BOSS_BAR).forGetter(Entry::placement)
                )
                .apply(instance, Entry::new)
        );

        @Override
        public boolean enabled() {
            return Config.INSTANCE.enableTpsBar && enabled;
        }
    }
}
