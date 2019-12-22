package com.github.hornta.wild;

import com.github.hornta.wild.events.BufferedLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public class LocationTask extends BukkitRunnable {
  private final WildManager wildManager;
  private final Random random;

  LocationTask(WildManager wildManager) {
    this.wildManager = wildManager;
    this.random = new Random();
  }

  @Override
  public void run() {
    Optional<Map.Entry<World, List<Location>>> worldLocation = wildManager
      .getLocationsByWorld()
      .entrySet()
      .stream()
      .min(Comparator.comparingInt((Map.Entry<World, List<Location>> a) -> a.getValue().size()));
    if(!worldLocation.isPresent()) {
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

    loc = new Location(
      world,
      (int) randX,
      world.getHighestBlockYAt((int) randX, (int) randZ),
      (int) randZ
    ).add(new Vector(0.5, 0, 0.5));

    if (lookup.getWbBorderData() != null && !lookup.getWbBorderData().insideBorder(loc)) {
      return;
    }

    if (!world.getWorldBorder().isInside(loc)) {
      return;
    }

    if (!Util.isSafeStandBlock(loc.getBlock())) {
      return;
    }

    Location finalLocation = Util.findSpaceBelow(loc);
    if(finalLocation != null) {
      worldLocation.get().getValue().add(finalLocation);
      Bukkit.getPluginManager().callEvent(new BufferedLocation());
    }
  }
}
