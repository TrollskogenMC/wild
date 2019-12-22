package com.github.hornta.wild;

import com.github.hornta.carbon.*;
import com.github.hornta.carbon.config.ConfigType;
import com.github.hornta.carbon.config.Configuration;
import com.github.hornta.carbon.config.ConfigurationBuilder;
import com.github.hornta.carbon.message.*;
import com.github.hornta.wild.commands.CommandInfo;
import com.github.hornta.wild.commands.CommandWild;
import com.github.hornta.wild.commands.CommandReload;
import com.github.hornta.wild.engine.WildManager;
import com.wimbli.WorldBorder.WorldBorder;
import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.logging.Level;

public class Wild extends JavaPlugin {
  private static Wild instance;
  private WorldBorder worldBorder;
  private Carbon carbon;
  private Configuration configuration;
  private Translations translations;
  private Economy economy;
  private WildManager wildManager;

  public static Wild getInstance() {
    return instance;
  }

  public void onEnable() {
    if (instance == null) {
      instance = this;
    }

    new Metrics(this);

    if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
      RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp != null) {
        economy = rsp.getProvider();
      }
    }
    worldBorder = (WorldBorder) Bukkit.getPluginManager().getPlugin("WorldBorder");

    try {
      configuration = new ConfigurationBuilder(this)
        .add(ConfigKey.LANGUAGE, "language", ConfigType.STRING, "english")
        .add(ConfigKey.COOLDOWN, "cooldown", ConfigType.INTEGER, 60)
        .add(ConfigKey.NO_BORDER_SIZE, "no_border_size", ConfigType.INTEGER, 5000)
        .add(ConfigKey.USE_VANILLA_WORLD_BORDER, "use_vanilla_world_border", ConfigType.BOOLEAN, false)
        .add(ConfigKey.IMMORTAL_DURATION_AFTER_TELEPORT, "immortal_duration_after_teleport", ConfigType.INTEGER, 5000)
        .add(ConfigKey.CHARGE_ENABLED, "charge.enabled", ConfigType.BOOLEAN, false)
        .add(ConfigKey.CHARGE_AMOUNT, "charge.amount", ConfigType.DOUBLE, 10)
        .add(ConfigKey.DISABLED_WORLDS, "disabled_worlds", ConfigType.LIST, Collections.emptyList())
        .add(ConfigKey.WILD_ON_FIRST_JOIN_ENABLED, "wild_on_first_join.enabled", ConfigType.BOOLEAN, false)
        .add(ConfigKey.WILD_ON_FIRST_JOIN_WORLD, "wild_on_first_join.world", ConfigType.STRING, "@same")
        .add(ConfigKey.WILD_ON_DEATH_ENABLED, "wild_on_death.enabled", ConfigType.BOOLEAN, false)
        .add(ConfigKey.WILD_ON_DEATH_WORLD, "wild_on_death.world", ConfigType.STRING, "@same")
        .add(ConfigKey.WILD_DEFAULT_WORLD, "default_world", ConfigType.STRING, "@same")
        .add(ConfigKey.PERF_BUFFER_SIZE, "performance.buffer_size", ConfigType.INTEGER, 256)
        .add(ConfigKey.PERF_BUFFER_INTERVAL, "performance.buffer_interval", ConfigType.INTEGER, 20 * 5)
        .add(ConfigKey.PERF_KEEP_BUFFER_LOADED, "performance.keep_buffer_loaded_size", ConfigType.INTEGER, 3)
        .add(ConfigKey.PERF_COMMAND_MAX_TRIES, "performance.command_max_tries", ConfigType.INTEGER, 2)
        .add(ConfigKey.VERBOSE, "verbose", ConfigType.BOOLEAN, false)
        .build();
    } catch (Exception e) {
      setEnabled(false);
      getLogger().log(Level.SEVERE, e.getMessage(), e);
      return;
    }

    MessageManager messageManager = new MessagesBuilder()
      .add(MessageKey.CONFIGURATION_RELOADED, "reloaded_ok")
      .add(MessageKey.CONFIGURATION_RELOAD_FAILED, "reloaded_fail")
      .add(MessageKey.WILD_NOT_FOUND, "wild_not_found")
      .add(MessageKey.COOLDOWN, "cooldown")
      .add(MessageKey.CHARGE, "charge")
      .add(MessageKey.CHARGE_SUCCESS, "charge_success")
      .add(MessageKey.NO_PERMISSION, "no_permission")
      .add(MessageKey.ONLY_OVERWORLD, "only_overworld")
      .add(MessageKey.FORGET_PLAYER, "forget_player")
      .add(MessageKey.WORLD_NOT_FOUND, "world_not_found")
      .add(MessageKey.PLAYER_NOT_FOUND, "player_not_found")
      .add(MessageKey.MISSING_ARGUMENTS, "missing_arguments")
      .add(MessageKey.WORLD_DISABLED, "world_disabled")
      .add(MessageKey.SEARCHING_ACTION_BAR, "searching")
      .add(MessageKey.INFO, "info")
      .add(MessageKey.TIME_UNIT_SECOND, "timeunit.second")
      .add(MessageKey.TIME_UNIT_SECONDS, "timeunit.seconds")
      .add(MessageKey.TIME_UNIT_MINUTE, "timeunit.minute")
      .add(MessageKey.TIME_UNIT_MINUTES, "timeunit.minutes")
      .add(MessageKey.TIME_UNIT_HOUR, "timeunit.hour")
      .add(MessageKey.TIME_UNIT_HOURS, "timeunit.hours")
      .add(MessageKey.TIME_UNIT_DAY, "timeunit.day")
      .add(MessageKey.TIME_UNIT_DAYS, "timeunit.days")
      .add(MessageKey.TIME_UNIT_NOW, "timeunit.now")
      .add(MessageKey.YES, "_yes")
      .add(MessageKey.NO, "_no")
      .build();

    translations = new Translations(this, messageManager);
    Translation translation = translations.createTranslation(configuration.get(ConfigKey.LANGUAGE));
    Translation fallbackTranslation = translations.createTranslation("english");
    messageManager.setTranslation(translation, fallbackTranslation);

    wildManager = new WildManager(this);
    getServer().getPluginManager().registerEvents(wildManager, this);

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
        .setDefaultValue(ConsoleCommandSender.class, (CommandSender sender, String[] prevArgs) -> {
          Player player = Bukkit.getPlayer(prevArgs[0]);
          return player.getWorld().getName();
        })
        .requiresPermission("wild.world")
        .create();

    carbon
      .addCommand("wild")
      .withArgument(playerArg)
      .withArgument(worldArg)
      .withHandler(new CommandWild(wildManager))
      .requiresPermission("wild.wild");

    carbon
      .addCommand("wild reload")
      .withHandler(new CommandReload())
      .requiresPermission("wild.reload");

    carbon
      .addCommand("wild info")
      .withHandler(new CommandInfo())
      .requiresPermission("wild.info");
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

  public WildManager getWildManager() {
    return wildManager;
  }

  public static void debug(String message, Object... args) {
    if(Wild.getInstance().getConfiguration().get(ConfigKey.VERBOSE)) {
      try {
        Wild.getInstance().getLogger().info(String.format(message, args));
      } catch (IllegalFormatConversionException e) {
        Wild.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }
}
