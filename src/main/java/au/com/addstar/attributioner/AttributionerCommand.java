package au.com.addstar.attributioner;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.aivruu.regionevents.util.RegionHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class AttributionerCommand implements CommandExecutor {
    private final Attributioner plugin;
    private final AttributeManager manager;

    public AttributionerCommand(Attributioner plugin, AttributeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("regions")) {
            if (!sender.hasPermission("attributioner.reload")) {
                sender.sendMessage("\u00a7cYou do not have permission to use this command.");
                return true;
            }

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
            if (!sender.hasPermission("attributioner.reload")) {
                sender.sendMessage("\u00a7cYou do not have permission to use this command.");
                return true;
            }

            // Toggle debug logging
            Level newLevel = plugin.getLogger().getLevel() == Level.FINE ? Level.INFO : Level.FINE;
            plugin.getLogger().setLevel(newLevel);
            sender.sendMessage(newLevel == Level.FINE ? "\u00a7aDebug logging enabled." : "\u00a7eDebug logging disabled.");
        } else if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("attributioner.reload")) {
                sender.sendMessage("\u00a7cYou do not have permission to use this command.");
                return true;
            }

            // Reload the plugin configuration
            plugin.reloadConfig();
            plugin.loadConfig();
            sender.sendMessage("\u00a7aAttributioner config reloaded.");

            // Clear all attribute modifiers for online players
            for (Player player : Bukkit.getOnlinePlayers()) {
                plugin.clearAttrModifiers(player);
            }
        } else {
            sender.sendMessage("\u00a7cUsage: /attributioner <regions|debug|reload>");
        }
        return true;
    }
}

