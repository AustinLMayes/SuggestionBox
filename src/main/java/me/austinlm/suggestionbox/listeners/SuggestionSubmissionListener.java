package me.austinlm.suggestionbox.listeners;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.UUID;
import me.austinlm.suggestionbox.CategoryRegistry;
import me.austinlm.suggestionbox.SuggestionBoxPlugin;
import me.austinlm.suggestionbox.SuggestionCategory;
import me.austinlm.suggestionbox.db.SuggestionService;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Listener which handles suggestions from books.
 *
 * @author Austin Mayes
 */
public class SuggestionSubmissionListener implements Listener {

  private static final HashMap<UUID, ItemStack> REPLACED_ITEMS = Maps.newHashMap();

  private static final NamespacedKey IS_SUGGESTION_BOOK = new NamespacedKey(
      SuggestionBoxPlugin.INSTANCE, "is-suggestion-book");
  private static final NamespacedKey SUGGESTION_CATEGORY = new NamespacedKey(
      SuggestionBoxPlugin.INSTANCE, "suggestion-category");
  public static BaseComponent[] ERROR = new ComponentBuilder(
      "There was an error submitting your suggestion, please try again!").color(ChatColor.RED)
      .create();
  public static BaseComponent[] SUCCESS = new ComponentBuilder(
      "Your suggestion has been successfully submitted! Thanks!").color(ChatColor.GREEN).create();
  private final CategoryRegistry registry;
  private final SuggestionService service;

  public SuggestionSubmissionListener(CategoryRegistry registry,
      SuggestionService service) {
    this.registry = registry;
    this.service = service;
  }

  public static void openBook(Player player, SuggestionCategory category) {
    ItemStack book = new ItemStack(Material.WRITABLE_BOOK, 1);
    BookMeta meta = (BookMeta) book.getItemMeta();

    meta.getPersistentDataContainer().set(IS_SUGGESTION_BOOK, PersistentDataType.BYTE, (byte) 1);
    meta.getPersistentDataContainer()
        .set(SUGGESTION_CATEGORY, PersistentDataType.STRING, category.getSlug());

    meta.setDisplayName(player.getDisplayName() + "'s Suggestion");

    book.setItemMeta(meta);

    PlayerInventory playerInv = player.getInventory();

    int slot = playerInv.getHeldItemSlot();
    REPLACED_ITEMS.put(player.getUniqueId(), playerInv.getItem(slot));
    playerInv.setItem(slot, book);
    player.sendMessage(new ComponentBuilder("Right-click the book to submit your suggestion")
        .color(ChatColor.YELLOW).create());
  }

  private boolean isBook(ItemMeta meta) {
    return meta.getPersistentDataContainer().has(IS_SUGGESTION_BOOK, PersistentDataType.BYTE);
  }

  @EventHandler
  public void onWrite(PlayerEditBookEvent event) {
    BookMeta newMeta = event.getNewBookMeta();
    Player player = event.getPlayer();
    if (!isBook(newMeta)) {
      return;
    }
    player.getInventory()
        .setItem(player.getInventory().getHeldItemSlot(), REPLACED_ITEMS.get(player.getUniqueId()));

    String catRaw = newMeta.getPersistentDataContainer()
        .get(SUGGESTION_CATEGORY, PersistentDataType.STRING);
    if (catRaw == null) {
      throw new IllegalStateException("Tagged book has no category");
    }
    SuggestionCategory category = this.registry.get(catRaw);
    if (category == null) {
      player.sendMessage(ERROR);
      return;
    }

    service
        .submit(player.getUniqueId(), category, StringUtils.join(newMeta.getPages(), " "), (b) -> {
          if (player.isOnline()) {
            player.sendMessage(SUCCESS);
          }
        }, e -> {
          if (player.isOnline()) {
            player.sendMessage(ERROR);
          }
          Bukkit.getLogger().severe("Failed to submit suggestion for " + player.getName());
          e.printStackTrace();
        });
  }
}
