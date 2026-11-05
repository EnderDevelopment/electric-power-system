# Electric Power System Minecraft Plugin

EnderElectric is a Minecraft plugin that adds a complete electric power system to your server. Build engines, connect them with cables, and power electric furnaces, water pumps, lamps, and an electric pickaxe. Choose from basic, diamond, and nether engines to generate electric power, then transfer it through cables to your machines. The plugin features real-time power distribution, battery charging for tools, persistent power storage, and overload protection to help you manage your electric grid.

## Install

1. Download the EnderElectric plugin JAR file.
2. Place the JAR file into the `plugins` folder of your Paper server (version 1.21 or newer).
3. Restart your server to enable the plugin.
4. Verify it loaded correctly by checking the server console for the message "EnderElectric enabled."

## Use

1. **Get the starter kit:** Run `/power give` to receive one of each item: Electric Engine, Diamond Engine, Nether Engine, Electric Cable, Electric Furnace, Water Pump, Electric Lamp, and Electric Pickaxe.

2. **Place engines:** Place any engine block (Electric, Diamond, or Nether). Each engine has a generation rate and a storage capacity:
   - Electric Engine: generates 1 electric per tick, stores up to 1000.
   - Diamond Engine: generates 5 electric per tick, stores up to 5000.
   - Nether Engine: generates 20 electric per tick, stores up to 20000.

3. **Connect with cables:** Place Electric Cable blocks between your engines and machines to transmit power. Each connected machine will automatically pull power from any engine reachable through the cable network.

4. **Use machines:**
   - **Electric Furnace:** Right-click it to open a 9-slot interface. Place smeltable items in slot 1; smelted results appear in slot 9. Each smelt operation uses 4 electric from connected engines.
   - **Water Pump:** Right-click it while holding an empty bucket to fill it with water (uses 2 electric).
   - **Electric Lamp:** Place it near a powered cable network. It lights up when connected engines have power, consuming 1 electric per tick.
   - **Electric Pickaxe:** Hold it to break blocks (uses 5 electric per block). If the battery is empty, you cannot break blocks until you recharge.

5. **Recharge the pickaxe:** Sneak-right-click an engine while holding the Electric Pickaxe to transfer power from the engine into the tool battery (capacity 500).

6. **Check power levels:** Run `/power` while near your engines to see the stored power and capacity of each engine within a 16-block radius, plus the battery level of any electric tool in your hand. Engines that are overloaded will show "(OVERLOADED)".

7. **Manage overloads:** If a machine tries to draw more power than connected engines can supply, it reports overload and stops working until enough power is available. Build more powerful engines or add more engines to your network to prevent this.

---

More Minecraft plugin projects by EnderDevelopment: https://github.com/EnderDevelopment

Project page: https://dash.enderdevelopment.com/community/2b3ae8d1-2788-4881-8a4d-5dd9bad9f4cb?utm_source=github&utm_medium=readme&utm_campaign=auto_publish&utm_content=2b3ae8d1-2788-4881-8a4d-5dd9bad9f4cb
