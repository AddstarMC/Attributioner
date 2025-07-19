package au.com.addstar.attributioner;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.github.aivruu.regionevents.event.RegionEnteredEvent;
import io.github.aivruu.regionevents.event.RegionQuitEvent;
import io.github.aivruu.regionevents.util.RegionHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.logging.Level;

import java.util.Set;

public class RegionListener implements Listener {
    private final Attributioner plugin;
    private final AttributeManager manager;

    public RegionListener(Attributioner plugin, AttributeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
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
            plugin.debugMsg(String.format("%s entered region %s, applying %s",
                    new Object[]{event.player().getName(), id, plugin.getRegionModifiers().get(id).keySet()}));
            manager.applyModifiers(event.player(), id);
        }
    }

    @EventHandler
    public void onRegionLeave(RegionQuitEvent event) {
        String id = event.region().getId();
        if (plugin.getRegionModifiers().containsKey(id)) {
            plugin.debugMsg(String.format("%s left region %s, removing %s",
                    new Object[]{event.player().getName(), id, plugin.getRegionModifiers().get(id).keySet()}));
            manager.removeModifiers(event.player(), id);
        }
    }

}