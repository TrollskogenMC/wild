package com.github.hornta.wild;

import com.github.hornta.carbon.message.MessageManager;
import com.github.hornta.wild.events.*;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class WildManager implements Listener {
  private Wild plugin;
  private Map<World, LinkedList<Location>> locationsByWorld;
  private Map<World, LookupData> lookupDataByWorld;
  private LinkedList<PlayerSearch> currentlyLooking;
  private Map<UUID, Long> immortals;
  private HashMap<UUID, Long> playerCooldowns;
  private int numKeepLoaded;
  private int bufferInterval;
  private BukkitTask bufferTask;

  WildManager(Wild plugin) {
    this.plugin = plugin;
    locationsByWorld = new HashMap<>();
    lookupDataByWorld = new HashMap<>();
    currentlyLooking = new LinkedList<>();
    immortals = new HashMap<>();
    playerCooldowns = new HashMap<>();
    numKeepLoaded = plugin.getConfiguration().get(ConfigKey.PERF_KEEP_BUFFER_LOADED);
    bufferInterval = plugin.getConfiguration().get(ConfigKey.PERF_BUFFER_INTERVAL);
    for(World world : plugin.getServer().getWorlds()) {
      acceptWorld(world);
    }
    bufferTask = new BufferLocationTask(this).runTaskTimer(plugin, 20, bufferInterval);
    new ProcessQueueTask(this).runTaskTimer(plugin, 20, 5);
  }

  @EventHandler
  void onWorldLoad(WorldLoadEvent event) {
    acceptWorld(event.getWorld());
  }

  @EventHandler
  void onWorldUnload(WorldUnloadEvent event) {
    locationsByWorld.remove(event.getWorld());
    lookupDataByWorld.remove(event.getWorld());
    currentlyLooking.removeIf((PlayerSearch search) -> search.getWorld().equals(event.getWorld()));
  }

  @EventHandler
  void onPlayerQuit(PlayerQuitEvent event) {
    currentlyLooking.removeIf((PlayerSearch search) -> search.getUuid().equals(event.getPlayer().getUniqueId()));
  }

  @EventHandler
  void onPlayerDeath(PlayerDeathEvent event) {
    currentlyLooking.removeIf((PlayerSearch search) -> search.getUuid().equals(event.getEntity().getUniqueId()));
  }

  @EventHandler
  void onPlayerDamage(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }

    Player player = (Player) event.getEntity();
    if (immortals.containsKey(player.getUniqueId())) {
      long expire = immortals.get(player.getUniqueId());
      if (System.currentTimeMillis() >= expire) {
        immortals.remove(player.getUniqueId());
      } else {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  void onPlayerJoin(PlayerJoinEvent event) {
    if ((boolean)Wild.getInstance().getConfiguration().get(ConfigKey.WILD_ON_FIRST_JOIN_ENABLED) && !event.getPlayer().hasPlayedBefore()) {
      Wild.debug("First join of %s", event.getPlayer().getName());
      PreTeleportEvent preEvent = new PreTeleportEvent(TeleportCause.FIRST_JOIN, event.getPlayer());
      Bukkit.getPluginManager().callEvent(preEvent);
      if(preEvent.isCancelled()) {
        return;
      }

      if(preEvent.getOverrideLocation() != null) {
        Bukkit.getScheduler().runTaskLater(Wild.getInstance(), () -> {
          event.getPlayer().teleport(preEvent.getOverrideLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
          event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);

          TeleportEvent teleportEvent = new TeleportEvent(preEvent.getOverrideLocation(), TeleportCause.FIRST_JOIN, event.getPlayer());
          Bukkit.getPluginManager().callEvent(teleportEvent);
        }, 1);
        return;
      }

      String worldTarget = Wild.getInstance().getConfiguration().get(ConfigKey.WILD_ON_FIRST_JOIN_WORLD);
      World world = Util.getWorldFromTarget(worldTarget, event.getPlayer());
      PlayerSearch search = new PlayerSearch(event.getPlayer().getUniqueId(), world, TeleportCause.FIRST_JOIN);
      RequestLocationEvent request = new RequestLocationEvent(search);
      Bukkit.getPluginManager().callEvent(request);
    }
  }

  @EventHandler
  void onPlayerRespawn(PlayerRespawnEvent event) {
    if (Wild.getInstance().getConfiguration().get(ConfigKey.WILD_ON_DEATH_ENABLED)) {
      PreTeleportEvent preEvent = new PreTeleportEvent(TeleportCause.RESPAWN, event.getPlayer());
      Bukkit.getPluginManager().callEvent(preEvent);
      if(preEvent.isCancelled()) {
        return;
      }

      if(preEvent.getOverrideLocation() != null) {
        event.setRespawnLocation(preEvent.getOverrideLocation());
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        TeleportEvent teleportEvent = new TeleportEvent(preEvent.getOverrideLocation(), TeleportCause.RESPAWN, event.getPlayer());
        Bukkit.getPluginManager().callEvent(teleportEvent);
        return;
      }

      String worldTarget = Wild.getInstance().getConfiguration().get(ConfigKey.WILD_ON_DEATH_WORLD);
      World world = Util.getWorldFromTarget(worldTarget, event.getPlayer());
      PlayerSearch search = new PlayerSearch(event.getPlayer().getUniqueId(), world, TeleportCause.RESPAWN);
      RequestLocationEvent request = new RequestLocationEvent(search);
      Bukkit.getPluginManager().callEvent(request);
    }
  }

  @EventHandler
  void onConfigReloaded(ConfigReloadedEvent event) {
    for(Map.Entry<World, LinkedList<Location>> entry : locationsByWorld.entrySet()) {
      for(Location location : entry.getValue()) {
        location.getChunk().removePluginChunkTicket(plugin);
      }
    }
    locationsByWorld.clear();
    lookupDataByWorld.clear();

    int newBufferInterval = plugin.getConfiguration().get(ConfigKey.PERF_BUFFER_INTERVAL);
    if(bufferInterval != newBufferInterval) {
      bufferInterval = newBufferInterval;
      bufferTask.cancel();
      bufferTask = new BufferLocationTask(this).runTaskTimer(plugin, 20, bufferInterval);
    }

    for(World world : plugin.getServer().getWorlds()) {
      acceptWorld(world);
    }
  }

  @EventHandler
  void onRequestLocation(RequestLocationEvent event) {
    Wild.debug("%s request location caused by %s", Bukkit.getPlayer(event.getSearch().getUuid()).getName(), event.getSearch().getCause());
    for(PlayerSearch search : currentlyLooking) {
      if(search.getUuid().equals(event.getSearch().getUuid())) {
        Wild.debug("%s is already looking for a location, skipping...", Bukkit.getPlayer(event.getSearch().getUuid()).getName());
        return;
      }
    }
    if(
      event.getSearch().getCause() == TeleportCause.FIRST_JOIN ||
      event.getSearch().getCause() == TeleportCause.RESPAWN
    ) {
      currentlyLooking.addFirst(event.getSearch());
    } else {
      currentlyLooking.addLast(event.getSearch());
    }
  }

  @EventHandler(ignoreCancelled = true)
  void onFoundLocation(FoundLocationEvent event) {
    Player player = Bukkit.getPlayer(event.getSearch().getUuid());
    Wild.debug("Location found for player %s at %s caused by %s", player.getName(), player.getLocation(), event.getSearch().getCause());

    if (event.getSearch().getFee() > 0) {
      EconomyResponse response = Wild.getInstance().getEconomy().withdrawPlayer(player, event.getSearch().getFee());
      if (response.type == EconomyResponse.ResponseType.SUCCESS) {
        MessageManager.setValue("amount", Wild.getInstance().getEconomy().format(event.getSearch().getFee()));
        MessageManager.sendMessage(player, MessageKey.CHARGE_SUCCESS);
      }
    }

    int immortal_duration = Wild.getInstance().getConfiguration().get(ConfigKey.IMMORTAL_DURATION_AFTER_TELEPORT);
    if (immortal_duration > 0) {
      immortals.put(event.getSearch().getUuid(), System.currentTimeMillis() + immortal_duration);
    }

    PlayerTeleportEvent.TeleportCause teleportCause = PlayerTeleportEvent.TeleportCause.PLUGIN;
    if(event.getSearch().getCause() == TeleportCause.COMMAND) {
      teleportCause = PlayerTeleportEvent.TeleportCause.COMMAND;
    }
    player.teleport(event.getLocation(), teleportCause);
    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    if(event.getSearch().getCause() == TeleportCause.COMMAND) {
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
      playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (int) Wild.getInstance().getConfiguration().get(ConfigKey.COOLDOWN) * 1000);
    }
    TeleportEvent teleportEvent = new TeleportEvent(event.getLocation(), TeleportCause.COMMAND, player);
    Bukkit.getPluginManager().callEvent(teleportEvent);
  }

  @EventHandler(ignoreCancelled = true)
  void onBufferedLocation(BufferedLocationEvent event) {
    Queue<Location> locations = locationsByWorld.get(event.getLocation().getWorld());
    if(locations.size() <= numKeepLoaded) {
      Wild.debug("Keep chunk of location %s loaded", event.getLocation());
      event.getLocation().getChunk().addPluginChunkTicket(plugin);
    }
  }

  @EventHandler
  void onPollLocation(PollLocationEvent event) {
    event.getLocation().getChunk().removePluginChunkTicket(plugin);
    LinkedList<Location> locations = locationsByWorld.get(event.getLocation().getWorld());
    if(locations.size() < numKeepLoaded) {
      Wild.debug("All locations are already kept loaded.");
      return;
    }
    Location loc = locations.get(numKeepLoaded - 1);
    Wild.debug("Keep chunk of location %s loaded", loc);
    loc.getChunk().addPluginChunkTicket(plugin);
  }

  @EventHandler
  void onDropUnsafeLocation(DropUnsafeLocationEvent event) {
    Wild.debug("Dropped unsafe location %s for player %s caused by %s", event.getLocation(), Bukkit.getPlayer(event.getSearch().getUuid()), event.getSearch().getCause());
    int maxTries = plugin.getConfiguration().get(ConfigKey.PERF_COMMAND_MAX_TRIES);
    if(event.getSearch().getTries() >= maxTries && event.getSearch().getCause() == TeleportCause.COMMAND) {
      Wild.debug("Search cancelled too many tries %d", maxTries);
      currentlyLooking.poll();
      MessageManager.sendMessage(Bukkit.getPlayer(event.getSearch().getUuid()), MessageKey.WILD_NOT_FOUND);
    }
  }

  private void acceptWorld(World world) {
    if(world.getEnvironment() != World.Environment.NORMAL) {
      return;
    }

    List<String> disabledWorlds = Wild.getInstance().getConfiguration().get(ConfigKey.DISABLED_WORLDS);
    for(String worldName : disabledWorlds) {
      if(worldName.equalsIgnoreCase(world.getName())) {
        return;
      }
    }

    Wild.debug("Accepting world %s", world.getName());

    locationsByWorld.put(world, new LinkedList<>());

    boolean isRound;
    BorderData borderData = null;
    int centerX;
    int centerZ;
    int radiusX;
    int radiusZ;

    if (Wild.getInstance().getWorldBorder() != null) {
      borderData = Config.getBorders().getOrDefault(world.getName(), null);
    }

    if (borderData != null) {
      centerX = (int) Math.floor(borderData.getX());
      centerZ = (int) Math.floor(borderData.getZ());
      radiusX = borderData.getRadiusX();
      radiusZ = borderData.getRadiusZ();
      if(borderData.getShape() != null) {
        isRound = borderData.getShape();
      } else {
        isRound = false;
      }
    } else if (Wild.getInstance().getConfiguration().get(ConfigKey.USE_VANILLA_WORLD_BORDER)) {
      centerX = world.getWorldBorder().getCenter().getBlockX();
      centerZ = world.getWorldBorder().getCenter().getBlockZ();
      radiusX = (int) Math.ceil(world.getWorldBorder().getSize() / 2);
      radiusZ = (int) Math.ceil(world.getWorldBorder().getSize() / 2);
      isRound = false;
    } else {
      centerX = world.getSpawnLocation().getBlockX();
      centerZ = world.getSpawnLocation().getBlockZ();
      radiusX = Wild.getInstance().getConfiguration().get(ConfigKey.NO_BORDER_SIZE);
      radiusZ = Wild.getInstance().getConfiguration().get(ConfigKey.NO_BORDER_SIZE);
      isRound = false;
    }

    LookupData lookupData = new LookupData(
      isRound,
      borderData,
      centerX,
      centerZ,
      radiusX,
      radiusZ
    );
    lookupDataByWorld.put(world, lookupData);
  }

  public Map<World, LinkedList<Location>> getLocationsByWorld() {
    return locationsByWorld;
  }

  public Map<World, LookupData> getLookupDataByWorld() {
    return lookupDataByWorld;
  }

  public Queue<PlayerSearch> getCurrentlyLooking() {
    return currentlyLooking;
  }

  public Wild getPlugin() {
    return plugin;
  }

  public long getCooldown(Player player) {
    long now = System.currentTimeMillis();
    if (!player.hasPermission("wild.bypasscooldown") && playerCooldowns.containsKey(player.getUniqueId())) {
      long expire = playerCooldowns.get(player.getUniqueId());
      if (expire > now) {
        return expire - now;
      }
    }
    return 0;
  }
}
