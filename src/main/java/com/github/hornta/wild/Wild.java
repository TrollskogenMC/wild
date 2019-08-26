package com.github.hornta.wild;

import com.github.hornta.carbon.*;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.config.ConfigType;
import com.github.hornta.wild.config.Configuration;
import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;
import com.github.hornta.wild.message.Translation;
import com.github.hornta.wild.message.Translations;
import com.wimbli.WorldBorder.WorldBorder;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

public class Wild extends JavaPlugin {
  private static Wild instance;
  private WorldBorder worldBorder;
  private Carbon carbon;
  private Configuration configuration;
  private Translations translations;
  private Economy economy;
  private Metrics metrics;

  public static Wild getInstance() {
    return instance;
  }

  public void onEnable() {
    if (instance == null) {
      instance = this;
    }

    metrics = new Metrics(this);

    if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
      RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp != null) {
        economy = rsp.getProvider();
      }
    }
    worldBorder = (WorldBorder) Bukkit.getPluginManager().getPlugin("WorldBorder");

    configuration = new Configuration(this);
    configuration.add(ConfigKey.LANGUAGE, "language", ConfigType.STRING, "english");
    configuration.add(ConfigKey.COOLDOWN, "cooldown", ConfigType.INTEGER, 60);
    configuration.add(ConfigKey.TRIES, "tries", ConfigType.INTEGER, 10);
    configuration.add(ConfigKey.NO_BORDER_SIZE, "no_border_size", ConfigType.INTEGER, 5000);
    configuration.add(ConfigKey.USE_VANILLA_WORLD_BORDER, "use_vanilla_world_border", ConfigType.BOOLEAN, false);
    configuration.add(ConfigKey.IMMORTAL_DURATION_AFTER_TELEPORT, "immortal_duration_after_teleport", ConfigType.INTEGER, 5000);
    configuration.add(ConfigKey.CHARGE_ENABLED, "charge.enabled", ConfigType.BOOLEAN, false);
    configuration.add(ConfigKey.CHARGE_AMOUNT, "charge.amount", ConfigType.DOUBLE, 10);
    configuration.add(ConfigKey.DISABLED_WORLDS, "disabled_worlds", ConfigType.LIST, Collections.emptyList());
    configuration.add(ConfigKey.WILD_ON_FIRST_JOIN_ENABLED, "wild_on_first_join.enabled", ConfigType.BOOLEAN, false);
    configuration.add(ConfigKey.WILD_ON_FIRST_JOIN_WORLD, "wild_on_first_join.world", ConfigType.STRING, "@same");
    configuration.add(ConfigKey.WILD_ON_DEATH_ENABLED, "wild_on_death.enabled", ConfigType.BOOLEAN, false);
    configuration.add(ConfigKey.WILD_ON_DEATH_WORLD, "wild_on_death.world", ConfigType.STRING, "@same");
    configuration.add(ConfigKey.WILD_DEFAULT_WORLD, "default_world", ConfigType.STRING, "@same");
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

  public Economy getEconomy() {
    return economy;
  }
}
