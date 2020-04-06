package com.github.hornta.wild.engine;

import com.github.hornta.carbon.message.MessageManager;
import com.github.hornta.wild.*;
import com.github.hornta.wild.events.*;
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
  private WildPlugin plugin;
  private List<WorldUnit> worldUnits;
  private Map<World, WorldUnit> worldUnitsByWorld;
  private LinkedList<PlayerSearch> currentlyLooking;
  private Map<UUID, Long> immortals;
  private HashMap<UUID, Long> playerCooldowns;
  private int bufferInterval;
  private BukkitTask bufferTask;
  private ForceLoadedSystem forceLoadedSystem;

  public WildManager(WildPlugin wildPlugin) {
    this.plugin = wildPlugin;
    worldUnits = new ArrayList<>();
    worldUnitsByWorld = new HashMap<>();
    currentlyLooking = new LinkedList<>();
    immortals = new HashMap<>();
    playerCooldowns = new HashMap<>();
    bufferInterval = wildPlugin.getConfiguration().get(ConfigKey.PERF_BUFFER_INTERVAL);
    for(World world : wildPlugin.getServer().getWorlds()) {
      acceptWorld(world);
    }
    bufferTask = new BufferLocationTask(this).runTaskTimer(wildPlugin, 20, bufferInterval);
    new ProcessQueueTask(this).runTaskTimer(wildPlugin, 20, 5);
    forceLoadedSystem = new ForceLoadedSystem(wildPlugin, this);
    Bukkit.getPluginManager().registerEvents(forceLoadedSystem, wildPlugin);
  }

  @EventHandler
  void onWorldLoad(WorldLoadEvent event) {
    acceptWorld(event.getWorld());
  }

  @EventHandler
  void onWorldUnload(WorldUnloadEvent event) {
    worldUnits.removeIf((WorldUnit worldUnit) -> event.getWorld() == worldUnit.getWorld());
    worldUnitsByWorld.remove(event.getWorld());
    currentlyLooking.removeIf((PlayerSearch search) -> search.getWorld().equals(event.getWorld()));
    WildPlugin.debug("Unloaded world %s.", event.getWorld().getName());
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
    if ((boolean) WildPlugin.getInstance().getConfiguration().get(ConfigKey.WILD_ON_FIRST_JOIN_ENABLED) && !event.getPlayer().hasPlayedBefore()) {
      WildPlugin.debug("First join of %s", event.getPlayer().getName());
      PreTeleportEvent preEvent = new PreTeleportEvent(TeleportCause.FIRST_JOIN, event.getPlayer());
      Bukkit.getPluginManager().callEvent(preEvent);
      if(preEvent.isCancelled()) {
        return;
      }

      if(preEvent.getOverrideLocation() != null) {
        Bukkit.getScheduler().runTaskLater(WildPlugin.getInstance(), () -> {
          event.getPlayer().teleport(preEvent.getOverrideLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
          event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);

          TeleportEvent teleportEvent = new TeleportEvent(preEvent.getOverrideLocation(), TeleportCause.FIRST_JOIN, event.getPlayer());
          Bukkit.getPluginManager().callEvent(teleportEvent);
        }, 1);
        return;
      }

      String worldTarget = WildPlugin.getInstance().getConfiguration().get(ConfigKey.WILD_ON_FIRST_JOIN_WORLD);
      World world = Util.getWorldFromTarget(worldTarget, event.getPlayer());
      PlayerSearch search = new PlayerSearch(event.getPlayer().getUniqueId(), world, TeleportCause.FIRST_JOIN);
      RequestLocationEvent request = new RequestLocationEvent(search);
      Bukkit.getPluginManager().callEvent(request);
    }
  }

  @EventHandler
  void onPlayerRespawn(PlayerRespawnEvent event) {
    if (WildPlugin.getInstance().getConfiguration().get(ConfigKey.WILD_ON_DEATH_ENABLED)) {
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

      String worldTarget = WildPlugin.getInstance().getConfiguration().get(ConfigKey.WILD_ON_DEATH_WORLD);
      World world = Util.getWorldFromTarget(worldTarget, event.getPlayer());
      PlayerSearch search = new PlayerSearch(event.getPlayer().getUniqueId(), world, TeleportCause.RESPAWN);
      RequestLocationEvent request = new RequestLocationEvent(search);
      Bukkit.getPluginManager().callEvent(request);
    }
  }

  @EventHandler
  void onConfigReloaded(ConfigReloadedEvent event) {
    worldUnits.clear();
    worldUnitsByWorld.clear();
    playerCooldowns.clear();
    currentlyLooking.clear();

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
    WildPlugin.debug("%s request location caused by %s", Bukkit.getPlayer(event.getSearch().getUuid()).getName(), event.getSearch().getCause());
    for(int i = 0; i < currentlyLooking.size(); ++i) {
      if(currentlyLooking.get(i).getUuid().equals(event.getSearch().getUuid())) {
        currentlyLooking.set(i, event.getSearch());
        WildPlugin.debug("%s is already looking for a location. Replacing PlayerSearch..", Bukkit.getPlayer(event.getSearch().getUuid()).getName());
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
    WildPlugin.debug("Location found for player %s at %s caused by %s", player.getName(), player.getLocation(), event.getSearch().getCause());

    if (event.getSearch().getFee() > 0) {
      EconomyResponse response = WildPlugin.getInstance().getEconomy().withdrawPlayer(player, event.getSearch().getFee());
      if (response.type == EconomyResponse.ResponseType.SUCCESS) {
        MessageManager.setValue("amount", WildPlugin.getInstance().getEconomy().format(event.getSearch().getFee()));
        MessageManager.sendMessage(player, MessageKey.CHARGE_SUCCESS);
      }
    }

    int immortal_duration = WildPlugin.getInstance().getConfiguration().get(ConfigKey.IMMORTAL_DURATION_AFTER_TELEPORT);
    if (immortal_duration > 0) {
      immortals.put(event.getSearch().getUuid(), System.currentTimeMillis() + immortal_duration);
    }

    PlayerTeleportEvent.TeleportCause teleportCause = PlayerTeleportEvent.TeleportCause.PLUGIN;
    if(event.getSearch().getCause() == TeleportCause.COMMAND) {
      teleportCause = PlayerTeleportEvent.TeleportCause.COMMAND;
    }

    int dropHeight = WildPlugin.getInstance().getConfiguration().get(ConfigKey.DROP_FROM_ABOVE_HEIGHT);
    Location actualTeleportLocation = event.getLocation();
    if(dropHeight > 0) {
      actualTeleportLocation.setY(actualTeleportLocation.getY() + dropHeight);
    }

    player.teleport(actualTeleportLocation, teleportCause);
    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    if(event.getSearch().getCause() == TeleportCause.COMMAND) {
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));
      playerCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (int) WildPlugin.getInstance().getConfiguration().get(ConfigKey.COOLDOWN) * 1000);
    }
    TeleportEvent teleportEvent = new TeleportEvent(actualTeleportLocation, TeleportCause.COMMAND, player);
    Bukkit.getPluginManager().callEvent(teleportEvent);
  }

  @EventHandler
  void onDropUnsafeLocation(DropUnsafeLocationEvent event) {
    WildPlugin.debug("Dropped unsafe location %s for player %s caused by %s", event.getLocation(), Bukkit.getPlayer(event.getSearch().getUuid()), event.getSearch().getCause());
    int maxTries = plugin.getConfiguration().get(ConfigKey.PERF_COMMAND_MAX_TRIES);
    if(event.getSearch().getTries() >= maxTries && event.getSearch().getCause() == TeleportCause.COMMAND) {
      WildPlugin.debug("Search cancelled too many tries %d", maxTries);
      currentlyLooking.poll();
      MessageManager.sendMessage(Bukkit.getPlayer(event.getSearch().getUuid()), MessageKey.WILD_NOT_FOUND);
    }
  }

  @EventHandler
  void onUnsafeLocationFound(UnsafeLocationFoundEvent event) {
    event.getWorldUnit().increaseUnsafeLookups();
    int unsafeLookups = event.getWorldUnit().getUnsafeLookups();
    if (unsafeLookups >= 50) {
       WildPlugin.getInstance().getLogger().warning(
         String.format(
           "The world `%s` has had more than 50 failed safe locations lookups in a row. Consider disabling this world.",
           event.getWorldUnit().getWorld().getName()
         )
       );
    }
  }

  private void acceptWorld(World world) {
    if(world.getEnvironment() != World.Environment.NORMAL) {
      WildPlugin.debug("Failed to accept world %s. World type is not NORMAL", world.getName());
      return;
    }

    List<String> disabledWorlds = WildPlugin.getInstance().getConfiguration().get(ConfigKey.DISABLED_WORLDS);
    for(String worldName : disabledWorlds) {
      if(worldName.equalsIgnoreCase(world.getName())) {
        WildPlugin.debug("Failed to accept world %s. World is disabled.", worldName);
        return;
      }
    }

    WildPlugin.debug("Accepting world %s", world.getName());

    WorldUnit worldUnit = new WorldUnit(world);
    worldUnits.add(worldUnit);
    worldUnitsByWorld.put(world, worldUnit);
  }

  public List<WorldUnit> getWorldUnits() {
    return worldUnits;
  }

  public boolean containsWorldUnit(WorldUnit worldUnit) {
    return worldUnitsByWorld.containsValue(worldUnit);
  }

  public WorldUnit getWorldUnitByWorld(World world) {
    return worldUnitsByWorld.get(world);
  }

  public Queue<PlayerSearch> getCurrentlyLooking() {
    return currentlyLooking;
  }

  public WildPlugin getPlugin() {
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

  public ForceLoadedSystem getForceLoadedSystem() {
    return forceLoadedSystem;
  }
}
