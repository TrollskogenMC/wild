package com.github.hornta.wild;

import com.github.hornta.carbon.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Util {
  private static final int SECONDS_IN_ONE_DAY = 86400;
  private static final int SECONDS_IN_ONE_HOUR = 3600;
  private static final int SECONDS_IN_ONE_MINUTE = 60;
  private static final int MAX_DURATION_UNITS = 2;
  private static final Random random = new Random();
  private static final Set<Material> bannedMaterials;
  private static final Set<Material> findBelow;

  static {
    bannedMaterials = new HashSet<>();
    bannedMaterials.add(Material.VOID_AIR);
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
    bannedMaterials.add(Material.END_PORTAL);
    bannedMaterials.add(Material.ACACIA_TRAPDOOR);
    bannedMaterials.add(Material.BIRCH_TRAPDOOR);
    bannedMaterials.add(Material.DARK_OAK_TRAPDOOR);
    bannedMaterials.add(Material.IRON_TRAPDOOR);
    bannedMaterials.add(Material.JUNGLE_TRAPDOOR);
    bannedMaterials.add(Material.OAK_TRAPDOOR);
    bannedMaterials.add(Material.SPRUCE_TRAPDOOR);
    bannedMaterials.add(Material.MOVING_PISTON);
    bannedMaterials.add(Material.TNT);
    bannedMaterials.add(Material.SPAWNER);

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

  public static String getTimeLeft(int duration) {
    if(duration == 0) {
      return null;
    }

    long days = duration / SECONDS_IN_ONE_DAY;
    long hours = (duration % SECONDS_IN_ONE_DAY) / SECONDS_IN_ONE_HOUR;
    long minutes = ((duration % SECONDS_IN_ONE_DAY) % SECONDS_IN_ONE_HOUR) / SECONDS_IN_ONE_MINUTE;
    long seconds = (((duration % SECONDS_IN_ONE_DAY) % SECONDS_IN_ONE_HOUR) % SECONDS_IN_ONE_MINUTE);

    List<DurationUnit> units = new ArrayList<>();
    units.add(new DurationUnit(days, "<day>", "<days>"));
    units.add(new DurationUnit(hours, "<hour>", "<hours>"));
    units.add(new DurationUnit(minutes, "<minute>", "<minutes>"));
    units.add(new DurationUnit(seconds, "<second>", "<seconds>"));

    StringBuilder stringBuilder = new StringBuilder();

    int count = 0;

    for(DurationUnit unit : units) {
      long amount = unit.getAmount();

      // make sure that we never get a format like 1 hour, 4 seconds, e.g. no skipping unit
      if(count != 0 && amount == 0) {
        break;
      }

      if(amount == 0) {
        continue;
      }

      stringBuilder.append(amount);
      stringBuilder.append(" ");
      stringBuilder.append(unit.getNumerus());
      stringBuilder.append(" and ");

      count += 1;
      if(count == MAX_DURATION_UNITS) {
        break;
      }
    }

    String string = stringBuilder.toString();

    // remove "and" and spaces
    return string.substring(0, string.length() - 5);
  }

  public static void setTimeUnitValues() {
    MessageManager.setValue("second", MessageKey.TIME_UNIT_SECOND);
    MessageManager.setValue("seconds", MessageKey.TIME_UNIT_SECONDS);
    MessageManager.setValue("minute", MessageKey.TIME_UNIT_MINUTE);
    MessageManager.setValue("minutes", MessageKey.TIME_UNIT_MINUTES);
    MessageManager.setValue("hour", MessageKey.TIME_UNIT_HOURS);
    MessageManager.setValue("hours", MessageKey.TIME_UNIT_HOURS);
    MessageManager.setValue("day", MessageKey.TIME_UNIT_DAY);
    MessageManager.setValue("days", MessageKey.TIME_UNIT_DAYS);
  }

  public static int randInt(int min, int max) {
    return random.nextInt(max - min + 1) + min;
  }

  public static void isSafeStandBlock(Block block) throws Exception {
    Material current = block.getType();
    Material above = block.getRelative(BlockFace.UP).getType();

    boolean isInsideWorldBorder = block.getWorld().getWorldBorder().isInside(block.getLocation());
    boolean isSafe = !bannedMaterials.contains(current);
    boolean isSafeAbove = !bannedMaterials.contains(above);

    if(!isInsideWorldBorder) {
      throw new Exception("Is not inside the world border");
    }

    if(!isSafe) {
      throw new Exception(String.format("Block %s is not safe", current.name()));
    }

    if(!isSafeAbove) {
      throw new Exception(String.format("Above block %s is not safe", above.name()));
    }
  }

  public static Location findSpaceBelow(Location location) {
    int dropHeight = WildPlugin.getInstance().getConfiguration().get(ConfigKey.DROP_FROM_ABOVE_HEIGHT);

    // disable finding space below when dropHeight is not set to zero to reduce eventual bugs
    if(dropHeight > 0) {
      return location;
    }

    Block currentCheck = location.getWorld().getBlockAt(location);
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

      try {
        isSafeStandBlock(aboveBlock);
        if (
          !findBelow.contains(currentCheck.getType()) &&
            currentCheck.getType().isSolid() &&
            airHeight >= 2 &&
            aboveBlock.getLightFromSky() > 7
        ) {
          return currentCheck.getLocation().add(new Vector(0.5, 0, 0.5));
        }
      } catch (Exception e) {
        return location;
      }
    }

    return location;
  }

  public static World getWorldFromTarget(String target, Player player) {
    World world;
    switch (target) {
      case "@same":
        world = player.getWorld();
        break;
      case "@random":
        List<String> disabledWorlds = WildPlugin.getInstance().getConfiguration().get(ConfigKey.DISABLED_WORLDS);
        List<World> worlds = Bukkit.getWorlds().stream().filter((World w) -> w.getEnvironment() == World.Environment.NORMAL && !disabledWorlds.contains(w.getName())).collect(Collectors.toList());
        if(worlds.isEmpty()) {
          world = player.getWorld();
        } else {
          world = worlds.get(Util.randInt(0, worlds.size() - 1));
        }
        break;
      default:
        world = Bukkit.getWorld(target);
    }

    if (world == null) {
      world = player.getWorld();
      WildPlugin.getInstance().getLogger().log(Level.WARNING, "A world couldn't be found with target `" + target + "`");
    }

    return world;
  }
}
