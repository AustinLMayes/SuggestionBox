package me.austinlm.suggestionbox.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.annotation.Nullable;
import org.bukkit.util.Consumer;

/**
 * Async database wrapper
 *
 * @author Austin Mayes
 */
public class Database {

  private final String host;
  private final int port;
  private final String database;
  private final String username;
  private final @Nullable
  String password;
  private final ExecutorService executorService;
  private Connection connection;

  public Database(String host, int port, String database, String username,
      @Nullable String password, int thrads) {
    this.host = host;
    this.port = port;
    this.database = database;
    this.username = username;
    this.password = password;
    this.executorService = Executors.newFixedThreadPool(thrads);
  }

  /**
   * @throws ClassNotFoundException if the driver is not loaded
   * @throws SQLException if the connection fails
   */
  public void connect() throws ClassNotFoundException, SQLException {
    if (connection != null) {
      throw new IllegalStateException("Database is already connected!");
    }

    Class.forName("com.mysql.jdbc.Driver");
    connection = DriverManager.getConnection("jdbc:mysql://"
            + this.host + ":" + this.port + "/" + this.database,
        this.username, this.password);
  }

  public void close() throws SQLException {
    if (this.connection == null) {
      return;
    }

    connection.close();

    this.connection = null;
  }

  private void ensureConnected() throws SQLException {
    if (connection == null || connection.isClosed()) {
      throw new IllegalStateException("Cannot execute SQL queries without an open connection.");
    }
  }

  /**
   * @param statement to prepare
   * @return a prepared statement from the provided SQL
   * @throws SQLException if the sql contains syntax errors
   */
  public PreparedStatement prepare(String statement) throws SQLException {
    return connection.prepareStatement(statement);
  }

  /**
   * @param statement containing the query
   * @param successCallback to execute if the query is successful
   * @param failCallback to execute if the query fails for any reason
   */
  public void executeQuery(final PreparedStatement statement, Consumer<ResultSet> successCallback,
      Consumer<SQLException> failCallback) {
    try {
      ensureConnected();
    } catch (SQLException e) {
      failCallback.accept(e);
    }

    this.executorService.submit(() -> {
      try {
        ResultSet res = statement.executeQuery();
        successCallback.accept(res);
      } catch (SQLException e) {
        failCallback.accept(e);
      }
    });
  }

  /**
   * @param statement to execute
   * @param successCallback to execute if the execution is successful
   * @param failCallback to execute if the execution fails for any reason
   */
  public void execute(final PreparedStatement statement, Consumer<Boolean> successCallback,
      Consumer<SQLException> failCallback) {
    try {
      ensureConnected();
    } catch (SQLException e) {
      failCallback.accept(e);
    }

    this.executorService.submit(() -> {
      try {
        boolean res = statement.execute();
        successCallback.accept(res);
      } catch (SQLException e) {
        failCallback.accept(e);
      }
    });
  }
}
