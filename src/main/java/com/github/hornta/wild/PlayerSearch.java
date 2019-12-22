package com.github.hornta.wild;

import org.bukkit.World;

import java.util.UUID;

public class PlayerSearch {
  private final UUID uuid;
  private final World world;
  private final double fee;
  private final TeleportCause cause;
  private int tries;

  public PlayerSearch(UUID uuid, World world, TeleportCause cause, double fee) {
    this.uuid = uuid;
    this.world = world;
    this.cause = cause;
    this.fee = fee;
    this.tries = 0;
  }

  public PlayerSearch(UUID uuid, World world, TeleportCause cause) {
    this.uuid = uuid;
    this.world = world;
    this.cause = cause;
    this.fee = 0;
    this.tries = 0;
  }

  public World getWorld() {
    return world;
  }

  public UUID getUuid() {
    return uuid;
  }

  public TeleportCause getCause() {
    return cause;
  }

  public double getFee() {
    return fee;
  }

  public int getTries() {
    return tries;
  }

  public void incrementTries() {
    tries += 1;
  }
}
