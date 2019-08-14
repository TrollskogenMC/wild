package com.github.hornta.wild.message;

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class StringReplacer {
  private StringReplacer() { }

  static String replace(String input, Pattern regex, Function<Matcher, String> callback) {
    StringBuffer resultString = new StringBuffer();
    Matcher regexMatcher = regex.matcher(input);
    boolean didFound = false;
    while (regexMatcher.find()) {
      didFound = true;
      String replaced = callback.apply(regexMatcher);

      // failed to replace a placeholder with a value
      if(replaced.equals(regexMatcher.group())) {
        didFound = false;
      }
      regexMatcher.appendReplacement(resultString, Matcher.quoteReplacement(replaced));
    }
    regexMatcher.appendTail(resultString);

    if(didFound) {
      return replace(resultString.toString(), regex, callback);
    }

    return resultString.toString();
  }
}
