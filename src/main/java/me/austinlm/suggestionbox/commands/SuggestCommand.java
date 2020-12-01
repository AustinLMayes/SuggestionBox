package me.austinlm.suggestionbox.commands;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import me.austinlm.suggestionbox.CategoryRegistry;
import me.austinlm.suggestionbox.SuggestionCategory;
import me.austinlm.suggestionbox.db.SuggestionService;
import me.austinlm.suggestionbox.listeners.SuggestionSubmissionListener;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SuggestCommand extends Command implements SBCommand {

  private final CategoryRegistry registry;
  private final SuggestionService service;

  public SuggestCommand(CategoryRegistry registry, SuggestionService service) {
    super("suggest", "Suggest something", "/suggest <category> <suggestion>",
        Collections.singletonList("sug"));
    this.registry = registry;
    this.service = service;
  }

  @Override
  public boolean execute(CommandSender sender, String command, String[] args) {
    if (!ensurePlayer(sender)) {
      return false;
    }

    Player player = (Player) sender;
    if (!checkMinArgs(sender, args, 1)) {
      return false;
    }

    String categoryName = args[0];
    SuggestionCategory category = registry.findCategory(categoryName);
    if (category == null) {
      sendError(player, "Unknown category: " + categoryName);
      return false;
    }

    if (args.length > 1) {
      this.service.submit(player.getUniqueId(), category, joinRemaining(args, 1), (b) -> {
        if (player.isOnline()) {
          player.sendMessage(SuggestionSubmissionListener.SUCCESS);
        }

      }, e -> {
        if (player.isOnline()) {
          sendError(player, SuggestionSubmissionListener.ERROR);
        }
        e.printStackTrace();
      });
    } else {
      SuggestionSubmissionListener.openBook(player, category);
      return true;
    }

    return true;
  }

  @Override
  public List<String> tabComplete(CommandSender sender, String alias, String[] args)
      throws IllegalArgumentException {
    if (args.length == 1) {
      String search = args[0].toLowerCase();
      List<String> res = Lists.newArrayList(this.registry.getCategoryNames());
      res.removeIf(s -> !s.toLowerCase().startsWith(search));
      return res;
    }

    return super.tabComplete(sender, alias, args);
  }
}
