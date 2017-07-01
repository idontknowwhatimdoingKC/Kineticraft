package net.kineticraft.lostcity.mechanics;

import lombok.Getter;
import net.kineticraft.lostcity.Core;
import net.kineticraft.lostcity.config.Configs;
import net.kineticraft.lostcity.data.lists.QueueList;
import net.kineticraft.lostcity.utils.TextBuilder;
import net.kineticraft.lostcity.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Manages basic core server utilities such as backing up, rebooting, announcements, lag controller, etc.
 *
 * Created by Kneesnap on 6/28/2017.
 */
public class ServerManager extends Mechanic {

    @Getter private static int renderDistance = 10;
    @Getter private static QueueList<Double> tpsQueue = new QueueList<>();
    private static long lastPoll = System.currentTimeMillis();

    private static final int TPS_INTERVAL = 50;
    private static final int MAX_RENDER = 10;
    private static final int MIN_RENDER = 5;

    @Override
    public void onEnable() {
        // Register announcer.
        Bukkit.getScheduler().runTaskTimer(Core.getInstance(), () -> {
            TextBuilder tb = Utils.randElement(Configs.getTextConfig(Configs.ConfigType.ANNOUNCER).getComponents());
            if (tb != null)
                Bukkit.broadcast(tb.create());
        }, 0L, 5 * 20 * 60L);

        // Update render distance every minute.
        Bukkit.getScheduler().runTaskTimer(Core.getInstance(), () ->
            setRenderDistance(getRenderDistance() + (getTPS() >= 19 ? 1 : -1)), 0L, 60 * 20L);

        // Update TPS Counter
        Bukkit.getScheduler().runTaskTimer(Core.getInstance(), () -> {
            getTpsQueue().trim(25); // Only keep recent records.
            final long startTime = System.currentTimeMillis();
            getTpsQueue().add((startTime - lastPoll) / (TPS_INTERVAL * (TPS_INTERVAL / 20D)));
            lastPoll = startTime;
        }, 0L, TPS_INTERVAL);
    }

    @Override
    public void onJoin(Player player) {
        player.setViewDistance(getRenderDistance());
    }

    /**
     * Update the entire server render distance.
     * @param newDistance
     */
    public static void setRenderDistance(int newDistance) {
        newDistance = Math.max(MIN_RENDER, Math.min(newDistance, MAX_RENDER));
        if (newDistance == getRenderDistance())
            return; // Don't waste resources changing the render distance every pulse, only when it should change.

        renderDistance = newDistance;
        Bukkit.getOnlinePlayers().forEach(p -> p.setViewDistance(getRenderDistance()));
    }

    /**
     * Get the average server TPS.
     * @return tps
     */
    public static double getTPS() {
        return Math.min(20, getTpsQueue().stream().mapToDouble(Double::doubleValue).average().orElse(20D));
    }

    /**
     * Returns a number between 0 and 4 that represents the lag setting.
     * @return lagSetting
     */
    public static int getLagSetting() {
        return (int) (((MAX_RENDER - getRenderDistance()) / (double) (MAX_RENDER - MIN_RENDER)) * 4);
    }
}