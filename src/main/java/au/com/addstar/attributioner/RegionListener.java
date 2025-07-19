package au.com.addstar.attributioner;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.aivruu.regionevents.event.RegionEnteredEvent;
import io.github.aivruu.regionevents.event.RegionQuitEvent;
import io.github.aivruu.regionevents.util.RegionHelper;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class RegionListener implements Listener, CommandExecutor {
    private final Attributioner plugin;
    private final AttributeManager manager;

    public RegionListener(Attributioner plugin, AttributeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
        plugin.getCommand("attributioner").setExecutor(this);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Set<ProtectedRegion> regions = RegionHelper.searchAtLocation(player.getWorld(), player.getLocation());
            for (ProtectedRegion region : regions) {
                String id = region.getId();
                if (plugin.getRegionModifiers().containsKey(id)) {
                    manager.applyModifiers(player, id);
                }
            }
        }, 20L);
    }

    @EventHandler
    public void onRegionEnter(RegionEnteredEvent event) {
        String id = event.region().getId();
        if (plugin.getRegionModifiers().containsKey(id)) {
            manager.applyModifiers(event.player(), id);
        }
    }

    @EventHandler
    public void onRegionLeave(RegionQuitEvent event) {
        String id = event.region().getId();
        if (plugin.getRegionModifiers().containsKey(id)) {
            manager.removeModifiers(event.player(), id);
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!sender.hasPermission("attributioner.reload")) {
            sender.sendMessage("\u00a7cYou do not have permission to use this command.");
            return true;
        }

        plugin.reloadConfig();
        plugin.loadConfig();
        sender.sendMessage("\u00a7aAttributioner config reloaded.");

        // Clear all modifiers for online players and reapply for those in matching regions
        for (Player player : Bukkit.getOnlinePlayers()) {
            plugin.clearAttrModifiers(player);
            Set<ProtectedRegion> regions = RegionHelper.searchAtLocation(player.getWorld(), player.getLocation());
            for (ProtectedRegion region : regions) {
                String id = region.getId();
                if (plugin.getRegionModifiers().containsKey(id)) {
                    manager.applyModifiers(player, id);
                }
            }
        }

        return true;
    }
}