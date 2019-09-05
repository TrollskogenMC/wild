package com.github.hornta.wild.events;

import com.github.hornta.wild.TeleportCause;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called before the search of a random location begins
 */
public class PreTeleportEvent extends Event implements Cancellable {
  private static final HandlerList handlers = new HandlerList();
  private boolean isCancelled;
  private TeleportCause cause;
  private Player player;

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
}
