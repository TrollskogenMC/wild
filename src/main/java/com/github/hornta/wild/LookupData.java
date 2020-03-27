package com.github.hornta.wild;

import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;
import org.bukkit.World;

public class LookupData {
  private boolean isRound;
  private BorderData wbBorderData;
  private int centerX;
  private int centerZ;
  private int radiusX;
  private int radiusZ;

  public LookupData(boolean isRound, BorderData wbBorderData, int centerX, int centerZ, int radiusX, int radiusZ) {
    this.isRound = isRound;
    this.wbBorderData = wbBorderData;
    this.centerX = centerX;
    this.centerZ = centerZ;
    this.radiusX = radiusX;
    this.radiusZ = radiusZ;
  }

  public boolean isRound() {
    return isRound;
  }

  public BorderData getWbBorderData() {
    return wbBorderData;
  }

  public int getCenterX() {
    return centerX;
  }

  public int getCenterZ() {
    return centerZ;
  }

  public int getRadiusZ() {
    return radiusZ;
  }

  public int getRadiusX() {
    return radiusX;
  }

  public static LookupData createLookup(World world) {
    boolean isRound;
    BorderData borderData = null;
    int centerX;
    int centerZ;
    int radiusX;
    int radiusZ;

    if (WildPlugin.getInstance().getWorldBorder() != null) {
      borderData = Config.getBorders().getOrDefault(world.getName(), null);
    }

    if (borderData != null) {
      centerX = (int) Math.floor(borderData.getX());
      centerZ = (int) Math.floor(borderData.getZ());
      radiusX = borderData.getRadiusX();
      radiusZ = borderData.getRadiusZ();
      if(borderData.getShape() != null) {
        isRound = borderData.getShape();
      } else {
        isRound = false;
      }
    } else if (WildPlugin.getInstance().getConfiguration().get(ConfigKey.USE_VANILLA_WORLD_BORDER)) {
      centerX = world.getWorldBorder().getCenter().getBlockX();
      centerZ = world.getWorldBorder().getCenter().getBlockZ();
      radiusX = (int) Math.ceil(world.getWorldBorder().getSize() / 2);
      radiusZ = (int) Math.ceil(world.getWorldBorder().getSize() / 2);
      isRound = false;
    } else {
      centerX = world.getSpawnLocation().getBlockX();
      centerZ = world.getSpawnLocation().getBlockZ();
      radiusX = WildPlugin.getInstance().getConfiguration().get(ConfigKey.NO_BORDER_SIZE);
      radiusZ = WildPlugin.getInstance().getConfiguration().get(ConfigKey.NO_BORDER_SIZE);
      isRound = false;
    }

    return new LookupData(
      isRound,
      borderData,
      centerX,
      centerZ,
      radiusX,
      radiusZ
    );
  }
}
