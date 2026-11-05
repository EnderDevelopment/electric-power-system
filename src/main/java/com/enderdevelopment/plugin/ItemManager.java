package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public final class ItemManager {

    public static final String ENGINE_BASIC = "engine_basic";
    public static final String ENGINE_DIAMOND = "engine_diamond";
    public static final String ENGINE_NETHER = "engine_nether";
    public static final String CABLE = "cable";
    public static final String FURNACE = "electric_furnace";
    public static final String PUMP = "water_pump";
    public static final String COLD_ALLOY = "cold_alloy";
    public static final String PICKAXE = "electric_pickaxe";
    public static final String LAMP = "electric_lamp";

    private final Main plugin;
    private final NamespacedKey typeKey;
    private final NamespacedKey batteryKey;

    public ItemManager(Main plugin) {
        this.plugin = plugin;
        this.typeKey = new NamespacedKey(plugin, "device_type");
        this.batteryKey = new NamespacedKey(plugin, "battery");
    }

    public NamespacedKey getBatteryKey() {
        return batteryKey;
    }

    public ItemStack createItem(Material mat, String type, String name, List<String> lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        List<Component> componentLore = new ArrayList<>();
        for (String line : lore) {
            componentLore.add(Component.text(line));
        }
        meta.lore(componentLore);
        meta.getPersistentDataContainer().set(typeKey, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    public String getType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(typeKey, PersistentDataType.STRING);
    }

    public boolean isEngine(ItemStack item) {
        String type = getType(item);
        return ENGINE_BASIC.equals(type) || ENGINE_DIAMOND.equals(type) || ENGINE_NETHER.equals(type);
    }

    public boolean hasBattery(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(batteryKey, PersistentDataType.INTEGER);
    }

    public int getBattery(ItemStack item) {
        if (!hasBattery(item)) {
            return 0;
        }
        Integer value = item.getItemMeta().getPersistentDataContainer().get(batteryKey, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    public void setBattery(ItemStack item, int energy) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(batteryKey, PersistentDataType.INTEGER, energy);
        item.setItemMeta(meta);
    }

    /** Build every craftable device item exactly once. */
    public ItemStack make(ItemStack template, int amount) {
        ItemStack out = template.clone();
        out.setAmount(amount);
        return out;
    }
}
