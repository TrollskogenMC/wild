package com.github.hornta.wild.message;

import java.util.HashSet;
import java.util.Set;

public enum MessageKey {
  CONFIGURATION_RELOADED("reloaded_ok"),
  WILD_NOT_FOUND("wild_not_found"),
  COOLDOWN("cooldown"),
  NO_PERMISSION("no_permission"),
  ONLY_OVERWORLD("only_overworld"),
  TIME_UNIT_SECOND("timeunit.second"),
  TIME_UNIT_SECONDS("timeunit.seconds"),
  TIME_UNIT_MINUTE("timeunit.minute"),
  TIME_UNIT_MINUTES("timeunit.minutes"),
  TIME_UNIT_HOUR("timeunit.hour"),
  TIME_UNIT_HOURS("timeunit.hours"),
  TIME_UNIT_DAY("timeunit.day"),
  TIME_UNIT_DAYS("timeunit.days"),
  TIME_UNIT_NOW("timeunit.now");

  private static final Set<String> identifiers = new HashSet<>();
  static {
    for (MessageKey key : MessageKey.values()) {
      if(key.getIdentifier() == null || key.getIdentifier().isEmpty()) {
        throw new Error("A message identifier can't be null or empty.");
      }

      for(char character : key.name().toCharArray()) {
        if(Character.getType(character) == Character.LOWERCASE_LETTER) {
          throw new Error("All characters in a message key must be uppercase");
        }
      }

      for(char character : key.getIdentifier().toCharArray()) {
        if(Character.getType(character) == Character.UPPERCASE_LETTER) {
          throw new Error("All characters in a message identifier must be lowercase. Found " + key.getIdentifier());
        }
      }

      if(identifiers.contains(key.getIdentifier())) {
        throw new Error("Duplicate identifier `" + key.getIdentifier() + "` found in MessageKey");
      }

      identifiers.add(key.getIdentifier());
    }
  }

  private String identifier;

  MessageKey(String identifier) {
    this.identifier = identifier;
  }

  public static boolean hasIdentifier(String identifier) {
    return identifiers.contains(identifier);
  }

  public String getIdentifier() {
    return identifier;
  }
}
