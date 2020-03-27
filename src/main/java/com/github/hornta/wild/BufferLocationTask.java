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
import org.bukkit.util.Vector;

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
      .min(Comparator.comparingInt((WorldUnit wu) -> wu.getLookups()));

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

    double randX;
    double randZ;

    Location loc;
    if (lookup.isRound()) {
      double a = random.nextDouble() * 2 * Math.PI;
      double r = Math.min(lookup.getRadiusX(), lookup.getRadiusZ()) * Math.sqrt(random.nextDouble());
      randX = r * Math.cos(a) + lookup.getCenterX();
      randZ = r * Math.sin(a) + lookup.getCenterZ();
    } else {
      randX = Util.randInt(
        lookup.getCenterX() - lookup.getRadiusX() + 1,
        lookup.getCenterX() + lookup.getRadiusX() - 1
      );
      randZ = Util.randInt(
        lookup.getCenterZ() - lookup.getRadiusZ() + 1,
        lookup.getCenterZ() + lookup.getRadiusZ() - 1
      );
    }

    loc = new Location(world, (int) randX, 0, (int) randZ).add(new Vector(0.5, 0, 0.5));

    PaperLib.getChunkAtAsync(loc).thenAccept((Chunk c) -> {
      Bukkit.getScheduler().runTaskLater(wildManager.getPlugin(), () -> {
        int y = world.getHighestBlockYAt((int) randX, (int) randZ);
        loc.setY(y);
        WildPlugin.debug("Found block %s at %s", loc.getBlock().getType().name(), loc);

        if (lookup.getWbBorderData() != null && !lookup.getWbBorderData().insideBorder(loc)) {
          UnsafeLocationFoundEvent event = new UnsafeLocationFoundEvent(worldUnit.get());
          Bukkit.getPluginManager().callEvent(event);
          WildPlugin.debug("Block is outside of WorldBorder border");
          return;
        }

        try {
          Util.isSafeStandBlock(loc.getBlock());
        } catch (Exception e) {
          WildPlugin.debug("Block %s at %s is not safe to stand on. Reason: %s", loc.getBlock().getType().name(), loc.getBlock().getLocation(), e.getMessage());
          UnsafeLocationFoundEvent event = new UnsafeLocationFoundEvent(worldUnit.get());
          Bukkit.getPluginManager().callEvent(event);
          return;
        }

        Location finalLocation = Util.findSpaceBelow(loc);
        if(finalLocation != null) {
          WildPlugin.debug("Save block %s at %s", loc.getBlock().getType().name(), loc);
          worldUnit.get().getLocations().offer(finalLocation);
          worldUnit.get().increaseSafeLookups();
          BufferedLocationEvent event = new BufferedLocationEvent(finalLocation);
          Bukkit.getPluginManager().callEvent(event);
        } else {
          WildPlugin.debug("Didn't find location after finding space below");
          UnsafeLocationFoundEvent event = new UnsafeLocationFoundEvent(worldUnit.get());
          Bukkit.getPluginManager().callEvent(event);
        }
      }, 0);
    });
  }
}
