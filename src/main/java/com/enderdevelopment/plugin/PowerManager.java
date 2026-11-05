package com.enderdevelopment.plugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class PowerManager {

    public static final int BASIC_GEN = 1;
    public static final int BASIC_CAP = 1000;
    public static final int DIAMOND_GEN = 5;
    public static final int DIAMOND_CAP = 5000;
    public static final int NETHER_GEN = 20;
    public static final int NETHER_CAP = 20000;

    public static final int FURNACE_DRAIN = 4;
    public static final int PUMP_DRAIN = 2;
    public static final int PICKAXE_DRAIN = 5;
    public static final int PICKAXE_CAP = 500;
    public static final int LAMP_DRAIN = 1;

    private final Main plugin;
    private final Map<Location, String> devices = new HashMap<>();
    private final Map<Location, Integer> stored = new HashMap<>();
    private final Set<Location> overloaded = new HashSet<>();

    private int taskId = -1;

    public PowerManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isDevice(Location loc) {
        return devices.containsKey(normalize(loc));
    }

    public String getDevice(Location loc) {
        return devices.get(normalize(loc));
    }

    public boolean isCable(Location loc) {
        return ItemManager.CABLE.equals(getDevice(loc));
    }

    public boolean isEngine(Location loc) {
        String d = getDevice(loc);
        return ItemManager.ENGINE_BASIC.equals(d)
                || ItemManager.ENGINE_DIAMOND.equals(d)
                || ItemManager.ENGINE_NETHER.equals(d);
    }

    public boolean isMachine(Location loc) {
        String d = getDevice(loc);
        return ItemManager.FURNACE.equals(d) || ItemManager.PUMP.equals(d);
    }

    public List<Location> allDeviceLocations() {
        return new ArrayList<>(devices.keySet());
    }

    /** Result of a lamp power request: lights up when connected electric is available. */
    public boolean requestLampPower(Location lamp) {
        return requestPower(lamp, LAMP_DRAIN);
    }

    public void registerDevice(Block block, String type) {
        Location loc = normalize(block.getLocation());
        devices.put(loc, type);
        if (isEngine(loc)) {
            stored.putIfAbsent(loc, 0);
        }
    }

    public void unregisterDevice(Block block) {
        Location loc = normalize(block.getLocation());
        devices.remove(loc);
        stored.remove(loc);
        overloaded.remove(loc);
    }

    public int getCapacity(Location loc) {
        String d = getDevice(loc);
        if (ItemManager.ENGINE_BASIC.equals(d)) {
            return BASIC_CAP;
        }
        if (ItemManager.ENGINE_DIAMOND.equals(d)) {
            return DIAMOND_CAP;
        }
        if (ItemManager.ENGINE_NETHER.equals(d)) {
            return NETHER_CAP;
        }
        return 0;
    }

    public int getGenPerTick(Location loc) {
        String d = getDevice(loc);
        if (ItemManager.ENGINE_BASIC.equals(d)) {
            return BASIC_GEN;
        }
        if (ItemManager.ENGINE_DIAMOND.equals(d)) {
            return DIAMOND_GEN;
        }
        if (ItemManager.ENGINE_NETHER.equals(d)) {
            return NETHER_GEN;
        }
        return 0;
    }

    public int getStored(Location loc) {
        Integer value = stored.get(normalize(loc));
        return value == null ? 0 : value;
    }

    public boolean isOverloaded(Location loc) {
        return overloaded.contains(normalize(loc));
    }

    private Location normalize(Location loc) {
        Location l = loc.clone();
        l.setX(l.getBlockX());
        l.setY(l.getBlockY());
        l.setZ(l.getBlockZ());
        l.setPitch(0);
        l.setYaw(0);
        return l;
    }

    /** Generate power for every registered engine. */
    public void tickGeneration() {
        for (Location loc : new ArrayList<>(devices.keySet())) {
            if (!isEngine(loc)) {
                continue;
            }
            if (overloaded.contains(loc)) {
                continue;
            }
            int current = getStored(loc);
            int cap = getCapacity(loc);
            if (current < cap) {
                stored.put(loc, Math.min(cap, current + getGenPerTick(loc)));
            }
        }
    }

    /**
     * Breadth-first search from a machine through cable / engine / machine blocks
     * to find every reachable engine.
     */
    public List<Location> findConnectedEngines(Location start) {
        List<Location> engines = new ArrayList<>();
        Set<Location> visited = new HashSet<>();
        ArrayDeque<Location> queue = new ArrayDeque<>();
        Location origin = normalize(start);
        visited.add(origin);
        queue.add(origin);
        int[] dx = {1, -1, 0, 0, 0, 0};
        int[] dy = {0, 0, 1, -1, 0, 0};
        int[] dz = {0, 0, 0, 0, 1, -1};
        while (!queue.isEmpty()) {
            Location cur = queue.poll();
            if (isEngine(cur)) {
                engines.add(cur);
            }
            for (int i = 0; i < 6; i++) {
                Location next = cur.clone().add(dx[i], dy[i], dz[i]);
                next = normalize(next);
                if (visited.contains(next)) {
                    continue;
                }
                if (!isCable(next) && !isEngine(next)) {
                    continue;
                }
                visited.add(next);
                queue.add(next);
            }
        }
        return engines;
    }

    /**
     * Try to take <code>amount</code> energy from the engines connected to a
     * machine. Returns true when the full amount was provided.
     */
    public boolean requestPower(Location machine, int amount) {
        Location origin = normalize(machine);
        List<Location> engines = findConnectedEngines(origin);
        int available = 0;
        for (Location e : engines) {
            available += getStored(e);
        }
        if (available < amount) {
            overloaded.add(origin);
            return false;
        }
        int need = amount;
        for (Location e : engines) {
            if (need <= 0) {
                break;
            }
            int have = getStored(e);
            int take = Math.min(have, need);
            stored.put(e, have - take);
            need -= take;
        }
        overloaded.remove(origin);
        return true;
    }

    public void startTasks() {
        taskId = Bukkit.getScheduler().runTaskTimer(plugin, this::tickGeneration, 1L, 1L).getTaskId();
    }

    public void stopTasks() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void save() {
        YamlConfiguration config = new YamlConfiguration();
        List<String> deviceKeys = new ArrayList<>();
        List<String> storedKeys = new ArrayList<>();
        for (Location loc : devices.keySet()) {
            deviceKeys.add(key(loc));
        }
        for (Location loc : stored.keySet()) {
            storedKeys.add(key(loc) + ":" + stored.get(loc));
        }
        config.set("devices", deviceKeys);
        config.set("stored", storedKeys);
        plugin.getDataFolder().mkdirs();
        try {
            config.save(new java.io.File(plugin.getDataFolder(), "power.yml"));
        } catch (java.io.IOException e) {
            plugin.getLogger().warning("Could not save power data: " + e.getMessage());
        }
    }

    public void load() {
        java.io.File file = new java.io.File(plugin.getDataFolder(), "power.yml");
        if (!file.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        List<String> deviceKeys = config.getStringList("devices");
        devices.clear();
        stored.clear();
        for (String s : deviceKeys) {
            String[] parts = s.split("\\|", 5);
            if (parts.length >= 5) {
                Location loc = fromKey(parts[0], parts[1], parts[2], parts[3]);
                devices.put(loc, parts[4]);
                if (isEngine(loc)) {
                    stored.putIfAbsent(loc, 0);
                }
            }
        }
        for (String s : config.getStringList("stored")) {
            int i = s.lastIndexOf(':');
            if (i < 0) {
                continue;
            }
            try {
                int value = Integer.parseInt(s.substring(i + 1));
                String[] parts = s.substring(0, i).split("\\|", 4);
                if (parts.length >= 4) {
                    stored.put(fromKey(parts[0], parts[1], parts[2], parts[3]), value);
                }
            } catch (NumberFormatException ignored) {
                // skip corrupt entry
            }
        }
    }

    private String key(Location loc) {
        String world = loc.getWorld() == null ? "" : loc.getWorld().getName();
        return world + "|" + loc.getBlockX() + "|" + loc.getBlockY() + "|" + loc.getBlockZ();
    }

    private Location fromKey(String world, String x, String y, String z) {
        org.bukkit.World w = world.isEmpty() ? null : Bukkit.getWorld(world);
        if (w == null && !Bukkit.getWorlds().isEmpty()) {
            w = Bukkit.getWorlds().get(0);
        }
        return new Location(w, Integer.parseInt(x), Integer.parseInt(y), Integer.parseInt(z));
    }
}
