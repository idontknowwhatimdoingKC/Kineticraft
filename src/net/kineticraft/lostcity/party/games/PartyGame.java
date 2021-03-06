package net.kineticraft.lostcity.party.games;

import com.destroystokyo.paper.Title;
import lombok.Getter;
import net.kineticraft.lostcity.Core;
import net.kineticraft.lostcity.party.Arena;
import net.kineticraft.lostcity.party.Parties;
import net.kineticraft.lostcity.utils.Utils;
import net.kineticraft.lostcity.utils.tasks.TaskList;
import org.bukkit.*;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * A party game base.
 * Game types: single player, free join (boost), minimum count.
 * Created by Kneesnap on 9/14/2017.
 */
@Getter
public class PartyGame implements Listener {
    private Set<Player> players = new HashSet<>();
    private boolean going;
    private Set<Location> spawnLocations = new HashSet<>();
    private Location exitLocation;
    private TaskList scheduler = new TaskList();
    private Arena arena;

    public PartyGame() {
        Bukkit.getPluginManager().registerEvents(this, Core.getInstance()); // Register this.
    }

    /**
     * Called when this game ends.
     */
    protected void onStop() {

    }

    /**
     * Called when this game is started.
     */
    protected void onStart() {

    }

    /**
     * Called when a player joins this game.
     * @param player
     */
    protected void onJoin(Player player) {

    }

    /**
     * Called when a player leaves this game.
     * @param player
     */
    protected void onLeave(Player player) {

    }

    /**
     * Can a player be added to this game?
     * @param player
     * @return canAdd
     */
    protected boolean canAdd(Player player) {
        return true;
    }

    /**
     * Called when a party sign action is performed.
     * @param action The sign action being performed.
     * @param player The player who clicked on the sign.
     * @param sign The sign being clicked on.
     */
    public void signAction(String action, Player player, Sign sign) {

    }

    /**
     * Start this game.
     */
    public void start() {
        if (isGoing())
            return;
        getPlayers().forEach(this::spawnPlayer);
        onStart();
        going = true;
    }

    /**
     * Stop this game.
     */
    public void stop() {
        if (!isGoing())
            return;
        going = false;
        getScheduler().cancelAll();
        onStop();
        new ArrayList<>(getPlayers()).forEach(this::removePlayer); // Remove all players.
        getPlayers().clear();
    }

    /**
     * Add a player to this game.
     * @param player
     */
    public void addPlayer(Player player) {
        if (isPlaying(player)) {
            player.sendMessage(ChatColor.RED + "You are already playing this game.");
            return;
        }

        if (!canAdd(player))
            return;

        getPlayers().add(player);
        player.setGameMode(GameMode.SURVIVAL);
        onJoin(player);
    }

    /**
     * Remove a player from this game.
     * @param player
     * @return removed
     */
    public boolean removePlayer(Player player) {
        if (!getPlayers().remove(player))
            return false;

        Location exit = getExitLocation();
        if (exit != null) {
            player.teleport(exit);
            Bukkit.getScheduler().runTask(Core.getInstance(), () -> player.teleport(exit));
        }
        Utils.stopNBS(player);

        if (isGoing()) { // If the game isn't over
            broadcastPlayers(player.getName() + " has left");
            onLeave(player);
            if (getPlayers().isEmpty())
                stop(); // There aren't any players left, automatically stop game.
        }

        return true;
    }

    /**
     * Run a countdown timer.
     * @param onFinish
     * @param duration - The length of the countdown, in seconds.
     */
    protected void countdown(Runnable onFinish, int duration) {
        for (int i = 0; i < duration; i++) {
            final int sec = (duration - i);
            getScheduler().runTaskLater(() -> getPlayers().forEach(p -> p.sendTitle(new Title(ChatColor.YELLOW.toString() + sec + "..."))), i * 20);
        }

        if (onFinish != null)
            getScheduler().runTaskLater(onFinish, duration * 20);
    }

    /**
     * Play a NBS file for everyone in this game.
     * @param track
     * @param repeat
     */
    protected void playMusic(String track, boolean repeat) {
        getPlayers().forEach(p -> playMusic(p, track, repeat)); // We don't play this all under the same player in-case a player leaves or something.
    }

    /**
     * Play a NBS file to a given player.
     * @param player
     * @param track
     * @param repeat
     */
    protected void playMusic(Player player, String track, boolean repeat) {
        Utils.playNBS(Arrays.asList(player), track, repeat);
    }

    /**
     * Get the party-world.
     * @return partyWorld
     */
    protected World getWorld() {
        return Parties.getPartyWorld();
    }

    /**
     * Is a given player currently taking part of this minigame?
     * @param player
     * @return isPlaying
     */
    public boolean isPlaying(Player player) {
        return getPlayers().contains(player);
    }

    /**
     * Get this game's name.
     * @return gameName
     */
    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Get the prefix that shows for this game for messaging.
     * @return prefix
     */
    protected String getPrefix() {
        return "[" + getName() + "]";
    }

    /**
     * Format a game-message for chat.
     * @param message
     * @return formatted
     */
    protected String format(String message) {
        return ChatColor.YELLOW + getPrefix() + " " + ChatColor.BLUE + message;
    }

    /**
     * Broadcast a party message.
     * @param message
     */
    protected void broadcast(String message) {
        broadcast(message, true);
    }

    /**
     * Broadcast a party message to all players in the game.
     * @param message
     */
    protected void broadcastPlayers(String message) {
        broadcast(message, false);
    }

    /**
     * Broadcast a party message.
     * @param message - The message to send.
     * @param serverWide - Should this message get sent to everyone, or just people playing this game?
     */
    private void broadcast(String message, boolean serverWide) {
        String fullMsg = format(message);
        if (serverWide) {
            Bukkit.broadcastMessage(fullMsg);
        } else {
            getPlayers().forEach(p -> p.sendMessage(fullMsg));
        }
    }

    /**
     * Gets a random spawn location for the player.
     * @return randSpawn
     */
    protected Location randomSpawn() {
        return Utils.randElement(getSpawnLocations());
    }

    /**
     * Spawns a player into the game at a random location.
     * @param player
     */
    protected void spawnPlayer(Player player) {
        Location loc = randomSpawn();
        if (loc == null)
            return;
        loc.setWorld(Parties.getPartyWorld());
        player.teleport(loc);
    }

    /**
     * Add a spawn location.
     * @param x
     * @param y
     * @param z
     * @param yaw
     * @param pitch
     */
    protected void addSpawnLocation(double x, double y, double z, float yaw, float pitch) {
        getSpawnLocations().add(new Location(null, x, y, z, yaw, pitch));
    }

    /**
     * Set the exit location for this game.
     */
    protected void setExit(double x, double y, double z, float yaw, float pitch) {
        exitLocation = new Location(Parties.getPartyWorld(), x, y, z, yaw, pitch);
    }

    /**
     * Set the arena bounds for this game.
     */
    protected void setArena(int xMin, int xMax, int zMin, int zMax) {
        setArena(xMin, xMax, zMin, zMax, 0, 256);
    }

    /**
     * Set the arena bounds for this game.
     */
    protected void setArena(int xMin, int xMax, int zMin, int zMax, int yMin, int yMax) {
        arena = new Arena(xMin, xMax, zMin, zMax, yMin, yMax, getWorld());
    }
}
