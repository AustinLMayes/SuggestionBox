package me.austinlm.suggestionbox.db;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.UUID;
import javax.annotation.Nullable;
import me.austinlm.suggestionbox.SuggestionCategory;
import org.bukkit.util.Consumer;

/**
 * Service for interacting with the suggestions table.
 *
 * CREATE TABLE suggestions (id INT AUTO_INCREMENT PRIMARY KEY, suggester VARCHAR(36), category
 * VARCHAR(30), suggestion VARCHAR(200), time TIMESTAMP);
 *
 * @author Austin Mayes
 */
public class SuggestionService {

  private static final String INSERT_SQL = "INSERT INTO suggestions (suggester, category, suggestion, time) VALUES (?, ?, ?, ?)";
  private static final String QUERY_ALL_SQL = "SELECT suggestion, time FROM suggestions WHERE suggester = (?)";
  private static final String QUERY_CAT = " AND category = (?)";
  private final Database database;

  public SuggestionService(Database database) {
    this.database = database;
  }

  /**
   * Submit a suggestion
   *
   * @param who is submitting the suggestion
   * @param category the suggestion is being submitted to
   * @param suggestion to submit
   * @param successCallback to execute if submission was successful
   * @param failCallback to execute if submission fails
   */
  public void submit(UUID who, SuggestionCategory category, String suggestion,
      Consumer<Boolean> successCallback,
      Consumer<SQLException> failCallback) {
    try {
      PreparedStatement insert = database.prepare(INSERT_SQL);
      insert.setString(1, who.toString());
      insert.setString(2, category.getSlug());
      insert.setString(3, suggestion);
      insert.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
      database.execute(insert, successCallback, failCallback);
    } catch (SQLException e) {
      failCallback.accept(e);
    }
  }

  /**
   * Get a list of suggestion based on search terms
   *
   * @param who to find suggestions for
   * @param category to look inside of, or {@code null} to look inside all categories
   * @param successCallback to execute if the query succeeds
   * @param failCallback to execute if the query fails
   */
  public void getSuggestions(UUID who, @Nullable SuggestionCategory category,
      Consumer<ResultSet> successCallback,
      Consumer<SQLException> failCallback) {
    String querySql = QUERY_ALL_SQL;
    if (category != null) {
      querySql += QUERY_CAT;
    }

    try {
      PreparedStatement query = database.prepare(querySql);
      query.setString(1, who.toString());
      if (category != null) {
        query.setString(2, category.getSlug());
      }
      database.executeQuery(query, successCallback, failCallback);
    } catch (SQLException e) {
      failCallback.accept(e);
    }
  }
}
