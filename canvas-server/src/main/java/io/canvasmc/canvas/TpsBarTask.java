package io.canvasmc.canvas;

import io.papermc.paper.threadedregions.RegionizedWorldData;
import io.papermc.paper.threadedregions.TickData;
import io.papermc.paper.threadedregions.commands.CommandUtil;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import java.text.DecimalFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class TpsBarTask {
    private static final ThreadLocal<DecimalFormat> TWO_DECIMAL_PLACES = ThreadLocal.withInitial(() -> {
        return new DecimalFormat("#,##0.00");
    });
    private static final ThreadLocal<DecimalFormat> ONE_DECIMAL_PLACES = ThreadLocal.withInitial(() -> {
        return new DecimalFormat("#,##0.0");
    });
    private final RegionizedWorldData worldData;
    private final BossBar bossBar;
    public final List<ServerPlayer> players = new CopyOnWriteArrayList<>() {
        @Override
        public boolean add(final @NotNull ServerPlayer serverPlayer) {
            TpsBarTask.this.bossBar.addViewer(serverPlayer.getBukkitEntity());
            return super.add(serverPlayer);
        }

        @Override
        public boolean remove(final Object o) {
            if (o instanceof ServerPlayer serverPlayer) {
                TpsBarTask.this.bossBar.removeViewer(serverPlayer.getBukkitEntity());
            }
            return super.remove(o);
        }
    };

    public TpsBarTask(RegionizedWorldData worldData) {
        this.worldData = worldData;
        this.bossBar = BossBar.bossBar(
            Component.text("Waiting for region to update ticking state..."), 0.0F, BossBar.Color.BLUE, BossBar.Overlay.NOTCHED_12
        );
    }

    public void tick() {
        if (this.worldData.getRedstoneGameTime() % 20 != 0) {
            return;
        }
        // run this every second
        // update tps/mspt
        TickData.TickReportData report5s = this.worldData.regionData.getRegionSchedulingHandle().getTickReport5s(System.nanoTime());
        final double util = report5s.utilisation();
        final double tps = report5s.tpsData().segmentAll().average();
        final double mspt = report5s.timePerTickData().segmentAll().average() / 1.0E6;
        this.bossBar.progress(
            Math.max(Math.min((float) mspt / 50.0F, 1.0F), 0.0F)
        );
        this.bossBar.name(MiniMessage.miniMessage().deserialize(
            "Region TPS<yellow>:</yellow> <tps> Region MSPT<yellow>:</yellow> <mspt> Util<yellow>:</yellow> <utilization>%",
            Placeholder.component("tps", getTPSColor(tps)),
            Placeholder.component("mspt", getMSPTColor(mspt)),
            Placeholder.component("utilization", getUtilizationColor(util))
        ));
    }

    private @NotNull Component getUtilizationColor(double utilization) {
        return Component.text(ONE_DECIMAL_PLACES.get().format(utilization), CommandUtil.getUtilisationColourRegion(utilization));
    }

    private @NotNull Component getMSPTColor(double mspt) {
        return Component.text(TWO_DECIMAL_PLACES.get().format(mspt), CommandUtil.getColourForMSPT(mspt));
    }

    private @NotNull Component getTPSColor(double tps) {
        return Component.text(TWO_DECIMAL_PLACES.get().format(tps), CommandUtil.getColourForTPS(tps));
    }

    public void addIfContained(ServerPlayer player) {
        if (player.tpsbar) {
            this.players.add(player);
        }
    }
}
