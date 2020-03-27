package com.github.hornta.wild;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.LinkedList;

public class WorldUnit {
  private World world;
  private LinkedList<Location> locations;
  private int lookups;
  private int safeLookups;
  private LookupData lookupData;

  public WorldUnit(World world) {
    this.world = world;
    this.locations = new LinkedList<>();
    this.lookups = 0;
    this.safeLookups = 0;
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

  public int getSafeLookups() {
    return safeLookups;
  }

  public void increaseLookups() {
    lookups += 1;
  }

  public void increaseSafeLookups() {
    safeLookups += 1;
  }

  public LookupData getLookupData() {
    return lookupData;
  }
}
