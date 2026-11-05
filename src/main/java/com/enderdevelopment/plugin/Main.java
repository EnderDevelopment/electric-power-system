package com.enderdevelopment.plugin;

import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private static Main instance;
    private PowerManager powerManager;
    private ItemManager itemManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        this.itemManager = new ItemManager(this);
        this.powerManager = new PowerManager(this);
        this.powerManager.load();
        this.powerManager.startTasks();
        getServer().getPluginManager().registerEvents(new MachineListener(this), this);
        getCommand("power").setExecutor(new PowerCommand(this));
        getLogger().info("EnderElectric enabled.");
    }

    @Override
    public void onDisable() {
        if (powerManager != null) {
            powerManager.stopTasks();
            powerManager.save();
        }
        getLogger().info("EnderElectric disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public PowerManager getPowerManager() {
        return powerManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }
}
