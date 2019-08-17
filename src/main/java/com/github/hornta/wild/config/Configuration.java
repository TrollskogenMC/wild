package com.github.hornta.wild.config;

import com.github.hornta.wild.Wild;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;

public class Configuration {
  private JavaPlugin plugin;
  private org.bukkit.configuration.Configuration configuration;
  private Map<Enum, ConfigValue> keys = new LinkedHashMap<>();

  public Configuration(JavaPlugin plugin) {
    this.plugin = plugin;
  }

  public void add(Enum id, String path, ConfigType type, Object defaultValue) {
    init(id, path, type, defaultValue, null);
  }

  public void add(Enum id, String path, ConfigType type, Object defaultValue, Function<String, Object> converter) {
    init(id, path, type, defaultValue, converter);
  }

  private void init(Enum id, String path, ConfigType type, Object defaultValue, Function<String, Object> converter) {
    if (path == null || path.isEmpty()) {
      throw new Error("Path cannot be null or empty.");
    }

    for (char character : path.toCharArray()) {
      if (Character.getType(character) == Character.UPPERCASE_LETTER) {
        throw new Error("All characters in the path must be lowercase");
      }
    }

    if (type == null) {
      throw new Error("Type cannot be null");
    }

    if (keys.containsKey(id)) {
      throw new Error("Config value with id `" + id + "` is already set.");
    }

    //if (keys.containsKey(path)) {
    //  throw new Error("Config value with path `" + path + "` is already set.");
    //}

    keys.put(id, new ConfigValue(path, type, defaultValue, converter));
  }

  public boolean reload() {
    plugin.reloadConfig();
    configuration = plugin.getConfig();
    boolean result = validate();
    deleteUnusedValues();
    plugin.saveConfig();
    return result;
  }

  public <T> T get(Enum key) {
    if (!keys.containsKey(key)) {
      throw new Error("Cannot find ConfigValue for key `" + key.name() + "`");
    }
    ConfigValue value = keys.get(key);
    Object obj = configuration.get(value.getPath());
    Function<String, Object> converter = value.getConverter();
    if(converter != null) {
      return (T)converter.apply(((String)obj).toUpperCase(Locale.ENGLISH));
    }
    return (T)obj;
  }

  private void deleteUnusedValues() {
    // try and see if we can delete unused config values
    List<String> keys = new ArrayList<>(configuration.getKeys(true));

    // make sure we reverse the collections so that all leaves ends up first
    Collections.reverse(keys);

    // keys to actually check for being used (leaves)
    Set<String> checkKeys = new HashSet<>();

    for(String key : keys) {
      boolean hasSubstring = false;
      for(String checkKey : checkKeys) {
        if(checkKey.contains(key)) {
          hasSubstring = true;
          break;
        }
      }

      if(hasSubstring) {
        continue;
      }

      checkKeys.add(key);
    }

    for(String path : checkKeys) {
      tryDeletePathRecursively(path);
    }
  }

  private void tryDeletePathRecursively(String path) {
    if(hasPath(path)) {
      return;
    }
    configuration.set(path, null);
    Wild.getInstance().getLogger().log(Level.WARNING, "Deleted unused path `" + path + "`");
    int separatorIndex = path.lastIndexOf('.');
    if(separatorIndex != -1) {
      tryDeletePathRecursively(path.substring(0, separatorIndex));
    }
  }

  private boolean validate() {
    Set<String> errors = new HashSet<>();

    // store keys and values in order defined in ConfigKey so that when saving new keys they end up in order when saving the config.yml
    Map<String, Object> keyValues = new LinkedHashMap<>();

    boolean save = false;
    for (Map.Entry<Enum, ConfigValue> entry : keys.entrySet()) {
      // try and see if we can add missing config values to the config
      if (!configuration.contains(entry.getValue().getPath())) {
        Object value;
        if(entry.getValue().getDefaultValue().getClass().isEnum()) {
          value = ((Enum)entry.getValue().getDefaultValue()).name().toLowerCase(Locale.ENGLISH);
        } else {
          value = entry.getValue().getDefaultValue();
        }
        keyValues.put(entry.getValue().getPath(), value);
        save = true;
        plugin.getLogger().log(Level.INFO, "Added missing property `" + entry.getValue().getPath() + "` with value `" + value + "`");
        continue;
      }

      keyValues.put(entry.getValue().getPath(), configuration.get(entry.getValue().getPath()));

      // verify that the type in the config file is of the expected type
      boolean isType = entry.getValue().isExpectedType(configuration);

      if(!isType) {
        errors.add("Expected config path \"" + entry.getValue().getPath() + "\" to be of type \"" + entry.getValue().getType().toString() + "\"");
      }
    }

    if(save) {
      // delete everything currently in the config
      for(String key : configuration.getKeys(true)) {
        configuration.set(key, null);
      }

      for(Map.Entry<String, Object> entry : keyValues.entrySet()) {
        configuration.set(entry.getKey(), entry.getValue());
      }
    }

    if(!errors.isEmpty()) {
      plugin.getLogger().log(Level.SEVERE, "*** config.yml contains bad values ***");
      errors
        .stream()
        .map((String s) -> "*** " + s + " ***")
        .forEach(plugin.getLogger()::severe);
      return false;
    }

    return true;
  }

  private boolean hasPath(String path) {
    for (ConfigValue value : keys.values()) {
      if (value.getPath().equals(path)) {
        return true;
      }
    }
    return false;
  }
}
