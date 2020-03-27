package com.github.hornta.wild;

import com.github.hornta.wild.engine.WildManager;
import com.github.hornta.wild.events.BufferedLocationEvent;
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
    Optional<Map.Entry<World, LinkedList<Location>>> worldLocation = wildManager
      .getLocationsByWorld()
      .entrySet()
      .stream()
      .min(Comparator.comparingInt((Map.Entry<World, LinkedList<Location>> a) -> a.getValue().size()));
    if(!worldLocation.isPresent()) {
      WildPlugin.debug("Didn't find any suitable world.");
      return;
    }

    if(worldLocation.get().getValue().size() >= (int)wildManager.getPlugin().getConfiguration().get(ConfigKey.PERF_BUFFER_SIZE)) {
      WildPlugin.debug("Reached maximum buffer size");
      return;
    }

    World world = worldLocation.get().getKey();
    LookupData lookup = wildManager.getLookupDataByWorld().get(worldLocation.get().getKey());

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
          WildPlugin.debug("Block is outside of WorldBorder border");
          return;
        }

        try {
          Util.isSafeStandBlock(loc.getBlock());
        } catch (Exception e) {
          WildPlugin.debug("Block %s at %s is not safe to stand on. Reason: %s", loc.getBlock().getType().name(), loc.getBlock().getLocation(), e.getMessage());
          return;
        }

        Location finalLocation = Util.findSpaceBelow(loc);
        if(finalLocation != null) {
          WildPlugin.debug("Save block %s at %s", loc.getBlock().getType().name(), loc);
          worldLocation.get().getValue().offer(finalLocation);
          BufferedLocationEvent event = new BufferedLocationEvent(finalLocation);
          Bukkit.getPluginManager().callEvent(event);
        } else {
          WildPlugin.debug("Didn't find location after finding space below");
        }
      }, 0);
    });
  }
}
