package au.com.addstar.attributioner;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class AttributeManager {
    public static final String TIMED_NAMESPACE = "attributioner-timed";
    public static final String MANUAL_NAMESPACE = "attributioner-manual";

    private final Attributioner plugin;
    private final Map<UUID, Map<Attribute, BukkitTask>> timedTasks = new HashMap<>();

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

    public static Registry<Attribute> attributeRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE);
    }

    /**
     * Looks up an attribute by key, e.g. "scale" or "minecraft:scale".
     *
     * @return the attribute, or null if the name is not a valid key or no such attribute exists
     */
    public static Attribute lookupAttribute(String name) {
        NamespacedKey key = NamespacedKey.fromString(name.toLowerCase(Locale.ROOT));
        return key == null ? null : attributeRegistry().get(key);
    }

    public static NamespacedKey timedKey(Attribute attribute) {
        return new NamespacedKey(TIMED_NAMESPACE, attribute.getKey().getKey());
    }

    public static NamespacedKey manualKey(Attribute attribute) {
        return new NamespacedKey(MANUAL_NAMESPACE, attribute.getKey().getKey());
    }

    /**
     * Applies a modifier that stays until removed by command or the usual join/quit/reload cleanup.
     * Any existing manual modifier for the same attribute on this player is replaced.
     *
     * @return false if the player does not have the attribute
     */
    public boolean applyManualModifier(Player player, Attribute attribute, double amount,
                                       AttributeModifier.Operation operation) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return false;

        NamespacedKey key = manualKey(attribute);
        if (instance.getModifier(key) != null) {
            instance.removeModifier(key);
        }
        instance.addModifier(new AttributeModifier(key, amount, operation));
        plugin.debugMsg(String.format("Applied manual %s %s %s to %s",
                attribute.getKey(), amount, operation, player.getName()));
        return true;
    }

    /**
     * Removes the manual and timed modifiers for one attribute, cancelling any pending timer.
     * Region modifiers are left alone.
     *
     * @return the number of modifiers removed
     */
    public int removeCommandModifiers(Player player, Attribute attribute) {
        cancelTimed(player.getUniqueId(), attribute);
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return 0;

        int removed = 0;
        for (NamespacedKey key : List.of(manualKey(attribute), timedKey(attribute))) {
            if (instance.getModifier(key) != null) {
                instance.removeModifier(key);
                removed++;
                plugin.debugMsg(String.format("Removed %s from %s", key, player.getName()));
            }
        }
        return removed;
    }

    /**
     * Removes every manual and timed modifier from a player, cancelling any pending timers.
     * Region modifiers are left alone.
     *
     * @return the number of modifiers removed
     */
    public int removeCommandModifiers(Player player) {
        cancelTimed(player.getUniqueId());
        int removed = 0;
        for (Attribute attribute : attributeRegistry()) {
            removed += removeCommandModifiers(player, attribute);
        }
        return removed;
    }

    /**
     * Applies a modifier that is removed after the given number of seconds. Any existing timed
     * modifier for the same attribute on this player is replaced and its timer restarted.
     *
     * @return false if the player does not have the attribute
     */
    public boolean applyTimedModifier(Player player, Attribute attribute, double amount,
                                      AttributeModifier.Operation operation, int seconds) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) return false;

        NamespacedKey key = timedKey(attribute);
        UUID uuid = player.getUniqueId();
        Map<Attribute, BukkitTask> tasks = timedTasks.computeIfAbsent(uuid, k -> new HashMap<>());

        BukkitTask previous = tasks.remove(attribute);
        if (previous != null) {
            previous.cancel();
        }
        if (instance.getModifier(key) != null) {
            instance.removeModifier(key);
        }
        instance.addModifier(new AttributeModifier(key, amount, operation));
        plugin.debugMsg(String.format("Applied timed %s %s %s to %s for %ds",
                attribute.getKey(), amount, operation, player.getName(), seconds));

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Map<Attribute, BukkitTask> current = timedTasks.get(uuid);
            if (current != null) {
                current.remove(attribute);
                if (current.isEmpty()) {
                    timedTasks.remove(uuid);
                }
            }
            Player online = Bukkit.getPlayer(uuid);
            if (online != null) {
                removeTimedModifier(online, attribute);
            }
        }, seconds * 20L);
        tasks.put(attribute, task);
        return true;
    }

    private void removeTimedModifier(Player player, Attribute attribute) {
        AttributeInstance instance = player.getAttribute(attribute);
        NamespacedKey key = timedKey(attribute);
        if (instance != null && instance.getModifier(key) != null) {
            instance.removeModifier(key);
            plugin.debugMsg(String.format("Removed timed %s from %s", attribute.getKey(), player.getName()));
        }
    }

    private void cancelTimed(UUID uuid, Attribute attribute) {
        Map<Attribute, BukkitTask> tasks = timedTasks.get(uuid);
        if (tasks == null) return;
        BukkitTask task = tasks.remove(attribute);
        if (task != null) {
            task.cancel();
        }
        if (tasks.isEmpty()) {
            timedTasks.remove(uuid);
        }
    }

    /** Cancels pending timed tasks for a player without touching their modifiers. */
    public void cancelTimed(UUID uuid) {
        Map<Attribute, BukkitTask> tasks = timedTasks.remove(uuid);
        if (tasks != null) {
            tasks.values().forEach(BukkitTask::cancel);
        }
    }

    /** Cancels all pending timed tasks and removes their modifiers from online players. */
    public void clearAllTimed() {
        for (Map.Entry<UUID, Map<Attribute, BukkitTask>> entry : timedTasks.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            for (Map.Entry<Attribute, BukkitTask> task : entry.getValue().entrySet()) {
                task.getValue().cancel();
                if (player != null) {
                    removeTimedModifier(player, task.getKey());
                }
            }
        }
        timedTasks.clear();
    }
}
