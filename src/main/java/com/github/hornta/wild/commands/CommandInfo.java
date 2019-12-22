package com.github.hornta.wild.commands;

import com.github.hornta.carbon.ICommandHandler;
import com.github.hornta.carbon.message.MessageManager;
import com.github.hornta.carbon.message.Translation;
import com.github.hornta.wild.ConfigKey;
import com.github.hornta.wild.MessageKey;
import com.github.hornta.wild.Wild;
import com.github.hornta.wild.events.ConfigReloadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandReload implements ICommandHandler {
  public void handle(CommandSender commandSender, String[] strings, int typedArgs) {
    try {
      Wild.getInstance().getConfiguration().reload();
    } catch (Exception e) {
      MessageManager.sendMessage(commandSender, MessageKey.CONFIGURATION_RELOAD_FAILED);
      return;
    }

    Bukkit.getPluginManager().callEvent(new ConfigReloadedEvent());

    Translation translation = Wild.getInstance().getTranslations().createTranslation(Wild.getInstance().getConfiguration().get(ConfigKey.LANGUAGE));
    MessageManager.getInstance().setPrimaryTranslation(translation);
    MessageManager.sendMessage(commandSender, MessageKey.CONFIGURATION_RELOADED);
  }
}
