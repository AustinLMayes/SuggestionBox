package me.austinlm.suggestionbox.commands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Some command helpers.
 *
 * @author Austin Mayes
 */
public interface SBCommand {

  String WARNING = "\u26A0"; // ⚠

  /**
   * Send an error message and sound to a user
   *
   * @param sender to send the error to
   * @param message describing the error
   */
  default void sendError(CommandSender sender, BaseComponent... message) {
    if (sender instanceof Player && !((Player) sender).isOnline()) {
      return;
    }

    ComponentBuilder builder = new ComponentBuilder();
    builder.append("[" + WARNING + "]").color(ChatColor.GOLD);
    builder.append(" ").append(message).color(ChatColor.RED);

    sender.sendMessage(builder.create());
    if (sender instanceof Player) {
      ((Player) sender)
          .playSound(((Player) sender).getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, .5f);
    }
  }

  default void sendError(CommandSender sender, String message) {
    sendError(sender, new TextComponent(message));
  }

  /**
   * Ensure the user supplied at least this many arguments
   *
   * @param sender executing the command
   * @param args to check
   * @param minArgs required
   * @return if the user submitted enough arguments
   */
  default boolean checkMinArgs(CommandSender sender, String[] args, int minArgs) {
    if (args.length < minArgs) {
      sendError(sender,
          "Please provide at least " + minArgs + " argument" + (minArgs > 1 ? "s" : "") + "!");
      return false;
    }
    return true;
  }

  /**
   * @param args to join
   * @param startIndex where in the args to begin joining from
   * @return a joined string delimited bu spaces
   */
  default String joinRemaining(String[] args, int startIndex) {
    StringBuilder res = new StringBuilder();
    for (int i = startIndex; i < args.length; i++) {
      res.append(" ").append(args[i]);
    }
    return res.toString();
  }

  /**
   * Ensure a {@link Player} is executing a command
   *
   * @param sender to check
   * @return if a player is executing the command
   */
  default boolean ensurePlayer(CommandSender sender) {
    if (!(sender instanceof Player)) {
      sendError(sender, "You must be a player to do this!");
      return false;
    }

    return true;
  }
}
