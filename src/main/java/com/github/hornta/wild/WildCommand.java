package com.github.hornta.wild;

import com.github.hornta.ICommandHandler;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.UUID;

public class WildCommand implements ICommandHandler {
  private HashMap<UUID, Long> playerCooldowns = new HashMap<>();

  public void handle(CommandSender commandSender, String[] strings) {
    Player player = (Player) commandSender;

    if(player.getWorld().getEnvironment() != World.Environment.NORMAL) {
      MessageManager.sendMessage(commandSender, MessageKey.ONLY_OVERWORLD);
      return;
    }

    long now = System.currentTimeMillis();
    if (playerCooldowns.containsKey(player.getUniqueId())) {
      long expire = playerCooldowns.get(player.getUniqueId());
      if (expire > now) {
        Util.setTimeUnitValues();
        MessageManager.setValue("time_left", Util.getTimeLeft((int) (expire - now) / 1000));
        MessageManager.sendMessage(player, MessageKey.COOLDOWN);
        return;
      }
    }

    RandomLocation randomLocation = new RandomLocation(player);
    Location loc = randomLocation.findLocation();
    if(loc != null) {
      player.teleport(loc);
      playerCooldowns.put(player.getUniqueId(), now + (int) Wild.getInstance().getConfiguration().get(ConfigKey.COOLDOWN) * 1000);
      player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
    } else {
      MessageManager.sendMessage(commandSender, MessageKey.WILD_NOT_FOUND);
    }
  }
}
