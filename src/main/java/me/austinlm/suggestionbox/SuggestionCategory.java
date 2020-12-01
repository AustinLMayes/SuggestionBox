package me.austinlm.suggestionbox;

/**
 * Category which suggestions can be submitted to
 *
 * @author Austin Mayes
 */
public class SuggestionCategory {

  private final String slug;
  private final String name;
  private final String description;

  public SuggestionCategory(String slug, String name, String description) {
    this.slug = slug;
    this.name = name;
    this.description = description;
  }

  String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }
}
