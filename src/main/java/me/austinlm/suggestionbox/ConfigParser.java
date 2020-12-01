package me.austinlm.suggestionbox;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;
import me.austinlm.suggestionbox.db.Database;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Class responsible for translating configuration values to usable information
 *
 * @author Austin Mayes
 */
public class ConfigParser {

  private final Configuration configuration;
  private final BiConsumer<String, Boolean> failCallback;

  public ConfigParser(Configuration configuration,
      BiConsumer<String, Boolean> failCallback) {
    this.configuration = configuration;
    this.failCallback = failCallback;
  }

  /**
   * Notify the user of a configuration error.
   *
   * @param section which the issue exists inside of
   * @param subPath to the value containing the error
   * @param error message to display to the user
   * @param fatal if this error should cause the plugin to not load
   */
  private void configFailed(ConfigurationSection section, @Nullable String subPath, String error,
      boolean fatal) {
    String message = "CONFIGURATION ERROR ";
    if (section != null) {
      message += "@ " + section.getCurrentPath() + (subPath == null ? "" : "." + subPath) + " - ";
    }
    message += error;
    this.failCallback.accept(message, fatal);
  }

  /**
   * Ensure a value is present
   *
   * @param clazz of the value type
   * @param section containing the value
   * @param name of the value, downcased and dashed for section retrieval
   * @param fatal if the absence of a value should cause the plugin to fail to load
   * @param <V> type of value being checked
   * @return the value, if it is present
   */
  private <V> V ensurePresent(Class<V> clazz, ConfigurationSection section, String name,
      boolean fatal) {
    String slug = name.toLowerCase().replaceAll(" ", "-");
    Object raw = section.get(slug);
    if (raw == null || (raw instanceof String && ((String) raw).isEmpty())) {
      configFailed(section, slug, name + " cannot be empty.", fatal);
      return null;
    }

    if (!clazz.isAssignableFrom(raw.getClass())) {
      configFailed(section, slug,
          name + " is a " + raw.getClass().getSimpleName() + "and shot be a " + clazz
              .getSimpleName(), fatal);
      return null;
    }

    return (V) section.get(slug);
  }

  Database constructDatabase() {
    ConfigurationSection section = this.configuration.getConfigurationSection("database");
    if (section == null) {
      configFailed(this.configuration, "database", "Missing database config section", true);
      return null;
    }
    String host = ensurePresent(String.class, section, "Host", true);
    if (host == null) {
      return null;
    }

    String username = ensurePresent(String.class, section, "Username", true);
    if (username == null) {
      return null;
    }

    String database = ensurePresent(String.class, section, "Database", true);
    if (database == null) {
      return null;
    }

    Integer port = ensurePresent(Integer.class, section, "Port", true);
    if (port == null) {
      return null;
    }

    if (port < 1 || port > 65535) {
      configFailed(section, "port", "Port must be in the range of 1-65535", true);
      return null;
    }

    Integer threads = ensurePresent(Integer.class, section, "Thread Pool Size", true);
    if (threads == null) {
      return null;
    }

    if (threads < 1) {
      configFailed(section, "threads", "Thread count must be at least 1", true);
      return null;
    }

    String password = section.getString("password");

    return new Database(host, port, database, username, password, threads);
  }

  CategoryRegistry parseCategories() {
    ConfigurationSection categorySection = this.configuration
        .getConfigurationSection("suggestion-categories");
    if (categorySection == null) {
      configFailed(this.configuration, "suggestion-categories",
          "Missing suggestion categories config section", true);
      return null;
    }

    List<SuggestionCategory> categories = Lists.newArrayList();

    for (Map.Entry<String, Object> entry : categorySection.getValues(false).entrySet()) {
      ConfigurationSection section = (ConfigurationSection) entry.getValue();
      String name = ensurePresent(String.class, section, "Name", false);
      String description = ensurePresent(String.class, section, "Description", false);
      if (name == null || description == null) {
        continue;
      }

      name = name.replaceAll(" ", "-");
      categories.add(new SuggestionCategory(entry.getKey(), name, description));
    }

    return new CategoryRegistry(categories);
  }
}
