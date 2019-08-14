package com.github.hornta.wild;

import com.github.hornta.Carbon;
import com.github.hornta.CarbonCommand;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.config.ConfigType;
import com.github.hornta.wild.config.Configuration;
import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;
import com.github.hornta.wild.message.Translation;
import com.github.hornta.wild.message.Translations;
import com.wimbli.WorldBorder.WorldBorder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class Wild extends JavaPlugin {
  private static Wild instance;
  private WorldBorder worldBorder;
  private Carbon carbon;
  private Configuration configuration;
  private Translations translations;

  public static Wild getInstance() {
    return instance;
  }

  public void onEnable() {
    if (instance == null) {
      instance = this;
    }

    worldBorder = (WorldBorder) Bukkit.getPluginManager().getPlugin("WorldBorder");

    configuration = new Configuration(this);
    configuration.add(ConfigKey.LANGUAGE, "language", ConfigType.STRING, "english");
    configuration.add(ConfigKey.COOLDOWN, "cooldown", ConfigType.INTEGER, 60);
    configuration.add(ConfigKey.TRIES, "tries", ConfigType.INTEGER, 10);
    configuration.reload();

    translations = new Translations(this);
    Translation translation = translations.createTranslation(configuration.get(ConfigKey.LANGUAGE));
    translation.load();
    MessageManager.setTranslation(translation);
    if (!configuration.get(ConfigKey.LANGUAGE).equals("english")) {
      Translation fallback = translations.createTranslation("english");
      fallback.load();
      MessageManager.setFallbackTranslation(fallback);
    }

    carbon = new Carbon();
    carbon.setNoPermissionHandler((CommandSender sender, CarbonCommand command) -> {
      MessageManager.sendMessage(sender, MessageKey.NO_PERMISSION);
    });

    carbon
      .addCommand("wild")
      .withHandler(new WildCommand())
      .requiresPermission("wild.wild")
      .preventConsoleCommandSender();

    carbon
      .addCommand("wild reload")
      .withHandler(new WildReload())
      .requiresPermission("wild.reload");
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    return carbon.handleCommand(sender, command, args);
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    return carbon.handleAutoComplete(sender, command, args);
  }

  public WorldBorder getWorldBorder() {
    return worldBorder;
  }

  public Configuration getConfiguration() {
    return configuration;
  }

  public Translations getTranslations() {
    return translations;
  }
}
