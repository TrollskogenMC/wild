package com.github.hornta.wild;

import com.github.hornta.ICommandHandler;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.EventExecutor;

import java.util.*;

public class WildCommand implements ICommandHandler, Listener {
  private HashMap<UUID, Long> playerCooldowns = new HashMap<>();
  private HashMap<UUID, Long> immortals = new HashMap<>();

  public void handle(CommandSender commandSender, String[] args) {
    Player player;
    boolean checkCooldown;
    World world;

    if (args.length >= 1) {
      player = Bukkit.getPlayer(args[0]);
    } else {
      player = (Player) commandSender;
    }

    if (args.length == 0 || (commandSender instanceof Player && player == commandSender)) {
      checkCooldown = true;
    } else {
      checkCooldown = false;
    }

    if(args.length == 2) {
      world = Bukkit.getWorld(args[1]);
    } else {
      world = player.getWorld();
    }

    if(world.getEnvironment() != World.Environment.NORMAL) {
      MessageManager.sendMessage(commandSender, MessageKey.ONLY_OVERWORLD);
      return;
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
}
