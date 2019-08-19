package com.github.hornta.wild;

import com.github.hornta.carbon.ICommandHandler;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;
import com.github.hornta.wild.message.Translation;
import org.bukkit.command.CommandSender;

public class WildReload implements ICommandHandler {
  public void handle(CommandSender commandSender, String[] strings) {
    if (!Wild.getInstance().getConfiguration().reload()) {
      MessageManager.sendMessage(commandSender, MessageKey.CONFIGURATION_RELOAD_FAILED);
      return;
    }
    Translation translation = Wild.getInstance().getTranslations().createTranslation(Wild.getInstance().getConfiguration().get(ConfigKey.LANGUAGE));
    translation.load();
    MessageManager.setTranslation(translation);
    MessageManager.sendMessage(commandSender, MessageKey.CONFIGURATION_RELOADED);
  }
}
