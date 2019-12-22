package com.github.hornta.wild.events;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BufferedLocationEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private boolean isCancelled;
  private Location location;

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public BufferedLocationEvent(Location location) {
    this.location = location;
  }

  public Location getLocation() {
    return location;
  }

  public void setCancelled(boolean cancelled) {
    isCancelled = cancelled;
  }

  public boolean isCancelled() {
    return isCancelled;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
