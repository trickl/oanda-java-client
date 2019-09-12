package com.trickl.oanda.text;

import com.trickl.model.oanda.instrument.CurrencyPair;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyPairFormat {
  private static final Pattern[] PARSE_PATTERNS =
      new Pattern[] {
        Pattern.compile("(?<buy>[A-Za-z]{3})(?<separator>_)?(?<sell>[A-Za-z]{3})"),
      };

  public static final String OANDA_FORMAT = "{0}_{1}";
  public static final String SIXCHAR_FORMAT = "{0}{1}";

  private CurrencyPairFormat() {}

  /**
   * Parse an fx instrument identifier.
   *
   * @param id The id to parse
   * @return A structured object
   * @throws ParseException If the identifier cannot be interpreted.
   */
  public static CurrencyPair parse(String id) throws ParseException {
    Matcher matcher = getMatchingMatcher(id).orElseThrow(() -> new ParseException(id, 0));
    Currency buyCurrency = Currency.getInstance(matcher.group("buy").toUpperCase());
    Currency sellCurrency = Currency.getInstance(matcher.group("sell").toUpperCase());
    return new CurrencyPair(buyCurrency, sellCurrency);
  }

  private static Optional<Matcher> getMatchingMatcher(String id) {
    for (Pattern pattern : PARSE_PATTERNS) {
      Matcher matcher = pattern.matcher(id);
      if (matcher.matches()) {
        return Optional.of(matcher);
      }
    }
    return Optional.empty();
  }

  /**
   * Format an FX instrument name.
   *
   * @param instrument instrument to format
   * @param format Format to use
   * @return Formatted string
   */
  public static String format(CurrencyPair instrument, String format) {
    MessageFormat messageFormat = new MessageFormat(format);
    return messageFormat.format(
        new Object[] {
          instrument.getBuyCurrency().getCurrencyCode(),
          instrument.getSellCurrency().getCurrencyCode()
        });
  }
}
