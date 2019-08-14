package com.github.hornta.wild;

import com.github.hornta.wild.config.ConfigKey;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RandomLocation {
  private final Player player;
  private final World world;
  private final BorderData border;
  private final boolean isRound;
  private final int centerX;
  private final int centerZ;
  private final int radiusX;
  private final int radiusZ;
  private Random random = new Random();
  private static Set<Material> bannedMaterials;
  private static Set<Material> findBelow;

  static {
    bannedMaterials = new HashSet<>();
    bannedMaterials.add(Material.WATER);
    bannedMaterials.add(Material.LAVA);
    bannedMaterials.add(Material.TRIPWIRE);
    bannedMaterials.add(Material.FIRE);
    bannedMaterials.add(Material.CACTUS);
    bannedMaterials.add(Material.SWEET_BERRY_BUSH);
    bannedMaterials.add(Material.CAMPFIRE);
    bannedMaterials.add(Material.COBWEB);
    bannedMaterials.add(Material.MAGMA_BLOCK);
    bannedMaterials.add(Material.ACACIA_PRESSURE_PLATE);
    bannedMaterials.add(Material.BIRCH_PRESSURE_PLATE);
    bannedMaterials.add(Material.JUNGLE_PRESSURE_PLATE);
    bannedMaterials.add(Material.OAK_PRESSURE_PLATE);
    bannedMaterials.add(Material.SPRUCE_PRESSURE_PLATE);
    bannedMaterials.add(Material.DARK_OAK_PRESSURE_PLATE);
    bannedMaterials.add(Material.STONE_PRESSURE_PLATE);
    bannedMaterials.add(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
    bannedMaterials.add(Material.LIGHT_WEIGHTED_PRESSURE_PLATE);

    findBelow = new HashSet<>();
    findBelow.add(Material.ACACIA_LEAVES);
    findBelow.add(Material.BIRCH_LEAVES);
    findBelow.add(Material.DARK_OAK_LEAVES);
    findBelow.add(Material.JUNGLE_LEAVES);
    findBelow.add(Material.OAK_LEAVES);
    findBelow.add(Material.SPRUCE_LEAVES);

    findBelow.add(Material.ACACIA_LOG);
    findBelow.add(Material.BIRCH_LOG);
    findBelow.add(Material.DARK_OAK_LOG);
    findBelow.add(Material.JUNGLE_LOG);
    findBelow.add(Material.OAK_LOG);
    findBelow.add(Material.SPRUCE_LOG);

    findBelow.add(Material.ACACIA_WOOD);
    findBelow.add(Material.BIRCH_WOOD);
    findBelow.add(Material.DARK_OAK_WOOD);
    findBelow.add(Material.JUNGLE_WOOD);
    findBelow.add(Material.OAK_WOOD);
    findBelow.add(Material.SPRUCE_WOOD);

    findBelow.add(Material.STRIPPED_ACACIA_LOG);
    findBelow.add(Material.STRIPPED_BIRCH_LOG);
    findBelow.add(Material.STRIPPED_DARK_OAK_LOG);
    findBelow.add(Material.STRIPPED_JUNGLE_LOG);
    findBelow.add(Material.STRIPPED_OAK_LOG);
    findBelow.add(Material.STRIPPED_SPRUCE_LOG);

    findBelow.add(Material.STRIPPED_ACACIA_WOOD);
    findBelow.add(Material.STRIPPED_BIRCH_WOOD);
    findBelow.add(Material.STRIPPED_DARK_OAK_WOOD);
    findBelow.add(Material.STRIPPED_JUNGLE_WOOD);
    findBelow.add(Material.STRIPPED_OAK_WOOD);
    findBelow.add(Material.STRIPPED_SPRUCE_WOOD);
  }

  public RandomLocation(Player player) {
    this.player = player;
    this.world = player.getWorld();

    if (Wild.getInstance().getWorldBorder() == null) {
      this.border = null;
    } else {
      this.border = Config.getBorders().getOrDefault(world.getName(), null);
    }

    if (this.border == null) {
      this.centerX = world.getSpawnLocation().getBlockX();
      this.centerZ = world.getSpawnLocation().getBlockZ();
      this.radiusX = 5000;
      this.radiusZ = 5000;
      this.isRound = false;
    } else {
      this.centerX = (int) Math.floor(border.getX());
      this.centerZ = (int) Math.floor(border.getZ());
      this.radiusX = border.getRadiusX();
      this.radiusZ = border.getRadiusZ();
      if(border.getShape() != null) {
        this.isRound = border.getShape();
      } else {
        this.isRound = false;
      }
    }
  }

  public Location findLocation() {
    double randX;
    double randZ;
    for (int i = 0; i < (int)Wild.getInstance().getConfiguration().get(ConfigKey.TRIES); ++i) {
      Location loc;
      if (isRound) {
        double a = random.nextDouble() * 2 * Math.PI;
        double r = Math.min(radiusX, radiusZ) * Math.sqrt(random.nextDouble());
        randX = r * Math.cos(a) + centerX;
        randZ = r * Math.sin(a) + centerZ;
      } else {
        randX = randInt(
          centerX - radiusX + 1,
          centerX + radiusX - 1
        );
        randZ = randInt(
          centerZ - radiusZ + 1,
          centerZ + radiusZ - 1
        );
      }

      loc = new Location(
        world,
        (int) randX,
        world.getHighestBlockYAt((int) randX, (int) randZ),
        (int) randZ
      ).add(new Vector(0.5, 0, 0.5));

      if (border != null && !border.insideBorder(loc)) {
        continue;
      }

      if (
        player.getLocation().getBlockX() == loc.getBlockX() &&
        player.getLocation().getBlockZ() == loc.getBlockZ()
      ) {
        continue;
      }

      if (!safeStandBlock(loc.getBlock())) {
        continue;
      }

      return findSpaceBelow(loc);
    }

    return null;
  }

  private Location findSpaceBelow(Location location) {
    Block currentCheck = world.getBlockAt(location);
    int airHeight = 0;

    for (int i = 0; i < 30; ++i) {
      if (findBelow.contains(currentCheck.getType())) {
        airHeight = 0;
      } else if (currentCheck.getType() == Material.AIR) {
        airHeight += 1;
      } else {
        return location;
      }

      Block aboveBlock = currentCheck;
      currentCheck = currentCheck.getRelative(BlockFace.DOWN);

      if (
        !findBelow.contains(currentCheck.getType()) &&
        currentCheck.getType().isSolid() &&
        airHeight >= 2 &&
        safeStandBlock(aboveBlock) &&
          aboveBlock.getLightFromSky() > 7
      ) {
        return aboveBlock.getLocation().add(new Vector(0.5, 0, 0.5));
      }
    }

    return location;
  }

  private boolean safeStandBlock(Block block) {
    return (
      !bannedMaterials.contains(block.getType()) &&
      !bannedMaterials.contains(block.getRelative(BlockFace.DOWN).getType())
    );
  }

  private int randInt(int min, int max) {
    return random.nextInt(max - min + 1) + min;
  }
}
