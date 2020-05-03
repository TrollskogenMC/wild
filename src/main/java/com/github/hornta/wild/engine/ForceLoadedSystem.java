package com.github.hornta.wild.engine;

import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.WildPlugin;
import com.github.hornta.wild.WorldUnit;
import com.github.hornta.wild.events.BufferedLocationEvent;
import com.github.hornta.wild.events.ConfigReloadedEvent;
import com.github.hornta.wild.events.PollLocationEvent;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;

public class ForceLoadedSystem implements Listener {
  private final WildPlugin wildPlugin;
  private final WildManager wildManager;
  private int maxNumKeepLoaded;
  private final HashMap<Chunk, Set<Location>> ticketsByChunk;

  ForceLoadedSystem(WildPlugin wildPlugin, WildManager wildManager) {
    this.wildPlugin = wildPlugin;
    this.wildManager = wildManager;
    maxNumKeepLoaded = wildPlugin.getConfiguration().get(ConfigKey.PERF_KEEP_BUFFER_LOADED);
    ticketsByChunk = new HashMap<>();
  }

  @EventHandler(ignoreCancelled = true)
  void onBufferedLocation(BufferedLocationEvent event) {
    WorldUnit worldUnit = wildManager.getWorldUnitByWorld(event.getLocation().getWorld());
    if(worldUnit.getLocations().size() <= maxNumKeepLoaded) {
      event.getLocation().getChunk().addPluginChunkTicket(wildPlugin);
      ticketsByChunk.putIfAbsent(event.getLocation().getChunk(), new HashSet<>());
      ticketsByChunk.get(event.getLocation().getChunk()).add(event.getLocation());
    }
  }

  @EventHandler
  void onPollLocation(PollLocationEvent event) {
    Location location = event.getLocation();
    Chunk chunk = location.getChunk();
    World world = location.getWorld();
    ticketsByChunk.get(chunk).remove(location);
    if(ticketsByChunk.get(chunk).isEmpty()) {
      chunk.removePluginChunkTicket(wildPlugin);
      ticketsByChunk.remove(chunk);
    }
    WorldUnit worldUnit = wildManager.getWorldUnitByWorld(world);
    if(worldUnit.getLocations().size() < maxNumKeepLoaded) {
      return;
    }
    Location loc = worldUnit.getLocations().get(maxNumKeepLoaded - 1);
    loc.getChunk().addPluginChunkTicket(wildPlugin);
    ticketsByChunk.putIfAbsent(loc.getChunk(), new HashSet<>());
    ticketsByChunk.get(loc.getChunk()).add(loc);
  }

  @EventHandler(priority = EventPriority.LOW)
  void onConfigReloaded(ConfigReloadedEvent event) {
    for(WorldUnit worldUnit : wildManager.getWorldUnits()) {
      worldUnit.getWorld().removePluginChunkTickets(wildPlugin);
    }

    ticketsByChunk.clear();
    maxNumKeepLoaded = wildPlugin.getConfiguration().get(ConfigKey.PERF_KEEP_BUFFER_LOADED);
  }

  public int getNumberOfLocations(Chunk chunk) {
    return ticketsByChunk.getOrDefault(chunk, Collections.emptySet()).size();
  }
}
