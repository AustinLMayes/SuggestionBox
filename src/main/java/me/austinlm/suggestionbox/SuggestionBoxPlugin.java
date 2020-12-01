package me.austinlm.suggestionbox;

import java.sql.SQLException;
import me.austinlm.suggestionbox.commands.SuggestCommand;
import me.austinlm.suggestionbox.commands.SuggestionsCommand;
import me.austinlm.suggestionbox.db.Database;
import me.austinlm.suggestionbox.db.SuggestionService;
import me.austinlm.suggestionbox.listeners.SuggestionSubmissionListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class
 *
 * @author Austin Mayes
 */
public class SuggestionBoxPlugin extends JavaPlugin {

  public static SuggestionBoxPlugin INSTANCE = null;

  private ConfigParser configParser;
  private Database database;
  private CategoryRegistry registry;
  private SuggestionService service;

  @Override
  public void onLoad() {
    INSTANCE = this;
    saveDefaultConfig();
    reloadConfig();
    this.configParser = new ConfigParser(getConfig(), (m, f) -> {
      if (f) {
        m += " Shutting down...";
        Bukkit.shutdown();
      }
      Bukkit.getLogger().severe(m);
    });
    this.database = this.configParser.constructDatabase();
  }

  @Override
  public void onEnable() {
    if (database == null) {
      return;
    }

    try {
      Bukkit.getLogger().info("Connecting to database...");
      database.connect();
      Bukkit.getLogger().info("Successfully connected to database");
    } catch (ClassNotFoundException | SQLException e) {
      Bukkit.getLogger().severe("Failed to connect to database! " + e.getMessage());
      e.printStackTrace();
      Bukkit.shutdown();
      return;
    }

    this.registry = configParser.parseCategories();
    this.service = new SuggestionService(this.database);
    registerCommands();
    getServer().getPluginManager()
        .registerEvents(new SuggestionSubmissionListener(this.registry, this.service), this);
  }

  @Override
  public void onDisable() {
    if (database != null) {
      try {
        database.close();
        Bukkit.getLogger().info("Disconnected from database");
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }

    INSTANCE = null;
  }

  private void registerCommands() {
    Bukkit.getCommandMap()
        .register("suggestion-box", new SuggestCommand(this.registry, this.service));
    Bukkit.getCommandMap()
        .register("suggestion-box", new SuggestionsCommand(this.registry, this.service));

  }
}
