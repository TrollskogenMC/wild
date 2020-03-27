package com.github.hornta.wild.events;

import com.github.hornta.wild.WorldUnit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class UnsafeLocationFoundEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private WorldUnit worldUnit;

  public UnsafeLocationFoundEvent(WorldUnit worldUnit) {
    this.worldUnit = worldUnit;
  }

  public WorldUnit getWorldUnit() {
    return worldUnit;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
