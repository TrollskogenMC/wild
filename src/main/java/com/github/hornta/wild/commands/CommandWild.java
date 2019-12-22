package com.github.hornta.wild.commands;

import com.github.hornta.carbon.ICommandHandler;
import com.github.hornta.wild.*;
import com.github.hornta.wild.events.RequestLocationEvent;
import com.github.hornta.wild.events.TeleportEvent;
import com.github.hornta.wild.events.PreTeleportEvent;
import com.github.hornta.carbon.message.MessageManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;

public class WildCommand implements ICommandHandler, Listener {
  private WildManager wildManager;

  WildCommand(WildManager wildManager) {
    this.wildManager = wildManager;
  }

  public void handle(CommandSender commandSender, String[] args, int numTypedArgs) {
    Player player;
    boolean checkCooldown;
    World world;
    double payAmount = 0;
    Economy economy;
    boolean showActionBarMessage = false;

    if (args.length >= 1) {
      player = Bukkit.getPlayer(args[0]);
    } else {
      player = (Player) commandSender;
    }

    PreTeleportEvent preEvent = new PreTeleportEvent(TeleportCause.COMMAND, player);
    Bukkit.getPluginManager().callEvent(preEvent);
    if(preEvent.isCancelled()) {
      return;
    }

    if(preEvent.getOverrideLocation() != null) {
      player.teleport(preEvent.getOverrideLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
      player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(""));

      TeleportEvent teleportEvent = new TeleportEvent(preEvent.getOverrideLocation(), TeleportCause.COMMAND, player);
      Bukkit.getPluginManager().callEvent(teleportEvent);
      return;
    }

    if (numTypedArgs == 0 || (commandSender instanceof Player && player == commandSender)) {
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
      showActionBarMessage = true;
    } else {
      checkCooldown = false;
    }

    if (numTypedArgs == 0) {
      world = Util.getWorldFromTarget(Wild.getInstance().getConfiguration().get(ConfigKey.WILD_DEFAULT_WORLD), player);
    } else if (numTypedArgs == 1) {
      world = player.getWorld();
    } else {
      world = Bukkit.getWorld(args[1]);
    }

    if (payAmount > 0) {
      checkCooldown = false;
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

    if(checkCooldown) {
      long cooldown = wildManager.getCooldown(player);
      if(cooldown > 0) {
        Util.setTimeUnitValues();
        MessageManager.setValue("time_left", Util.getTimeLeft((int) cooldown / 1000));
        MessageManager.sendMessage(player, MessageKey.COOLDOWN);
        return;
      }
    }

    if (showActionBarMessage) {
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(MessageManager.getMessage(MessageKey.SEARCHING_ACTION_BAR)));
    }

    PlayerSearch search = new PlayerSearch(player.getUniqueId(), world, TeleportCause.COMMAND, payAmount);
    Bukkit.getPluginManager().callEvent(new RequestLocationEvent(search));
  }
}
