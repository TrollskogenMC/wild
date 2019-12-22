package com.github.hornta.wild.events;

import com.github.hornta.wild.TeleportCause;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TeleportEvent extends Event {
  private Location location;
  private TeleportCause cause;
  private Player player;
  private static final HandlerList handlers = new HandlerList();

  public static HandlerList getHandlerList() {
    return handlers;
  }

  public TeleportEvent(Location location, TeleportCause cause, Player player) {
    this.location = location;
    this.cause = cause;
    this.player = player;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  public Location getLocation() {
    return location;
  }

  public TeleportCause getCause() {
    return cause;
  }

  public Player getPlayer() {
    return player;
  }
}
