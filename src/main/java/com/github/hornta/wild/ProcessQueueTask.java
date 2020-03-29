package com.github.hornta.wild;

import com.github.hornta.wild.engine.WildManager;
import com.github.hornta.wild.events.DropUnsafeLocationEvent;
import com.github.hornta.wild.events.FoundLocationEvent;
import com.github.hornta.wild.events.PollLocationEvent;
import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


public class ProcessQueueTask extends BukkitRunnable {
  private final WildManager wildManager;

  public ProcessQueueTask(WildManager wildManager) {
    this.wildManager = wildManager;
  }

  @Override
  public void run() {
    PlayerSearch search = wildManager.getCurrentlyLooking().peek();
    if(search == null) {
      return;
    }

    WildPlugin.debug("%s is searching for a location in world %s caused by %s", Bukkit.getPlayer(search.getUuid()).getName(), search.getWorld().getName(), search.getCause());
    WorldUnit worldUnit = wildManager.getWorldUnitByWorld(search.getWorld());
    Location location = worldUnit.getLocations().poll();
    if(location == null) {
      WildPlugin.debug("Location not found... skipping");
      return;
    }

    Bukkit.getPluginManager().callEvent(new PollLocationEvent(location));

    PaperLib.getChunkAtAsync(location).thenAccept((Chunk c) -> {
      Bukkit.getScheduler().runTaskLater(wildManager.getPlugin(), () -> {
        Player player = Bukkit.getPlayer(search.getUuid());

        // this check is necessary because a player might have disconnected right after the search was picked above
        if (player == null) {
          return;
        }

        Block highestBlock = location.getWorld().getHighestBlockAt((int)location.getX(), (int)location.getZ());
        try {
          Util.isSafeStandBlock(highestBlock);
        } catch (Exception e) {
          WildPlugin.debug("Block %s at %s is no longer safe to stand on. Reason: %s", highestBlock.getType().name(), highestBlock.getLocation(), e.getMessage());
          search.incrementTries();
          DropUnsafeLocationEvent drop = new DropUnsafeLocationEvent(search, highestBlock.getLocation());
          Bukkit.getPluginManager().callEvent(drop);
          return;
        }
        wildManager.getCurrentlyLooking().poll();
        FoundLocationEvent event = new FoundLocationEvent(search, location);
        Bukkit.getPluginManager().callEvent(event);
      }, 0);
    });
  }
}
