package net.kineticraft.lostcity.commands;

import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

/**
 * Command - A KC command base.
 *
 * Created by Kneesnap on 5/29/2017.
 */
@Getter
public abstract class Command {

    private CommandType type;
    private String rawUsage;
    private String help;
    private List<String> alias;

    public Command(CommandType type, String usage, String help, String... alias) {
        this.type = type;
        this.rawUsage = usage;
        this.help = help;
        this.alias = Arrays.asList(alias);
    }

    /**
     * Gets the command usage.
     * @return
     */
    public String getUsage() {
        return getUsage(getName());
    }

    /**
     * Gets the command usage with a specified alias.
     * @return
     */
    public String getUsage(String alias) {
        return ChatColor.RED + "Usage: " + getType().getPrefix() + alias + " " + getRawUsage();
    }

    /**
     * Get the minimum amount of required arguments for this command.
     * @return
     */
    public int getMinArgs() {
        return (int) Arrays.stream(getRawUsage().split(" ")).filter(s -> s.startsWith("<") && s.endsWith(">")).count();
    }

    /**
     * Returns the command display name.
     */
    public String getName() {
        return getAlias().get(0);
    }

    /**
     * Returns the string prefix that preceeds this command.
     * @return
     */
    public String getCommandPrefix() {
        return getType().getPrefix();
    }

    /**
     * Show default usage to the player
     * @param sender
     */
    protected void showUsage(CommandSender sender) {
        showUsage(sender, getName());
    }

    /**
     * Send the proper usage message for the given label to the command sender.
     * @param sender
     * @param label
     */
    protected void showUsage(CommandSender sender, String label) {
        sender.sendMessage(getUsage(label));
    }

    /**
     * Handles this command logic.
     * @param sender
     * @param label
     * @param args
     */
    public void handle(CommandSender sender, String label, String[] args) {
        if (args.length < getMinArgs()) {
            showUsage(sender, label);
            return;
        }

        try {
            onCommand(sender, args);
        } catch (NumberFormatException nfe) {
            String input = nfe.getLocalizedMessage().split(": ")[1];
            sender.sendMessage(ChatColor.RED + "Invalid number " + input + ".");
        }
    }

    /**
     * The code specific to each command.
     * @param sender
     * @param args
     */
    protected abstract void onCommand(CommandSender sender,  String[] args);
}
