package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.Sound;

import java.util.Iterator;
import java.util.List;

public final class MachineListener implements Listener {

    private final Main plugin;
    private final ItemManager items;
    private final PowerManager power;

    public MachineListener(Main plugin) {
        this.plugin = plugin;
        this.items = plugin.getItemManager();
        this.power = plugin.getPowerManager();
        startLampTask();
    }

    private void startLampTask() {
        Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                updateLamps();
            }
        }, 5L, 10L);
    }

    private void updateLamps() {
        for (Location loc : power.allDeviceLocations()) {
            String type = power.getDevice(loc);
            if (!ItemManager.LAMP.equals(type)) {
                continue;
            }
            Block block = loc.getBlock();
            boolean wantsLit = power.requestLampPower(loc);
            setLampLit(block, wantsLit);
        }
    }

    /**
     * Control the lamp's visual light. Minecraft's redstone lamp always emits
     * full light even when "unlit", so to make the area genuinely dark when the
     * lamp is off we swap the block to a dark block. When it is on we place a
     * lit redstone lamp (light 15).
     */
    private static final Material OFF_LAMP_MATERIAL = Material.BLACKSTONE;

    private void setLampLit(Block block, boolean lit) {
        if (lit) {
            if (block.getType() != Material.REDSTONE_LAMP) {
                block.setType(Material.REDSTONE_LAMP);
            }
            org.bukkit.block.data.Lightable lightable =
                    (org.bukkit.block.data.Lightable) block.getBlockData();
            if (!lightable.isLit()) {
                lightable.setLit(true);
                block.setBlockData(lightable);
            }
        } else {
            if (block.getType() != OFF_LAMP_MATERIAL) {
                block.setType(OFF_LAMP_MATERIAL);
            }
        }
    }

    // ---------------- Block placement / breaking ----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        ItemStack item = event.getItemInHand();
        String type = items.getType(item);
        if (type == null) {
            return;
        }
        power.registerDevice(event.getBlockPlaced(), type);
        event.getPlayer().sendMessage(Component.text("Device placed: " + friendlyName(type)).color(NamedTextColor.GREEN));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!power.isDevice(block.getLocation())) {
            return;
        }
        String type = power.getDevice(block.getLocation());
        power.unregisterDevice(block);
        ItemStack drop = deviceItem(type);
        if (drop != null) {
            block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 0.5, 0.5), drop);
        }
        event.setDropItems(false);
    }

    private String friendlyName(String type) {
        switch (type) {
            case ItemManager.ENGINE_BASIC: return "Electric Engine";
            case ItemManager.ENGINE_DIAMOND: return "Diamond Engine";
            case ItemManager.ENGINE_NETHER: return "Nether Engine";
            case ItemManager.CABLE: return "Electric Cable";
            case ItemManager.FURNACE: return "Electric Furnace";
            case ItemManager.PUMP: return "Water Pump";
            case ItemManager.LAMP: return "Electric Lamp";
            default: return type;
        }
    }

    private ItemStack deviceItem(String type) {
        if (type == null) {
            return null;
        }
        switch (type) {
            case ItemManager.ENGINE_BASIC:
                return items.createItem(Material.FURNACE, ItemManager.ENGINE_BASIC, "Electric Engine",
                        List.of("Generates " + PowerManager.BASIC_GEN + " electric per tick.",
                                "Capacity: " + PowerManager.BASIC_CAP));
            case ItemManager.ENGINE_DIAMOND:
                return items.createItem(Material.DIAMOND_BLOCK, ItemManager.ENGINE_DIAMOND, "Diamond Engine",
                        List.of("Generates " + PowerManager.DIAMOND_GEN + " electric per tick.",
                                "Capacity: " + PowerManager.DIAMOND_CAP));
            case ItemManager.ENGINE_NETHER:
                return items.createItem(Material.NETHERITE_BLOCK, ItemManager.ENGINE_NETHER, "Nether Engine",
                        List.of("Generates " + PowerManager.NETHER_GEN + " electric per tick.",
                                "Capacity: " + PowerManager.NETHER_CAP));
            case ItemManager.CABLE:
                return items.createItem(Material.COPPER_BLOCK, ItemManager.CABLE, "Electric Cable",
                        List.of("Transmits electric between devices."));
            case ItemManager.FURNACE:
                return items.createItem(Material.SMOKER, ItemManager.FURNACE, "Electric Furnace",
                        List.of("Smelts while it has electric power."));
            case ItemManager.PUMP:
                return items.createItem(Material.DISPENSER, ItemManager.PUMP, "Water Pump",
                        List.of("Fills buckets with water using electric."));
            case ItemManager.LAMP:
                return items.createItem(Material.REDSTONE_LAMP, ItemManager.LAMP, "Electric Lamp",
                        List.of("Glows while it has electric power via a cable."));
            default:
                return null;
        }
    }

    // ---------------- Interactions ----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        Location loc = block.getLocation();
        String type = power.getDevice(loc);

        // Recharge a tool battery by sneaking on an engine.
        if (type != null && power.isEngine(loc) && player.isSneaking()
                && player.getInventory().getItemInMainHand() != null
                && items.hasBattery(player.getInventory().getItemInMainHand())) {
            chargeTool(player, loc);
            event.setCancelled(true);
            return;
        }

        if (ItemManager.FURNACE.equals(type)) {
            openFurnace(player, block);
            event.setCancelled(true);
            return;
        }

        if (ItemManager.PUMP.equals(type)) {
            pumpWater(player, loc);
            event.setCancelled(true);
        }
    }

    private void chargeTool(Player player, Location engine) {
        ItemStack tool = player.getInventory().getItemInMainHand();
        int need = PowerManager.PICKAXE_CAP - items.getBattery(tool);
        if (need <= 0) {
            player.sendMessage(Component.text("Tool is already fully charged.").color(NamedTextColor.GRAY));
            return;
        }
        int amount = Math.min(need, PowerManager.PICKAXE_DRAIN * 20);
        if (power.requestPower(engine, amount)) {
            items.setBattery(tool, items.getBattery(tool) + amount);
            player.sendMessage(Component.text("Tool charged. Battery: "
                    + items.getBattery(tool) + "/" + PowerManager.PICKAXE_CAP).color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Engine has no electric power.").color(NamedTextColor.RED));
        }
    }

    private void openFurnace(Player player, Block block) {
        FurnaceHolder holder = new FurnaceHolder(block.getLocation());
        Inventory inv = Bukkit.createInventory(holder, 9, Component.text("Electric Furnace"));
        holder.setInventory(inv);
        player.openInventory(inv);
    }

    private void pumpWater(Player player, Location loc) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.BUCKET || hand.getAmount() != 1) {
            player.sendMessage(Component.text("Hold a bucket to fill it with water.")
                    .color(NamedTextColor.YELLOW));
            return;
        }
        if (!power.requestPower(loc, PowerManager.PUMP_DRAIN)) {
            player.sendMessage(Component.text("Pump has no electric power.").color(NamedTextColor.RED));
            return;
        }
        hand.setAmount(0);
        player.getInventory().addItem(new ItemStack(Material.WATER_BUCKET));
        player.playSound(player.getLocation(), Sound.ITEM_BUCKET_FILL, 1.0f, 1.0f);
        player.sendMessage(Component.text("Pump filled a water bucket.").color(NamedTextColor.GREEN));
    }

    // ---------------- Electric furnace smelting ----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FurnaceHolder)) {
            return;
        }
        // The furnace window is chest-shaped (9 slots). Block moving items into
        // occupied slots except input/output handled by slot logic below.
        if (event.getClickedInventory() == null
                || event.getClickedInventory().getType() == InventoryType.PLAYER) {
            return;
        }
        // prevent interaction with the 7 reserved middle slots
        int slot = event.getSlot();
        if (slot == FurnaceHolder.INPUT || slot == FurnaceHolder.OUTPUT) {
            return; // allowed
        }
        event.setCancelled(true); // reserved area
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFurnaceClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof FurnaceHolder)) {
            return;
        }
        if (event.getSlot() != FurnaceHolder.INPUT) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        Inventory inv = event.getInventory();
        ItemStack input = inv.getItem(FurnaceHolder.INPUT);
        if (input == null || input.getAmount() <= 0) {
            return;
        }
        trySmelt(player, inv, (FurnaceHolder) inv.getHolder(), input);
    }

    private void trySmelt(Player player, Inventory inv, FurnaceHolder holder, ItemStack input) {
        ItemStack result = findSmeltResult(input);
        if (result == null) {
            return;
        }
        Location furnaceLoc = holder.getLocation();
        if (furnaceLoc == null) {
            return;
        }
        if (!power.requestPower(furnaceLoc, PowerManager.FURNACE_DRAIN)) {
            player.sendMessage(Component.text("Electric Furnace has no electric power.")
                    .color(NamedTextColor.RED));
            return;
        }
        ItemStack output = inv.getItem(FurnaceHolder.OUTPUT);
        if (output == null || output.getType().isAir()) {
            ItemStack produced = result.clone();
            produced.setAmount(1);
            inv.setItem(FurnaceHolder.OUTPUT, produced);
        } else if (output.isSimilar(result) && output.getAmount() < output.getMaxStackSize()) {
            output.setAmount(output.getAmount() + 1);
            inv.setItem(FurnaceHolder.OUTPUT, output);
        } else {
            return;
        }
        input.setAmount(input.getAmount() - 1);
        inv.setItem(FurnaceHolder.INPUT, input.getAmount() <= 0 ? null : input);
        player.playSound(player.getLocation(), Sound.BLOCK_FURNACE_FIRE_CRACKLE, 1.0f, 1.0f);
    }

    private ItemStack findSmeltResult(ItemStack input) {
        Iterator<Recipe> it = Bukkit.recipeIterator();
        while (it.hasNext()) {
            Recipe r = it.next();
            if (r instanceof FurnaceRecipe furnace) {
                ItemStack in = furnace.getInput();
                if (in != null && in.isSimilar(input)) {
                    ItemStack out = furnace.getResult();
                    if (out != null && out.getType() != Material.AIR) {
                        return out;
                    }
                }
            }
        }
        return null;
    }

    // ---------------- Electric pickaxe ----------------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        if (tool == null || !items.hasBattery(tool)) {
            return;
        }
        int battery = items.getBattery(tool);
        if (battery < PowerManager.PICKAXE_DRAIN) {
            event.setCancelled(true);
            player.sendMessage(Component.text("Electric tool battery is empty. Recharge it on an engine.")
                    .color(NamedTextColor.RED));
            return;
        }
        items.setBattery(tool, battery - PowerManager.PICKAXE_DRAIN);
    }
}
