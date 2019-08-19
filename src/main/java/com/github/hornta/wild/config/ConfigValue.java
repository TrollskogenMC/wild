package com.github.hornta.wild.config;

import org.bukkit.configuration.ConfigurationSection;

import java.util.function.Function;

public class ConfigValue {
  private String path;
  private ConfigType type;
  private Object defaultValue;
  private Function<String, Object> converter;

  public ConfigValue(String path, ConfigType type, Object defaultValue, Function<String, Object> converter) {
    this.path = path;
    this.type = type;
    this.defaultValue = defaultValue;
    this.converter = converter;
  }

  public ConfigType getType() {
    return type;
  }

  public Object getDefaultValue() {
    return defaultValue;
  }

  public String getPath() {
    return path;
  }

  public boolean isExpectedType(ConfigurationSection configurationSection) {
    boolean isExpectedType = false;

    switch (type) {
      case SET:
        isExpectedType = configurationSection.isSet(path);
        break;
      case LIST:
        isExpectedType = configurationSection.isList(path);
        break;
      case LONG:
        isExpectedType = configurationSection.isLong(path);
        break;
      case COLOR:
        isExpectedType = configurationSection.isColor(path);
        break;
      case STRING:
        isExpectedType = configurationSection.isString(path);
        break;
      case VECTOR:
        isExpectedType = configurationSection.isVector(path);
        break;
      case BOOLEAN:
        isExpectedType = configurationSection.isBoolean(path);
        break;
      case INTEGER:
        isExpectedType = configurationSection.isInt(path);
        break;
      case ITEM_STACK:
        isExpectedType = configurationSection.isItemStack(path);
        break;
      case OFFLINE_PLAYER:
        isExpectedType = configurationSection.isOfflinePlayer(path);
        break;
      case DOUBLE:
        isExpectedType = configurationSection.isDouble(path) || configurationSection.isInt(path);
      default:
    }

    return isExpectedType;
  }

  public Function<String, Object> getConverter() {
    return converter;
  }
}
