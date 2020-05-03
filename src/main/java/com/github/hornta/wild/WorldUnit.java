package com.github.hornta.wild;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedList;

public class WorldUnit {
  private final World world;
  private final LinkedList<Location> locations;
  private int lookups;
  private int unsafeLookups;
  private final LookupData lookupData;

  public WorldUnit(World world) {
    this.world = world;
    this.locations = new LinkedList<>();
    this.lookups = 0;
    this.unsafeLookups = 0;
    this.lookupData = LookupData.createLookup(world);
  }

  public World getWorld() {
    return world;
  }

  public LinkedList<Location> getLocations() {
    return locations;
  }

  public int getLookups() {
    return lookups;
  }

  public int getUnsafeLookups() {
    return unsafeLookups;
  }

  public void increaseLookups() {
    lookups += 1;
  }

  public void increaseUnsafeLookups() {
    unsafeLookups += 1;
  }

  public void resetUnsafeLookups() {
    unsafeLookups = 0;
  }

  public LookupData getLookupData() {
    return lookupData;
  }
}
