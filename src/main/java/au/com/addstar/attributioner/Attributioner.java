package au.com.addstar.attributioner;

import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class Attributioner extends JavaPlugin implements Listener {
    private final Map<String, Map<Attribute, AttributeModifier>> regionModifiers = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadConfig();

        AttributeManager manager = new AttributeManager(this);
        getServer().getPluginManager().registerEvents(new RegionListener(this, manager), this);
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("attributioner").setExecutor(new AttributionerCommand(this, manager));
    }

    public void loadConfig() {
        regionModifiers.clear();
        List<Map<?, ?>> regions = getConfig().getMapList("regions");

        for (int i = 0; i < regions.size(); i++) {
            Map<?, ?> entry = regions.get(i);
            String regionName = (String) entry.get("name");
            if (regionName == null || !entry.containsKey("modifiers")) continue;

            ConfigurationSection section = getConfig().getConfigurationSection("regions." + i + ".modifiers");
            Map<Attribute, AttributeModifier> modifiers = new HashMap<>();

            for (String attrName : section.getKeys(false)) {
                try {
                    Attribute attribute = Attribute.valueOf(attrName.toUpperCase());
                    double amount = section.getDouble(attrName + ".amount");
                    String opStr = section.getString(attrName + ".operation");
                    AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(opStr);

                    NamespacedKey modKey = new NamespacedKey("attributioner-" + regionName.toLowerCase(), attrName.toLowerCase());
                    //"Attributioner-" + regionName + "-" + attrName,
                    AttributeModifier modifier = new AttributeModifier(
                            modKey,
                            amount,
                            op
                    );
                    modifiers.put(attribute, modifier);
                } catch (IllegalArgumentException e) {
                    getLogger().warning("Invalid attribute name: " + attrName + " in region " + regionName);
                }
            }

            if (!modifiers.isEmpty()) {
                regionModifiers.put(regionName, modifiers);
            }
        }
    }

    public Map<String, Map<Attribute, AttributeModifier>> getRegionModifiers() {
        return regionModifiers;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        clearAttrModifiers(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearAttrModifiers(event.getPlayer());
    }

    public void clearAttrModifiers(Player player) {
        for (Attribute attribute : Attribute.values()) {
            AttributeInstance instance = player.getAttribute(attribute);
            if (instance == null) continue;
            for (AttributeModifier modifier : instance.getModifiers()) {
                if (modifier.getKey() != null && modifier.getKey().getNamespace().startsWith("attributioner-")) {
                    instance.removeModifier(modifier);
                }
            }
        }
    }
}
