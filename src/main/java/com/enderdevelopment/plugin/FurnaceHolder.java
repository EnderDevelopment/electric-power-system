package com.enderdevelopment.plugin;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public final class FurnaceHolder implements InventoryHolder {

    public static final int INPUT = 0;
    public static final int OUTPUT = 8;

    private Inventory inventory;
    private Location location;

    public FurnaceHolder(Location location) {
        this.location = location;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public Location getLocation() {
        return location;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
