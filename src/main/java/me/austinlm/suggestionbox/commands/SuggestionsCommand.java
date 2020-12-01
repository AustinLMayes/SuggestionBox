package me.austinlm.suggestionbox.commands;

import com.google.common.collect.Lists;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import me.austinlm.suggestionbox.CategoryRegistry;
import me.austinlm.suggestionbox.SuggestionCategory;
import me.austinlm.suggestionbox.db.SuggestionService;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ocpsoft.pretty.time.PrettyTime;

public class SuggestionsCommand extends Command implements SBCommand {

  private static final PrettyTime PRETTY_TIME = new PrettyTime();

  private static final BaseComponent LINE = new TextComponent(
      new ComponentBuilder("         ").strikethrough(true).color(
          ChatColor.AQUA).create());
  private final CategoryRegistry registry;
  private final SuggestionService service;

  public SuggestionsCommand(CategoryRegistry registry, SuggestionService service) {
    super("suggestions", "Get a list of suggestions for a specific player/category",
        "/suggestions <category|*> <player name|uuid>",
        Collections.singletonList("suggs"));
    this.registry = registry;
    this.service = service;
  }

  @Override
  public boolean execute(CommandSender sender, String command, String[] args) {
    if (!checkMinArgs(sender, args, 2)) {
      return false;
    }

    String categoryName = args[0];
    SuggestionCategory category = null;
    if (!categoryName.equals("*")) {
      category = registry.findCategory(categoryName);
      if (category == null) {
        sendError(sender, "Unknown category: " + categoryName);
        return false;
      }
    }

    UUID who = null;
    String playerOrUUID = args[1];
    if (playerOrUUID.contains("-")) {
      try {
        who = UUID.fromString(playerOrUUID);
      } catch (IllegalArgumentException e) {
        sendError(sender, "Invalid UUID format!");
        return false;
      }
    } else {
      Player found = Bukkit.getPlayer(playerOrUUID);
      if (found == null) {
        sendError(sender, "Player not found!");
        return false;
      }
      who = found.getUniqueId();
    }
    this.service.getSuggestions(who, category, (r) -> {
      try {
        List<BaseComponent[]> rows = Lists.newArrayList();
        int i = 1;
        while (r.next()) {
          String suggestion = r.getString(1);
          Date when = r.getTimestamp(2);
          rows.add(new ComponentBuilder()
              .append(i + ".").color(ChatColor.DARK_AQUA)
              .append(" ")
              .append(PRETTY_TIME.format(when)).color(ChatColor.YELLOW)
              .append(" - ").color(ChatColor.GREEN)
              .append(suggestion).color(ChatColor.GRAY)
              .create());
          i++;
        }
        if (rows.isEmpty()) {
          sendError(sender, "No suggestions matched the search");
        } else {
          sender.sendMessage(new ComponentBuilder()
              .append(LINE)
              .append(" Suggestions from " + playerOrUUID + " ").color(ChatColor.GOLD)
              .append(LINE)
              .create());
          for (BaseComponent[] row : rows) {
            sender.sendMessage(row);
          }
        }
      } catch (SQLException e) {
        sendError(sender,
            "Unable to retrieve suggestion from DB! See console for more information.");
        e.printStackTrace();
      }
    }, e -> {
      sendError(sender, "Unable to retrieve suggestion from DB! See console for more information.");
      e.printStackTrace();
    });

    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String alias, String[] args)
      throws IllegalArgumentException {
    if (args.length == 1) {
      String search = args[0].toLowerCase();
      List<String> res = Lists.newArrayList(this.registry.getCategoryNames());
      res.add("*");
      res.removeIf(s -> !s.toLowerCase().startsWith(search));
      return res;
    }

    return super.tabComplete(sender, alias, args);
  }
}
