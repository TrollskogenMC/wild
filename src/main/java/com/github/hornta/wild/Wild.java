package com.github.hornta.wild;

import com.github.hornta.*;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.config.ConfigType;
import com.github.hornta.wild.config.Configuration;
import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;
import com.github.hornta.wild.message.Translation;
import com.github.hornta.wild.message.Translations;
import com.wimbli.WorldBorder.WorldBorder;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.logging.Level;

public class Wild extends JavaPlugin {
  private static Wild instance;
  private WorldBorder worldBorder;
  private Carbon carbon;
  private Configuration configuration;
  private Translations translations;
  private Metrics metrics;

  public static Wild getInstance() {
    return instance;
  }

  public void onEnable() {
    if (instance == null) {
      instance = this;
    }

    metrics = new Metrics(this);

    worldBorder = (WorldBorder) Bukkit.getPluginManager().getPlugin("WorldBorder");

    configuration = new Configuration(this);
    configuration.add(ConfigKey.LANGUAGE, "language", ConfigType.STRING, "english");
    configuration.add(ConfigKey.COOLDOWN, "cooldown", ConfigType.INTEGER, 60);
    configuration.add(ConfigKey.TRIES, "tries", ConfigType.INTEGER, 10);
    configuration.add(ConfigKey.NO_BORDER_SIZE, "no_border_size", ConfigType.INTEGER, 5000);
    configuration.add(ConfigKey.USE_VANILLA_WORLD_BORDER, "use_vanilla_world_border", ConfigType.BOOLEAN, false);
    configuration.add(ConfigKey.IMMORTAL_DURATION_AFTER_TELEPORT, "immortal_duration_after_teleport", ConfigType.INTEGER, 5000);
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
    carbon.setMissingArgumentHandler((CommandSender sender, CarbonCommand command) -> {
      MessageManager.setValue("usage", command.getHelpText());
      MessageManager.sendMessage(sender, MessageKey.MISSING_ARGUMENTS);
    });

    carbon.handleValidation((ValidationResult result) -> {
      switch (result.getStatus()) {
        case ERR_OTHER:
          if (result.getArgument().getType() == CarbonArgumentType.WORLD_NORMAL) {
            MessageManager.setValue("world", result.getValue());
            MessageManager.sendMessage(result.getCommandSender(), MessageKey.WORLD_NOT_FOUND);
          } else if(result.getArgument().getType() == CarbonArgumentType.ONLINE_PLAYER) {
            MessageManager.setValue("player", result.getValue());
            MessageManager.sendMessage(result.getCommandSender(), MessageKey.PLAYER_NOT_FOUND);
          }
          break;
      }
    });

    CarbonArgument playerArg =
      new CarbonArgument.Builder("player")
        .setType(CarbonArgumentType.ONLINE_PLAYER)
        .setDefaultValue(Player.class, (CommandSender sender, String[] prevArgs) -> sender.getName())
        .requiresPermission("wild.player")
        .create();
    CarbonArgument worldArg =
      new CarbonArgument.Builder("world")
        .setType(CarbonArgumentType.WORLD_NORMAL)
        .dependsOn(playerArg)
        .setDefaultValue(Player.class, (CommandSender sender, String[] prevArgs) -> ((Player) sender).getWorld().getName())
        .setDefaultValue(CommandSender.class,(CommandSender sender, String[] prevArgs) -> {
          Player player = Bukkit.getPlayer(prevArgs[0]);
          return player.getWorld().getName();
        })
        .requiresPermission("wild.world")
        .create();

    WildCommand wildCommand = new WildCommand();
    Bukkit.getPluginManager().registerEvents(wildCommand, this);

    carbon
      .addCommand("wild")
      .withArgument(playerArg)
      .withArgument(worldArg)
      .withHandler(wildCommand)
      .requiresPermission("wild.wild");

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
