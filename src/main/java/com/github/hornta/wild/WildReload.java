package com.github.hornta.wild;

import com.github.hornta.ICommandHandler;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;
import com.github.hornta.wild.message.Translation;
import org.bukkit.command.CommandSender;

public class WildReload implements ICommandHandler {
  public void handle(CommandSender commandSender, String[] strings) {
    Wild.getInstance().getConfiguration().reload();
    Translation translation = Wild.getInstance().getTranslations().createTranslation(Wild.getInstance().getConfiguration().get(ConfigKey.LANGUAGE));
    translation.load();
    MessageManager.setTranslation(translation);
    MessageManager.sendMessage(commandSender, MessageKey.CONFIGURATION_RELOADED);
  }
}
