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
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("attributioner").setExecutor(new AttributionerCommand(this, manager));
        if (getServer().getPluginManager().isPluginEnabled("RegionEvents")) {
            getLogger().info("RegionEvents plugin found, registering region listener.");
            getServer().getPluginManager().registerEvents(new RegionListener(this, manager), this);
        } else {
            getLogger().warning("RegionEvents plugin not loaded, region-based attribute modifiers will not work.");
        }
    }

    public void loadConfig() {
        regionModifiers.clear();

        ConfigurationSection regions = getConfig().getConfigurationSection("regions");
        if (regions == null) {
            return;
        }

        for (String regionName : regions.getKeys(false)) {
            ConfigurationSection section = regions.getConfigurationSection(regionName);
            if (section == null) continue;

            Map<Attribute, AttributeModifier> modifiers = new HashMap<>();

            for (String attrName : section.getKeys(false)) {
                try {
                    Attribute attribute = Attribute.valueOf(attrName.toUpperCase());
                    double amount = section.getDouble(attrName + ".amount");
                    String opStr = section.getString(attrName + ".operation");
                    AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(opStr);

                    NamespacedKey modKey = new NamespacedKey("attributioner-" + regionName.toLowerCase(), attrName.toLowerCase());
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
