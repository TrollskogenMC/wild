package com.github.hornta.wild.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BufferedLocation extends Event {
  private static final HandlerList handlers = new HandlerList();
  private boolean isCancelled;

  public static HandlerList getHandlerList() {
    return handlers;
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
