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

  public BufferLocationTask(WildManager wildManager) {
    this.wildManager = wildManager;
  }

  @Override
  public void run() {
    int maxStoredLocations = wildManager.getPlugin().getConfiguration().get(ConfigKey.PERF_BUFFER_SIZE);

    Optional<WorldUnit> optWorldUnit = wildManager
      .getWorldUnits()
      .stream()
      .filter((WorldUnit w) -> !(w.getLocations().size() >= maxStoredLocations))
      .min(Comparator.comparingInt(WorldUnit::getLookups));

    if(!optWorldUnit.isPresent()) {
      WildPlugin.debug("No more worlds to find locations in.");
      return;
    }

    WorldUnit worldUnit = optWorldUnit.get();
    World world = worldUnit.getWorld();

    worldUnit.increaseLookups();
    LookupData lookup = worldUnit.getLookupData();
    Location loc = lookup.getRandomLocation();

    PaperLib.getChunkAtAsync(loc).thenAccept((Chunk c) -> {
      Bukkit.getScheduler().runTaskLater(wildManager.getPlugin(), () -> {

        // we need to check if the world still exists in the engine
        // because it could have been removed after we received it
        // because we are inside async methods
        if (!wildManager.containsWorldUnit(worldUnit)) {
          WildPlugin.debug("World %s was removed.", world.getName());
          return;
        }

        int y = world.getHighestBlockYAt(loc.getBlockX(), loc.getBlockZ());
        loc.setY(y);
        WildPlugin.debug(
          "Found block %s at %s",
          loc.getBlock().getType().name(),
          loc
        );

        try {
          Util.isSafeStandBlock(loc.getBlock());
        } catch (Exception e) {
          WildPlugin.debug(
            "Block %s at %s is not safe to stand on. Reason: %s",
            loc.getBlock().getType().name(),
            loc.getBlock().getLocation(),
            e.getMessage()
          );
          UnsafeLocationFoundEvent event = new UnsafeLocationFoundEvent(worldUnit);
          Bukkit.getPluginManager().callEvent(event);
          return;
        }

        Location finalLocation = Util.findSpaceBelow(loc);
        WildPlugin.debug(
          "Save block %s at %s",
          finalLocation.getBlock().getType().name(),
          finalLocation
        );
        finalLocation.add(0, 1, 0);
        worldUnit.getLocations().add(finalLocation);
        worldUnit.resetUnsafeLookups();
        BufferedLocationEvent event = new BufferedLocationEvent(finalLocation);
        Bukkit.getPluginManager().callEvent(event);
      }, 0);
    });
  }
}
