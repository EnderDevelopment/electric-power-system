package com.enderdevelopment.plugin;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class PowerCommand implements CommandExecutor {

    private final Main plugin;
    private final ItemManager items;
    private final PowerManager power;

    public PowerCommand(Main plugin) {
        this.plugin = plugin;
        this.items = plugin.getItemManager();
        this.power = plugin.getPowerManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command.").color(NamedTextColor.RED));
            return true;
        }

        if (args.length == 1 && args[0].equalsIgnoreCase("give")) {
            giveKit(player);
            return true;
        }

        // Show held tool battery.
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand != null && items.hasBattery(hand)) {
            player.sendMessage(Component.text("Electric tool battery: "
                    + items.getBattery(hand) + "/" + PowerManager.PICKAXE_CAP)
                    .color(NamedTextColor.GOLD));
        }

        // Show engines in a 16 block radius.
        int shown = 0;
        for (int x = -16; x <= 16; x++) {
            for (int y = -16; y <= 16; y++) {
                for (int z = -16; z <= 16; z++) {
                    Location loc = player.getLocation().clone().add(x, y, z);
                    if (!power.isEngine(loc)) {
                        continue;
                    }
                    player.sendMessage(Component.text("[Engine] "
                                    + friendlyName(power.getDevice(loc))
                                    + " at " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ()
                                    + "  Power: " + power.getStored(loc) + "/" + power.getCapacity(loc)
                                    + (power.isOverloaded(loc) ? "  (OVERLOADED)" : ""))
                            .color(NamedTextColor.GREEN));
                    shown++;
                    if (shown >= 24) {
                        break;
                    }
                }
                if (shown >= 24) {
                    break;
                }
            }
            if (shown >= 24) {
                break;
            }
        }
        if (shown == 0) {
            player.sendMessage(Component.text("No electric engines found nearby.").color(NamedTextColor.GRAY));
        }
        return true;
    }

    private String friendlyName(String type) {
        switch (type) {
            case ItemManager.ENGINE_BASIC: return "Electric Engine";
            case ItemManager.ENGINE_DIAMOND: return "Diamond Engine";
            case ItemManager.ENGINE_NETHER: return "Nether Engine";
            default: return type;
        }
    }

    private void giveKit(Player player) {
        ItemStack basic = items.createItem(Material.FURNACE, ItemManager.ENGINE_BASIC, "Electric Engine",
                List.of("Generates " + PowerManager.BASIC_GEN + " electric per tick.",
                        "Capacity: " + PowerManager.BASIC_CAP));
        ItemStack diamond = items.createItem(Material.DIAMOND_BLOCK, ItemManager.ENGINE_DIAMOND, "Diamond Engine",
                List.of("Generates " + PowerManager.DIAMOND_GEN + " electric per tick.",
                        "Capacity: " + PowerManager.DIAMOND_CAP));
        ItemStack nether = items.createItem(Material.NETHERITE_BLOCK, ItemManager.ENGINE_NETHER, "Nether Engine",
                List.of("Generates " + PowerManager.NETHER_GEN + " electric per tick.",
                        "Capacity: " + PowerManager.NETHER_CAP));
        ItemStack cable = items.createItem(Material.COPPER_BLOCK, ItemManager.CABLE, "Electric Cable",
                List.of("Transmits electric between devices."));
        ItemStack furnace = items.createItem(Material.SMOKER, ItemManager.FURNACE, "Electric Furnace",
                List.of("Smelts while it has electric power."));
        ItemStack pump = items.createItem(Material.DISPENSER, ItemManager.PUMP, "Water Pump",
                List.of("Fills buckets with water using electric."));
        ItemStack lamp = items.createItem(Material.REDSTONE_LAMP, ItemManager.LAMP, "Electric Lamp",
                List.of("Glows while it has electric power via a cable."));
        ItemStack alloy = items.createItem(Material.IRON_INGOT, ItemManager.COLD_ALLOY, "Cold Alloy",
                List.of("Cooling alloy used to build machines."));

        ItemStack pickaxe = items.createItem(Material.NETHERITE_PICKAXE, ItemManager.PICKAXE,
                "Electric Pickaxe", List.of("Uses electric to break blocks."));
        items.setBattery(pickaxe, PowerManager.PICKAXE_CAP);

        ItemStack[] kit = {basic, diamond, nether, cable, furnace, pump, lamp, alloy, pickaxe};
        for (ItemStack item : kit) {
            player.getInventory().addItem(item);
        }
        player.sendMessage(Component.text("Electric kit given.").color(NamedTextColor.GREEN));
        player.sendMessage(Component.text("Place engines, connect them with cables, then use machines. "
                        + "See power with /power.")
                .color(NamedTextColor.GRAY));
    }
}
