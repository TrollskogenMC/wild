package com.github.hornta.wild;

import com.github.hornta.commando.CarbonArgument;
import com.github.hornta.commando.CarbonArgumentType;
import com.github.hornta.commando.CarbonCommand;
import com.github.hornta.commando.Commando;
import com.github.hornta.commando.ICarbonArgument;
import com.github.hornta.commando.ValidationResult;
import com.github.hornta.messenger.MessageManager;
import com.github.hornta.messenger.MessagesBuilder;
import com.github.hornta.messenger.Translation;
import com.github.hornta.messenger.Translations;
import com.github.hornta.versioned_config.Configuration;
import com.github.hornta.versioned_config.ConfigurationBuilder;
import com.github.hornta.wild.commands.CommandInfo;
import com.github.hornta.wild.commands.CommandInfoLoaded;
import com.github.hornta.wild.commands.CommandWild;
import com.github.hornta.wild.commands.CommandReload;
import com.github.hornta.wild.config.ConfigKey;
import com.github.hornta.wild.config.InitialVersion;
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

import java.io.File;
import java.util.IllegalFormatConversionException;
import java.util.List;
import java.util.logging.Level;

public class WildPlugin extends JavaPlugin {
  private static WildPlugin instance;
  private WorldBorder worldBorder;
  private Commando commando;
  private Configuration<ConfigKey> configuration;
  private Translations translations;
  private Economy economy;
  private WildManager wildManager;

  public static WildPlugin getInstance() {
    return instance;
  }

  public void onEnable() {
    if (instance == null) {
      instance = this;
    }

    new Metrics(this, 7399);

    if (Bukkit.getPluginManager().isPluginEnabled("Vault")) {
      RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
      if (rsp != null) {
        economy = rsp.getProvider();
      }
    }
    worldBorder = (WorldBorder) Bukkit.getPluginManager().getPlugin("WorldBorder");

    setupConfig();
    setupMessages();
    wildManager = new WildManager(this);
    getServer().getPluginManager().registerEvents(wildManager, this);
    setupCommands();
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    return commando.handleCommand(sender, command, args);
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    return commando.handleAutoComplete(sender, command, args);
  }

  private void setupConfig() {
    File cfgFile = new File(getDataFolder(), "config.yml");
    ConfigurationBuilder<ConfigKey> cb = new ConfigurationBuilder<>(this, cfgFile);
    cb.addVersion(new InitialVersion());
    configuration = cb.run();
  }

  private void setupCommands() {
    commando = new Commando();
    commando.setNoPermissionHandler((CommandSender sender, CarbonCommand command) -> {
      MessageManager.sendMessage(sender, MessageKey.NO_PERMISSION);
    });
    commando.setMissingArgumentHandler((CommandSender sender, CarbonCommand command) -> {
      MessageManager.setValue("usage", command.getHelpText());
      MessageManager.sendMessage(sender, MessageKey.MISSING_ARGUMENTS);
    });

    commando.handleValidation((ValidationResult result) -> {
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

    ICarbonArgument playerArg =
      new CarbonArgument.Builder("player")
        .setType(CarbonArgumentType.ONLINE_PLAYER)
        .setDefaultValue(Player.class, (CommandSender sender, String[] prevArgs) -> sender.getName())
        .requiresPermission("wild.player")
        .create();
    ICarbonArgument worldArg =
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

    commando
      .addCommand("wild")
      .withArgument(playerArg)
      .withArgument(worldArg)
      .withHandler(new CommandWild(wildManager))
      .requiresPermission("wild.wild");

    commando
      .addCommand("wild reload")
      .withHandler(new CommandReload())
      .requiresPermission("wild.reload");

    commando
      .addCommand("wild info")
      .withHandler(new CommandInfo())
      .requiresPermission("wild.info");

    commando
      .addCommand("wild info loaded")
      .withHandler(new CommandInfoLoaded())
      .requiresPermission("wild.info.loaded");
  }

  private void setupMessages() {
    MessageManager messageManager = new MessagesBuilder()
      .add(MessageKey.CONFIGURATION_RELOADED, "reloaded_ok")
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
      .add(MessageKey.INFO_LOADED, "info_loaded")
      .add(MessageKey.INFO_LOADED_ITEM, "info_loaded_item")
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
    messageManager.setTranslation(translation);
  }

  public WorldBorder getWorldBorder() {
    return worldBorder;
  }

  public Configuration<ConfigKey> getConfiguration() {
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
    if(WildPlugin.getInstance().getConfiguration().get(ConfigKey.VERBOSE)) {
      try {
        WildPlugin.getInstance().getLogger().info(String.format(message, args));
      } catch (IllegalFormatConversionException e) {
        WildPlugin.getInstance().getLogger().log(Level.SEVERE, e.getMessage(), e);
      }
    }
  }
}
