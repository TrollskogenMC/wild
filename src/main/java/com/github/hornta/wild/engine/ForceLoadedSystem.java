package com.github.hornta.wild.engine;

import com.github.hornta.wild.ConfigKey;
import com.github.hornta.wild.WildPlugin;
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
  private WildPlugin wildPlugin;
  private WildManager wildManager;
  private int maxNumKeepLoaded;
  private HashMap<Chunk, Set<Location>> ticketsByChunk;

  ForceLoadedSystem(WildPlugin wildPlugin, WildManager wildManager) {
    this.wildPlugin = wildPlugin;
    this.wildManager = wildManager;
    maxNumKeepLoaded = wildPlugin.getConfiguration().get(ConfigKey.PERF_KEEP_BUFFER_LOADED);
    ticketsByChunk = new HashMap<>();
  }

  @EventHandler(ignoreCancelled = true)
  void onBufferedLocation(BufferedLocationEvent event) {
    Queue<Location> locations = wildManager.getLocations(event.getLocation().getWorld());
    if(locations.size() <= maxNumKeepLoaded) {
      event.getLocation().getChunk().addPluginChunkTicket(wildPlugin);
      ticketsByChunk.putIfAbsent(event.getLocation().getChunk(), new HashSet<>());
      ticketsByChunk.get(event.getLocation().getChunk()).add(event.getLocation());
    }
  }

  @EventHandler
  void onPollLocation(PollLocationEvent event) {
    ticketsByChunk.get(event.getLocation().getChunk()).remove(event.getLocation());
    if(ticketsByChunk.get(event.getLocation().getChunk()).size() == 1) {
      event.getLocation().getChunk().removePluginChunkTicket(wildPlugin);
    } else if (ticketsByChunk.get(event.getLocation().getChunk()).isEmpty()) {
      ticketsByChunk.remove(event.getLocation().getChunk());
    }
    LinkedList<Location> locations = wildManager.getLocations(event.getLocation().getWorld());
    if(locations.size() < maxNumKeepLoaded) {
      return;
    }
    Location loc = locations.get(maxNumKeepLoaded - 1);
    loc.getChunk().addPluginChunkTicket(wildPlugin);
    ticketsByChunk.putIfAbsent(loc.getChunk(), new HashSet<>());
    ticketsByChunk.get(loc.getChunk()).add(loc);
  }

  @EventHandler(priority = EventPriority.LOW)
  void onConfigReloaded(ConfigReloadedEvent event) {
    for(Map.Entry<World, LinkedList<Location>> entry : wildManager.getLocationsByWorld().entrySet()) {
      for(Location location : entry.getValue()) {
        location.getChunk().removePluginChunkTicket(wildPlugin);
      }
    }
    ticketsByChunk.clear();
    maxNumKeepLoaded = wildPlugin.getConfiguration().get(ConfigKey.PERF_KEEP_BUFFER_LOADED);
  }

  public int getNumberOfLocations(Chunk chunk) {
    return ticketsByChunk.getOrDefault(chunk, Collections.emptySet()).size();
  }
}
