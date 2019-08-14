package com.github.hornta.wild.message;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager {
  private static final Pattern placeholderPattern = Pattern.compile("<([a-z_]+)(?:\\|(.+))?>", Pattern.CASE_INSENSITIVE);
  private static Translation translation;
  private static Translation fallbackTranslation;
  private static Map<String, List<String>> placeholderValues = new HashMap<>();
  private static Map<String, MessageKey> placeholderKeys = new HashMap<>();

  public static void setTranslation(Translation translation) {
    MessageManager.translation = translation;
  }

  public static void setFallbackTranslation(Translation fallbackTranslation) {
    MessageManager.fallbackTranslation = fallbackTranslation;
  }

  static String transformPattern(String input) {
    return StringReplacer.replace(input, placeholderPattern, (Matcher m) -> {
      String placeholder = m.group(1);

      Map<PlaceholderOption, String> options = Collections.emptyMap();

      if (m.group(2) != null) {
        options = getPlaceholderOptions(m.group(2));
      }

      String delimiter = "";
      if (placeholderValues.containsKey(placeholder)) {
        if (options.containsKey(PlaceholderOption.DELIMITER) && options.get(PlaceholderOption.DELIMITER) != null) {
          delimiter = options.get(PlaceholderOption.DELIMITER);
        }
        return String.join(delimiter, placeholderValues.get(placeholder));
      } else if(placeholderKeys.containsKey(placeholder)) {
        return getTranslation(placeholderKeys.get(placeholder));
      } else {
        return m.group();
      }
    });
  }

  public static void sendMessage(CommandSender commandSender, MessageKey key) {
    String message = getTranslation(key);
    message = transformPlaceholders(message);
    commandSender.sendMessage(message);
  }

  private static String transformPlaceholders(String input) {
    String transformed = transformPattern(input);
    placeholderValues.clear();
    placeholderKeys.clear();

    return transformed;
  }

  public static void setValue(String key, MessageKey messageKey) {
    placeholderKeys.put(key, messageKey);
  }

  public static void setValue(String key, Object value) {
    if (value == null) {
      value = Collections.emptyList();
    }

    if(!(value instanceof Collection<?>)) {
      value = Collections.singletonList(value.toString());
    }

    placeholderValues.put(key.toLowerCase(Locale.ENGLISH), (List<String>) value);
  }

  public static void broadcast(MessageKey key) {
    String message = getTranslation(key);

    message = transformPlaceholders(message);

    Bukkit.broadcastMessage(message);
  }

  public static String getMessage(MessageKey key) {
    String message = getTranslation(key);
    return transformPlaceholders(message);
  }

  private static Map<PlaceholderOption, String> getPlaceholderOptions(String options) {
    options = options.trim();
    Map<PlaceholderOption, String> map = new EnumMap<>(PlaceholderOption.class);
    for (String option : options.split(",")) {
      option = option.trim();
      PlaceholderOption key;
      String value;
      if(option.contains(":")) {
        key = PlaceholderOption.fromString(option.substring(0, option.lastIndexOf(":")).trim());
        value = option.substring(option.lastIndexOf(":") + 1).trim();
      } else {
        key = PlaceholderOption.fromString(option);
        value = null;
      }

      if(key != null) {
        map.put(key, value);
      }
    }

    return map;
  }

  private static String getTranslation(MessageKey key) {
    if(translation.hasKey(key)) {
      return translation.getTranslation(key);
    } else if(fallbackTranslation != null) {
      return fallbackTranslation.getTranslation(key);
    }
    return key.name();
  }
}
