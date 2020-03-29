package com.github.hornta.wild.commands;

import com.github.hornta.carbon.ICommandHandler;
import com.github.hornta.carbon.message.MessageManager;
import com.github.hornta.wild.ConfigKey;
import com.github.hornta.wild.MessageKey;
import com.github.hornta.wild.WildPlugin;
import com.github.hornta.wild.WorldUnit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.stream.Collectors;

public class CommandInfo implements ICommandHandler {
  public void handle(CommandSender commandSender, String[] strings, int typedArgs) {
    boolean vanillaBorder = WildPlugin.getInstance().getConfiguration().get(ConfigKey.USE_VANILLA_WORLD_BORDER);
    String vanillaBorderResult = vanillaBorder ? MessageManager.getMessage(MessageKey.YES) : MessageManager.getMessage(MessageKey.NO);
    MessageManager.setValue("cooldown", (int) WildPlugin.getInstance().getConfiguration().get(ConfigKey.COOLDOWN));
    MessageManager.setValue("vanilla_border", vanillaBorderResult);
    MessageManager.setValue(
      "buffered_locations",
      WildPlugin
        .getInstance()
        .getWildManager()
        .getWorldUnits()
        .stream()
        .map((WorldUnit worldUnit) -> worldUnit.getWorld().getName() + ": " + worldUnit.getLocations().size() + ", " + worldUnit.getLookups() + ", " + worldUnit.getUnsafeLookups())
        .collect(Collectors.joining(", "))
    );
    MessageManager.setValue(
      "locations_loaded",
      WildPlugin
        .getInstance()
        .getWildManager()
        .getWorldUnits()
        .stream()
        .map((WorldUnit::getWorld))
        .filter((World world) -> world.getPluginChunkTickets().get(WildPlugin.getInstance()) != null)
        .map((World world) -> world.getName() + ": " + world.getPluginChunkTickets().get(WildPlugin.getInstance()).size())
        .collect(Collectors.joining(", "))
    );
    MessageManager.sendMessage(commandSender, MessageKey.INFO);
  }
}
