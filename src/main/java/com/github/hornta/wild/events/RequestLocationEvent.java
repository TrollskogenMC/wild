package com.github.hornta.wild.events;

import com.github.hornta.wild.PlayerSearch;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class RequestLocationEvent extends Event {
  private static final HandlerList handlers = new HandlerList();
  private PlayerSearch search;

  public RequestLocationEvent(PlayerSearch search) {
    this.search = search;
  }

  public PlayerSearch getSearch() {
    return search;
  }

  public static HandlerList getHandlerList() {
    return handlers;
  }

  @Override
  public HandlerList getHandlers() {
    return handlers;
  }
}
