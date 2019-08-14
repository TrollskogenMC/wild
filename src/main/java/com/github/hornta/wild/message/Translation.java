package com.github.hornta.wild.message;

import com.github.hornta.wild.Wild;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

public class Translation {
  private File file;
  private String language;
  private Map<MessageKey, String> translations = new EnumMap<>(MessageKey.class);

  Translation(File file, String language) {
    this.file = file;
    this.language = language;
  }

  public String getLanguage() {
    return language;
  }

  public boolean load() {
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
    MessageKey[] messageKeys = MessageKey.values();

    Set<String> validationErrors = new HashSet<>();

    Set<String> yamlKeys = yaml.getKeys(true);
    for(String key : yamlKeys) {
      if(yaml.isConfigurationSection(key)) {
        continue;
      }

      if (!yaml.isString(key)) {
        validationErrors.add("Expected \"" + key + "\" in \"" + file.getName() + "\" to be of type string.");
      }
    }

    try {
      yaml.save(file);
    } catch (IOException ex) {
      Wild.getInstance().getLogger().log(Level.SEVERE, "Failed to save to " + file.getName(), ex);
    }

    if (!validationErrors.isEmpty()) {
      Wild.getInstance().getLogger().log(Level.SEVERE, "*** " + file.getName() + " has bad values ***");
      for(String string : validationErrors) {
        Wild.getInstance().getLogger().log(Level.SEVERE, "*** " + string + " ***");
      }
      return false;
    }

    for(MessageKey key : messageKeys) {
      if(!yaml.contains(key.getIdentifier())) {
        continue;
      }
      translations.put(key, yaml.getString(key.getIdentifier()));
    }

    return true;
  }

  public String getTranslation(MessageKey key) {
    return translations.get(key);
  }

  public boolean hasKey(MessageKey key) {
    return translations.containsKey(key);
  }
}
