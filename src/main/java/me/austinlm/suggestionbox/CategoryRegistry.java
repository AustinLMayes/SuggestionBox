package me.austinlm.suggestionbox;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Class which keeps track of configured {@link SuggestionCategory categories} and adds some useful
 * search functionality.
 *
 * @author Austin Mayes
 */
public class CategoryRegistry {

  private final Map<String, SuggestionCategory> categories;

  CategoryRegistry(List<SuggestionCategory> categories) {
    this.categories = Maps.newHashMap();
    for (SuggestionCategory category : categories) {
      this.categories.put(category.getName(), category);
    }
  }

  public SuggestionCategory get(String slug) {
    for (SuggestionCategory category : this.categories.values()) {
      if (category.getSlug().equals(slug)) {
        return category;
      }
    }

    return null;
  }

  public SuggestionCategory findCategory(String search) {
    for (Entry<String, SuggestionCategory> categoryEntry : this.categories
        .entrySet()) {
      if (categoryEntry.getKey().toLowerCase().startsWith(search.toLowerCase())) {
        return categoryEntry.getValue();
      }
    }

    return null;
  }

  public Set<String> getCategoryNames() {
    return this.categories.keySet();
  }
}
