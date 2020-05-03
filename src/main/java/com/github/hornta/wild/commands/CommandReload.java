package com.github.hornta.wild.commands;

import com.github.hornta.commando.ICommandHandler;
import com.github.hornta.messenger.MessageManager;
import com.github.hornta.messenger.Translation;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.MessageKey;
import com.github.hornta.wild.WildPlugin;
import com.github.hornta.wild.events.ConfigReloadedEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class CommandReload implements ICommandHandler {
  public void handle(CommandSender commandSender, String[] strings, int typedArgs) {
    WildPlugin.getInstance().getConfiguration().reload();
    Translation translation = WildPlugin.getInstance().getTranslations().createTranslation(WildPlugin.getInstance().getConfiguration().get(ConfigKey.LANGUAGE));
    MessageManager.getInstance().setTranslation(translation);
    MessageManager.sendMessage(commandSender, MessageKey.CONFIGURATION_RELOADED);
    Bukkit.getPluginManager().callEvent(new ConfigReloadedEvent());
  }
}
