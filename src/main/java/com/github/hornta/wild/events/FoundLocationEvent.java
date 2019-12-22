package com.github.hornta.wild.events;

import com.github.hornta.wild.PlayerSearch;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FoundLocationEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private PlayerSearch search;
  private Location location;
  private boolean isCancelled;

  public FoundLocationEvent(PlayerSearch search, Location location) {
    this.search = search;
    this.location = location;
  }

  public PlayerSearch getSearch() {
    return search;
  }

  public Location getLocation() {
    return location;
  }

  public boolean isCancelled() {
    return isCancelled;
  }

  public void setCancelled(boolean cancelled) {
    isCancelled = cancelled;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
