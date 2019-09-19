package com.github.hornta.wild;

import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class RandomLocation {
  private final Player player;
  private final World world;
  private final double payAmount;
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
    for(String material : new String[] {
      "VOID_AIR",
      "WATER",
      "LAVA",
      "TRIPWIRE",
      "FIRE",
      "CACTUS",
      "SWEET_BERRY_BUSH",
      "CAMPFIRE",
      "COBWEB",
      "MAGMA_BLOCK",
      "ACACIA_PRESSURE_PLATE",
      "BIRCH_PRESSURE_PLATE",
      "JUNGLE_PRESSURE_PLATE",
      "OAK_PRESSURE_PLATE",
      "SPRUCE_PRESSURE_PLATE",
      "DARK_OAK_PRESSURE_PLATE",
      "STONE_PRESSURE_PLATE",
      "HEAVY_WEIGHTED_PRESSURE_PLATE",
      "LIGHT_WEIGHTED_PRESSURE_PLATE"
    }) {
      try {
        bannedMaterials.add(Material.valueOf(material));
      } catch (IllegalArgumentException e) {
        //
      }
    }

    findBelow = new HashSet<>();
    for(String material : new String[]{
      "ACACIA_LEAVES",
      "BIRCH_LEAVES",
      "DARK_OAK_LEAVES",
      "JUNGLE_LEAVES",
      "OAK_LEAVES",
      "SPRUCE_LEAVES",

      "ACACIA_LOG",
      "BIRCH_LOG",
      "DARK_OAK_LOG",
      "JUNGLE_LOG",
      "OAK_LOG",
      "SPRUCE_LOG",

      "ACACIA_WOOD",
      "BIRCH_WOOD",
      "DARK_OAK_WOOD",
      "JUNGLE_WOOD",
      "OAK_WOOD",
      "SPRUCE_WOOD",

      "STRIPPED_ACACIA_LOG",
      "STRIPPED_BIRCH_LOG",
      "STRIPPED_DARK_OAK_LOG",
      "STRIPPED_JUNGLE_LOG",
      "STRIPPED_OAK_LOG",
      "STRIPPED_SPRUCE_LOG",

      "STRIPPED_ACACIA_WOOD",
      "STRIPPED_BIRCH_WOOD",
      "STRIPPED_DARK_OAK_WOOD",
      "STRIPPED_JUNGLE_WOOD",
      "STRIPPED_OAK_WOOD",
      "STRIPPED_SPRUCE_WOOD"
    }) {
      try {
        findBelow.add(Material.valueOf(material));
      } catch (IllegalArgumentException e) {
        //
      }
    }
  }

  public RandomLocation(Player player, World world, double payAmount) {
    this.player = player;
    this.world = world;
    this.payAmount = payAmount;

    if (Wild.getInstance().getWorldBorder() == null) {
      border = null;
    } else {
      border = Config.getBorders().getOrDefault(world.getName(), null);
    }

    if (border != null) {
      centerX = (int) Math.floor(border.getX());
      centerZ = (int) Math.floor(border.getZ());
      radiusX = border.getRadiusX();
      radiusZ = border.getRadiusZ();
      if(border.getShape() != null) {
        isRound = border.getShape();
      } else {
        isRound = false;
      }
    } else if (Wild.getInstance().getConfiguration().get(ConfigKey.USE_VANILLA_WORLD_BORDER)) {
      centerX = world.getWorldBorder().getCenter().getBlockX();
      centerZ = world.getWorldBorder().getCenter().getBlockZ();
      radiusX = (int) Math.ceil(world.getWorldBorder().getSize() / 2);
      radiusZ = (int) Math.ceil(world.getWorldBorder().getSize() / 2);
      isRound = false;
    } else {
      centerX = world.getSpawnLocation().getBlockX();
      centerZ = world.getSpawnLocation().getBlockZ();
      radiusX = Wild.getInstance().getConfiguration().get(ConfigKey.NO_BORDER_SIZE);
      radiusZ = Wild.getInstance().getConfiguration().get(ConfigKey.NO_BORDER_SIZE);
      isRound = false;
    }
  }

  public double getPayAmount() {
    return payAmount;
  }

  public void findLocation(Consumer<Location> callback) {
    Task task = new Task(Wild.getInstance().getConfiguration().get(ConfigKey.TRIES), callback);
    task.runTaskTimer(Wild.getInstance(), 0, 2L);
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

  private class Task extends BukkitRunnable {
    private int maxIterations;
    private int currentIteration = 0;
    private Consumer<Location> callback;

    Task(int maxIterations, Consumer<Location> callback) {
      this.maxIterations = maxIterations;
      this.callback = callback;
    }

    @Override
    public void run() {
      currentIteration += 1;
      if (currentIteration == maxIterations) {
        this.cancel();
        callback.accept(null);
      }

      double randX;
      double randZ;

      Location loc;
      if (isRound) {
        double a = random.nextDouble() * 2 * Math.PI;
        double r = Math.min(radiusX, radiusZ) * Math.sqrt(random.nextDouble());
        randX = r * Math.cos(a) + centerX;
        randZ = r * Math.sin(a) + centerZ;
      } else {
        randX = Util.randInt(
          centerX - radiusX + 1,
          centerX + radiusX - 1
        );
        randZ = Util.randInt(
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
        return;
      }

      if (!world.getWorldBorder().isInside(loc)) {
        return;
      }

      if (
        player.getLocation().getBlockX() == loc.getBlockX() &&
          player.getLocation().getBlockZ() == loc.getBlockZ()
      ) {
        return;
      }

      if (!safeStandBlock(loc.getBlock())) {
        return;
      }

      callback.accept(findSpaceBelow(loc));
      this.cancel();
    }
  }
}
