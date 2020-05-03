package com.github.hornta.wild.events;

import com.github.hornta.wild.PlayerSearch;
import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DropUnsafeLocationEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private final PlayerSearch search;
  private final Location location;

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public DropUnsafeLocationEvent(PlayerSearch search, Location location) {
    this.search = search;
    this.location = location;
  }

  public PlayerSearch getSearch() {
    return search;
  }

  public Location getLocation() {
    return location;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
