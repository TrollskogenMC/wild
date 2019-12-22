package com.github.hornta.wild.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PollLocationEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private Location location;

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public PollLocationEvent(Location location) {
    this.location = location;
  }

  public Location getLocation() {
    return location;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
