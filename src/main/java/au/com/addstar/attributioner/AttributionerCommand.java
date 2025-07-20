package au.com.addstar.attributioner;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class AttributionerCommand implements CommandExecutor {
    private final Attributioner plugin;
    private final AttributeManager manager;

    public AttributionerCommand(Attributioner plugin, AttributeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("attributioner.admin")) {
            sender.sendMessage("\u00a7cYou do not have permission to use this command.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("regions")) {
            if (plugin.getRegionModifiers().isEmpty()) {
                sender.sendMessage("\u00a7eNo region attributes configured.");
                return true;
            }

            // List all regions with their attribute modifiers
            for (Map.Entry<String, Map<org.bukkit.attribute.Attribute, org.bukkit.attribute.AttributeModifier>> entry : plugin.getRegionModifiers().entrySet()) {
                sender.sendMessage("\u00a7aRegion \u00a7f" + entry.getKey());
                for (org.bukkit.attribute.Attribute attr : entry.getValue().keySet()) {
                    sender.sendMessage("  - " + attr);
                }
            }
        } else if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            // Toggle debug logging
            plugin.setebugMode(!plugin.isDebugMode());
            sender.sendMessage(plugin.isDebugMode() ? "\u00a7aDebug logging enabled." : "\u00a7eDebug logging disabled.");
        } else if (args.length > 0 && args[0].equalsIgnoreCase("info")) {
            if (args.length < 2) {
                sender.sendMessage("\u00a7cUsage: /attributioner info <player>");
                return true;
            }

            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("\u00a7cPlayer not found.");
                return true;
            }

            sender.sendMessage("\u00a7aCustom modifiers for \u00a7f" + target.getName() + ":");
            boolean found = false;
            for (Attribute attr : Attribute.values()) {
                AttributeInstance inst = target.getAttribute(attr);
                if (inst == null) continue;
                for (AttributeModifier mod : inst.getModifiers()) {
                    if (mod.getKey() != null && mod.getKey().getNamespace().startsWith("attributioner-")) {
                        sender.sendMessage("  " + attr + " -> " + mod.getAmount() + " " + mod.getOperation() + " (" + mod.getKey() + ")");
                        found = true;
                    }
                }
            }
            if (!found) {
                sender.sendMessage("  None");
            }
        } else if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            // Reload the plugin configuration
            plugin.reloadConfig();
            plugin.loadConfig();
            sender.sendMessage("\u00a7aAttributioner config reloaded.");

            // Clear all attribute modifiers for online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.clearAttrModifiers(player);
            }
        } else {
            sender.sendMessage("\u00a7cUsage: /attributioner <regions|debug|info|reload>");
        }
        return true;
    }
}

