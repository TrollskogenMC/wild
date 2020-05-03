package com.github.hornta.wild.events;

import com.github.hornta.wild.TeleportCause;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PreTeleportEvent extends Event implements Cancellable {
  private static final HandlerList handlers = new HandlerList();
  private boolean isCancelled;
  private final TeleportCause cause;
  private final Player player;
  private Location overrideLocation;

  public PreTeleportEvent(TeleportCause cause, Player player) {
    this.isCancelled = false;
    this.cause = cause;
    this.player = player;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }

  @Override
  public boolean isCancelled() {
    return isCancelled;
  }

  @Override
  public void setCancelled(boolean cancel) {
    isCancelled = cancel;
  }

  public Player getPlayer() {
    return player;
  }

  public TeleportCause getCause() {
    return cause;
  }

  public void setOverrideLocation(Location overrideLocation) {
    this.overrideLocation = overrideLocation;
  }

  public Location getOverrideLocation() {
    return overrideLocation;
  }
}
