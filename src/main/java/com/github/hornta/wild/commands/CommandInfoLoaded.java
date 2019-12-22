package com.github.hornta.wild.commands;

import com.github.hornta.carbon.ICommandHandler;
import com.github.hornta.carbon.message.MessageManager;
import com.github.hornta.wild.MessageKey;
import com.github.hornta.wild.WildPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class CommandInfoLoaded implements ICommandHandler {
  public void handle(CommandSender commandSender, String[] strings, int typedArgs) {
    List<String> items = new ArrayList<>();
    for(World world : Bukkit.getWorlds()) {
      Collection<Chunk> chunks = world.getPluginChunkTickets().get(WildPlugin.getInstance());
      if(chunks == null) {
        continue;
      }
      for(Chunk chunk : chunks) {
        MessageManager.setValue("world", chunk.getWorld().getName());
        MessageManager.setValue("x", chunk.getX());
        MessageManager.setValue("z", chunk.getZ());
        MessageManager.setValue("num_locations", WildPlugin.getInstance().getWildManager().getForceLoadedSystem().getNumberOfLocations(chunk));
        String item = MessageManager.getMessage(MessageKey.INFO_LOADED_ITEM);
        items.add(item);
      }
    }
    MessageManager.setValue("list", String.join("\n", items));
    MessageManager.sendMessage(commandSender, MessageKey.INFO_LOADED);
  }
}
