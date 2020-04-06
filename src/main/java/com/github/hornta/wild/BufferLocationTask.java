package com.github.hornta.wild;

import com.github.hornta.wild.engine.WildManager;
import com.github.hornta.wild.events.BufferedLocationEvent;
import com.github.hornta.wild.events.UnsafeLocationFoundEvent;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class BufferLocationTask extends BukkitRunnable {
  private final WildManager wildManager;
  private final Random random;

  public BufferLocationTask(WildManager wildManager) {
    this.wildManager = wildManager;
    this.random = new Random();
  }

  @Override
  public void run() {
    Optional<WorldUnit> worldUnit = wildManager
      .getWorldUnits()
      .stream()
      .min(Comparator.comparingInt(WorldUnit::getLookups));

    if(!worldUnit.isPresent()) {
      WildPlugin.debug("Didn't find any suitable world.");
      return;
    }

    if(worldUnit.get().getLocations().size() >= (int)wildManager.getPlugin().getConfiguration().get(ConfigKey.PERF_BUFFER_SIZE)) {
      WildPlugin.debug("Reached maximum buffer size");
      return;
    }

    worldUnit.get().increaseLookups();

    World world = worldUnit.get().getWorld();
    LookupData lookup = worldUnit.get().getLookupData();
    Location loc = lookup.getRandomLocation();

    PaperLib.getChunkAtAsync(loc).thenAccept((Chunk c) -> {
      Bukkit.getScheduler().runTaskLater(wildManager.getPlugin(), () -> {
        int y = world.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
        loc.setY(y);
        WildPlugin.debug("Found block %s at %s", loc.getBlock().getType().name(), loc);

        try {
          Util.isSafeStandBlock(loc.getBlock());
        } catch (Exception e) {
          WildPlugin.debug("Block %s at %s is not safe to stand on. Reason: %s", loc.getBlock().getType().name(), loc.getBlock().getLocation(), e.getMessage());
          UnsafeLocationFoundEvent event = new UnsafeLocationFoundEvent(worldUnit.get());
          Bukkit.getPluginManager().callEvent(event);
          return;
        }

        Location finalLocation = Util.findSpaceBelow(loc);
        WildPlugin.debug("Save block %s at %s", finalLocation.getBlock().getType().name(), finalLocation);
        finalLocation.add(0, 1, 0);
        worldUnit.get().getLocations().offer(finalLocation);
        worldUnit.get().resetUnsafeLookups();
        BufferedLocationEvent event = new BufferedLocationEvent(finalLocation);
        Bukkit.getPluginManager().callEvent(event);
      }, 0);
    });
  }
}
