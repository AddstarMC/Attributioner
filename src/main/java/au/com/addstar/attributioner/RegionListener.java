package au.com.addstar.attributioner;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitAdapter;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionContainer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Periodically checks the regions each player is in and triggers
 * attribute application or removal when regions change.
 */
public class RegionListener implements Listener, Runnable {
    private final Attributioner plugin;
    private final AttributeManager manager;
    private final Map<UUID, Set<String>> playerRegions = new HashMap<>();
    private final long interval;
    private BukkitTask task;

    public RegionListener(Attributioner plugin, AttributeManager manager, long intervalTicks) {
        this.plugin = plugin;
        this.manager = manager;
        this.interval = intervalTicks;
    }

    /** Start polling online players */
    public void start() {
        task = Bukkit.getScheduler().runTaskTimer(plugin, this, 20L, interval);
    }

    /** Stop the polling task */
    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> updatePlayer(event.getPlayer()), 20L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        playerRegions.remove(event.getPlayer().getUniqueId());
    }

    private void updatePlayer(Player player) {
        Set<String> current = getRegions(player);
        Set<String> previous = playerRegions.getOrDefault(player.getUniqueId(), Collections.emptySet());

        // Entered regions
        for (String id : current) {
            if (!previous.contains(id) && plugin.getRegionModifiers().containsKey(id)) {
                plugin.debugMsg(String.format("%s entered region %s, applying %s", player.getName(), id,
                        plugin.getRegionModifiers().get(id).keySet()));
                manager.applyModifiers(player, id);
            }
        }

        // Left regions
        for (String id : previous) {
            if (!current.contains(id) && plugin.getRegionModifiers().containsKey(id)) {
                plugin.debugMsg(String.format("%s left region %s, removing %s", player.getName(), id,
                        plugin.getRegionModifiers().get(id).keySet()));
                manager.removeModifiers(player, id);
            }
        }

        playerRegions.put(player.getUniqueId(), current);
    }

    private Set<String> getRegions(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(player.getWorld()));
        if (manager == null) {
            return Collections.emptySet();
        }
        ApplicableRegionSet set = manager.getApplicableRegions(BukkitAdapter.asBlockVector(player.getLocation()));
        Set<String> ids = new HashSet<>();
        for (ProtectedRegion region : set) {
            ids.add(region.getId());
        }
        return ids;
    }
}
