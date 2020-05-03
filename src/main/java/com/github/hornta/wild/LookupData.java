package com.github.hornta.wild;

import com.github.hornta.wild.config.ConfigKey;
import com.wimbli.WorldBorder.BorderData;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Random;

public class LookupData {
  private final World world;
  private final boolean isRound;
  private final int centerX;
  private final int centerZ;
  private final int radiusX;
  private final int radiusZ;
  private final Random random;

  public LookupData(World world, boolean isRound, int centerX, int centerZ, int radiusX, int radiusZ) {
    this.world = world;
    this.isRound = isRound;
    this.centerX = centerX;
    this.centerZ = centerZ;
    this.radiusX = radiusX;
    this.radiusZ = radiusZ;
    this.random = new Random();
  }

  public Location getRandomLocation() {
    double randX;
    double randZ;

    int cx = centerX;
    int cz = centerZ;
    int rx = radiusX;
    int rz = radiusZ;
    boolean checkRound = isRound;

    if(WildPlugin.getInstance().getWorldBorder() != null) {
      BorderData worldBorder = WildPlugin.getInstance().getWorldBorder().getWorldBorder(world.getName());
      if(worldBorder != null) {
        boolean isRound;

        if(worldBorder.getShape() == null) {
          // the default border in WorldBorder plugin is round
          isRound = true;
        } else {
          isRound = worldBorder.getShape();
        }

        boolean worldBorderContains =
          (
            isRound &&
            worldBorder.getRadiusX() <= radiusX &&
            worldBorder.getRadiusZ() <= radiusZ
          ) ||
          (
            !isRound &&
            worldBorder.getX() - worldBorder.getRadiusX() >= radiusX &&
            worldBorder.getX() + worldBorder.getRadiusX() <= radiusX &&
            worldBorder.getZ() - worldBorder.getRadiusZ() >= radiusZ &&
            worldBorder.getZ() + worldBorder.getRadiusZ() <= radiusZ
          );

        // if the WorldBorder border fits within the lookup area, then find location inside
        // to prevent locations found outside the border which is still guarded for
        if (worldBorderContains) {
          checkRound = isRound;
          cx = (int)worldBorder.getX();
          cz = (int)worldBorder.getZ();
          rx = worldBorder.getRadiusX();
          rz = worldBorder.getRadiusZ();
        }
      }
    }

    if (checkRound) {
      double a = random.nextDouble() * 2 * Math.PI;
      double r = Math.min(rx, rz) * Math.sqrt(random.nextDouble());
      randX = r * Math.cos(a) + cx;
      randZ = r * Math.sin(a) + cz;
    } else {
      randX = Util.randInt(
        cx - rx + 1,
        cx + rx - 1
      );
      randZ = Util.randInt(
        cz - rz + 1,
        cz + rz - 1
      );
    }

    return new Location(
      world,
      (int) randX,
      0,
      (int) randZ
    ).add(new Vector(0.5, 0, 0.5));
  }

  public static LookupData createLookup(World world) {
    boolean isRound = false;
    int centerX = world.getWorldBorder().getCenter().getBlockX();
    int centerZ = world.getWorldBorder().getCenter().getBlockZ();
    int radiusX;
    int radiusZ;

    if (WildPlugin.getInstance().getConfiguration().get(ConfigKey.USE_VANILLA_WORLD_BORDER)) {
      radiusX = (int) Math.ceil(world.getWorldBorder().getSize() / 2);
      radiusZ = (int) Math.ceil(world.getWorldBorder().getSize() / 2);
    } else {
      radiusX = WildPlugin.getInstance().getConfiguration().get(ConfigKey.NO_BORDER_SIZE);
      radiusZ = WildPlugin.getInstance().getConfiguration().get(ConfigKey.NO_BORDER_SIZE);
    }

    return new LookupData(
      world,
      isRound,
      centerX,
      centerZ,
      radiusX,
      radiusZ
    );
  }
}
