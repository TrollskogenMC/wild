package com.github.hornta.wild;

import com.github.hornta.wild.message.MessageKey;
import com.github.hornta.wild.message.MessageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Util {
  private static final int SECONDS_IN_ONE_DAY = 86400;
  private static final int SECONDS_IN_ONE_HOUR = 3600;
  private static final int SECONDS_IN_ONE_MINUTE = 60;
  private static final int MAX_DURATION_UNITS = 2;
  private static final Random random = new Random();

  public static String getFilenameWithoutExtension(File file) {
    String filename = file.getName();
    int lastDotIndex = filename.lastIndexOf('.');

    if(lastDotIndex == -1) {
      return filename;
    }

    return filename.substring(0, lastDotIndex);
  }

  public static String getTimeLeft(int duration) {
    if(duration == 0) {
      return null;
    }

    long days = duration / SECONDS_IN_ONE_DAY;
    long hours = (duration % SECONDS_IN_ONE_DAY) / SECONDS_IN_ONE_HOUR;
    long minutes = ((duration % SECONDS_IN_ONE_DAY) % SECONDS_IN_ONE_HOUR) / SECONDS_IN_ONE_MINUTE;
    long seconds = (((duration % SECONDS_IN_ONE_DAY) % SECONDS_IN_ONE_HOUR) % SECONDS_IN_ONE_MINUTE);

    List<DurationUnit> units = new ArrayList<>();
    units.add(new DurationUnit(days, "<day>", "<days>"));
    units.add(new DurationUnit(hours, "<hour>", "<hours>"));
    units.add(new DurationUnit(minutes, "<minute>", "<minutes>"));
    units.add(new DurationUnit(seconds, "<second>", "<seconds>"));

    StringBuilder stringBuilder = new StringBuilder();

    int count = 0;

    for(DurationUnit unit : units) {
      long amount = unit.getAmount();

      // make sure that we never get a format like 1 hour, 4 seconds, e.g. no skipping unit
      if(count != 0 && amount == 0) {
        break;
      }

      if(amount == 0) {
        continue;
      }

      stringBuilder.append(amount);
      stringBuilder.append(" ");
      stringBuilder.append(unit.getNumerus());
      stringBuilder.append(" and ");

      count += 1;
      if(count == MAX_DURATION_UNITS) {
        break;
      }
    }

    String string = stringBuilder.toString();

    // remove "and" and spaces
    return string.substring(0, string.length() - 5);
  }

  public static void setTimeUnitValues() {
    MessageManager.setValue("second", MessageKey.TIME_UNIT_SECOND);
    MessageManager.setValue("seconds", MessageKey.TIME_UNIT_SECONDS);
    MessageManager.setValue("minute", MessageKey.TIME_UNIT_MINUTE);
    MessageManager.setValue("minutes", MessageKey.TIME_UNIT_MINUTES);
    MessageManager.setValue("hour", MessageKey.TIME_UNIT_HOURS);
    MessageManager.setValue("hours", MessageKey.TIME_UNIT_HOURS);
    MessageManager.setValue("day", MessageKey.TIME_UNIT_DAY);
    MessageManager.setValue("days", MessageKey.TIME_UNIT_DAYS);
  }

  public static int randInt(int min, int max) {
    return random.nextInt(max - min + 1) + min;
  }
}
