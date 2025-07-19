package au.com.addstar.attributioner;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.logging.Level;

public class AttributeManager {
    private final Attributioner plugin;

    public AttributeManager(Attributioner plugin) {
        this.plugin = plugin;
    }

    public void applyModifiers(Player player, String regionName) {
        Map<Attribute, AttributeModifier> modifiers = plugin.getRegionModifiers().get(regionName);
        if (modifiers == null) return;

        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());
            if (instance != null && instance.getModifier(entry.getValue().getKey()) == null) {
                instance.addModifier(entry.getValue());
                plugin.debugMsg(String.format("Applied %s to %s in region %s",
                        entry.getKey(), player.getName(), regionName));
            }
        }
    }

    public void removeModifiers(Player player, String regionName) {
        Map<Attribute, AttributeModifier> modifiers = plugin.getRegionModifiers().get(regionName);
        if (modifiers == null) return;

        for (Map.Entry<Attribute, AttributeModifier> entry : modifiers.entrySet()) {
            AttributeInstance instance = player.getAttribute(entry.getKey());
            if (instance != null && instance.getModifier(entry.getValue().getKey()) != null) {
                instance.removeModifier(entry.getValue().getKey());
                plugin.debugMsg(String.format("Removed %s from %s in region %s",
                        entry.getKey(), player.getName(), regionName));
            }
        }
    }
}