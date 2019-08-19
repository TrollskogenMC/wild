package com.github.hornta.wild;

import com.github.hornta.carbon.ICommandHandler;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class WildCommand implements ICommandHandler, Listener {
  private HashMap<UUID, Long> playerCooldowns = new HashMap<>();
  private HashMap<UUID, Long> immortals = new HashMap<>();

  public void handle(CommandSender commandSender, String[] args) {
    Player player;
    boolean checkCooldown;
    World world;
    double payAmount = 0;
    Economy economy = null;

    if (args.length >= 1) {
      player = Bukkit.getPlayer(args[0]);
    } else {
      player = (Player) commandSender;

      double amount = Wild.getInstance().getConfiguration().get(ConfigKey.CHARGE_AMOUNT);
      economy = Wild.getInstance().getEconomy();
      if (
        !player.hasPermission("wild.bypasscharge") &&
        economy != null &&
        (boolean)Wild.getInstance().getConfiguration().get(ConfigKey.CHARGE_ENABLED) &&
        amount != 0.0
      ) {
        if (economy.getBalance(player) < amount) {
          MessageManager.setValue("required", economy.format(amount));
          MessageManager.setValue("current", economy.format(economy.getBalance(player)));
          MessageManager.sendMessage(player, MessageKey.CHARGE);
          return;
        } else {
          payAmount = amount;
        }
      }
    }

    if (args.length == 0 || (commandSender instanceof Player && player == commandSender)) {
      checkCooldown = true;
    } else {
      checkCooldown = false;
    }

    if (payAmount > 0) {
      checkCooldown = false;
    }

    if (args.length == 2) {
      world = Bukkit.getWorld(args[1]);
    } else {
      world = player.getWorld();
    }

    if (world.getEnvironment() != World.Environment.NORMAL) {
      MessageManager.sendMessage(commandSender, MessageKey.ONLY_OVERWORLD);
      return;
    }

    if (args.length == 0) {
      List<String> disabledWorlds = Wild.getInstance().getConfiguration().get(ConfigKey.DISABLED_WORLDS);
      if (disabledWorlds.contains(world.getName())) {
        MessageManager.sendMessage(player, MessageKey.WORLD_DISABLED);
        return;
      }
    }

    long now = System.currentTimeMillis();
    if (checkCooldown && !player.hasPermission("wild.bypasscooldown") && playerCooldowns.containsKey(player.getUniqueId())) {
      long expire = playerCooldowns.get(player.getUniqueId());
      if (expire > now) {
        Util.setTimeUnitValues();
        MessageManager.setValue("time_left", Util.getTimeLeft((int) (expire - now) / 1000));
        MessageManager.sendMessage(player, MessageKey.COOLDOWN);
        return;
      }
    }

    RandomLocation randomLocation = new RandomLocation(player, world);
    Location loc = randomLocation.findLocation();
    if(loc != null) {
      if (payAmount > 0) {
        EconomyResponse response = economy.withdrawPlayer(player, payAmount);
        if (response.type == EconomyResponse.ResponseType.SUCCESS) {
          MessageManager.setValue("amount", economy.format(payAmount));
          MessageManager.sendMessage(player, MessageKey.CHARGE_SUCCESS);
        }
      }

      int immortal_duration = Wild.getInstance().getConfiguration().get(ConfigKey.IMMORTAL_DURATION_AFTER_TELEPORT);
      if (immortal_duration > 0) {
        immortals.put(player.getUniqueId(), System.currentTimeMillis() + immortal_duration);
      }
      player.teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
      playerCooldowns.put(player.getUniqueId(), now + (int) Wild.getInstance().getConfiguration().get(ConfigKey.COOLDOWN) * 1000);
      player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    } else {
      MessageManager.sendMessage(commandSender, MessageKey.WILD_NOT_FOUND);
    }
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
      String worldTarget = Wild.getInstance().getConfiguration().get(ConfigKey.WILD_ON_FIRST_JOIN_WORLD);
      World world = getWorldFromTarget(worldTarget, event.getPlayer());
      RandomLocation randomLocation = new RandomLocation(event.getPlayer(), world);
      Location loc = randomLocation.findLocation();
      if (loc != null) {
        Bukkit.getScheduler().runTaskLater(Wild.getInstance(), () -> {
          event.getPlayer().teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
          event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
        }, 1);
      }
    }
  }

  @EventHandler
  void onPlayerRespawn(PlayerRespawnEvent event) {
    if (Wild.getInstance().getConfiguration().get(ConfigKey.WILD_ON_DEATH_ENABLED)) {
      String worldTarget = Wild.getInstance().getConfiguration().get(ConfigKey.WILD_ON_DEATH_WORLD);
      World world = getWorldFromTarget(worldTarget, event.getPlayer());
      RandomLocation randomLocation = new RandomLocation(event.getPlayer(), world);
      Location loc = randomLocation.findLocation();
      if (loc != null) {
        event.setRespawnLocation(loc);
        event.getPlayer().teleport(loc, PlayerTeleportEvent.TeleportCause.COMMAND);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
      }
    }
  }

  private World getWorldFromTarget(String target, Player player) {
    World world;
    switch (target) {
      case "@same":
        world = player.getWorld();
        break;
      case "@random":
        List<World> worlds = Bukkit.getWorlds().stream().filter((World w) -> w.getEnvironment() == World.Environment.NORMAL).collect(Collectors.toList());
        if(worlds.isEmpty()) {
          world = player.getWorld();
        } else {
          world = worlds.get(Util.randInt(0, worlds.size() - 1));
        }
        break;
      default:
        world = Bukkit.getWorld(target);
    }

    if (world == null) {
      world = player.getWorld();
      Wild.getInstance().getLogger().log(Level.WARNING, "A world couldn't be found with target `" + target + "`");
    }

    return world;
  }
}
