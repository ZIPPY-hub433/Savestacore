package com.savesta.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public final class SavestaCore extends JavaPlugin implements Listener {
    private static final Component MENU_TITLE =
            Component.text("SAVESTA SMP", NamedTextColor.DARK_GREEN);

    private static final Map<Integer, MenuButton> BUTTONS = Map.of(
            10, new MenuButton(Material.EMERALD, "Shop", "shop", "Open the server shop"),
            11, new MenuButton(Material.GOLD_INGOT, "Auction House", "ah", "Browse player listings"),
            12, new MenuButton(Material.BARREL, "Orders", "order", "View your orders"),
            13, new MenuButton(Material.RED_BED, "Homes", "home", "Teleport to your home"),
            14, new MenuButton(Material.ENDER_PEARL, "Random Teleport", "rtp", "Explore a random location"),
            15, new MenuButton(Material.COMPASS, "Spawn", "spawn", "Return to server spawn"),
            22, new MenuButton(Material.COMPARATOR, "Settings", null, "More player settings coming soon")
    );

    @Override
    public void onEnable() {
        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("SavestaCore enabled. Use /settings to open the menu.");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can open the Savesta menu.");
            return true;
        }
        if (!player.hasPermission("savestacore.menu")) {
            player.sendMessage(Component.text("You do not have permission to use this menu.", NamedTextColor.RED));
            return true;
        }
        openMenu(player);
        return true;
    }

    private void openMenu(Player player) {
        MenuHolder holder = new MenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, 27, MENU_TITLE);
        holder.setInventory(inventory);

        for (Map.Entry<Integer, MenuButton> entry : BUTTONS.entrySet()) {
            MenuButton button = entry.getValue();
            ItemStack item = new ItemStack(button.material());
            ItemMeta meta = item.getItemMeta();
            meta.displayName(Component.text(button.name(), NamedTextColor.GREEN));
            meta.lore(List.of(
                    Component.text(button.description(), NamedTextColor.GRAY),
                    Component.text(button.command() == null ? "Click to view" : "Click to open", NamedTextColor.YELLOW)
            ));
            item.setItemMeta(meta);
            inventory.setItem(entry.getKey(), item);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder(false) instanceof MenuHolder)) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }

        MenuButton button = BUTTONS.get(event.getRawSlot());
        if (button == null) {
            return;
        }

        if (button.command() == null) {
            player.sendMessage(Component.text("More settings are coming soon!", NamedTextColor.YELLOW));
            return;
        }

        String command = getConfig().getString("commands." + button.command(), button.command());
        if (command == null || command.isBlank()) {
            player.sendMessage(Component.text("That command is not configured.", NamedTextColor.RED));
            return;
        }

        player.closeInventory();
        String finalCommand = command.startsWith("/") ? command.substring(1) : command;
        Bukkit.getScheduler().runTask(this, () -> player.performCommand(finalCommand));
    }

    private record MenuButton(Material material, String name, String command, String description) {}

    private static final class MenuHolder implements InventoryHolder {
        private Inventory inventory;

        void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
