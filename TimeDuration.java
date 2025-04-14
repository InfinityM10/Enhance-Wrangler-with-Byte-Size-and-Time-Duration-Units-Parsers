package io.cdap.wrangler.api.parser;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a time duration token with unit conversion functionality.
 */
public class TimeDuration extends Token {
  private static final Pattern TIME_DURATION_PATTERN = Pattern.compile("([-+]?\\d*\\.?\\d+)\\s*(ms|s|m|h|d|w)");

  private final double value;
  private final String unit;
  private final long nanoseconds; // Canonical unit

  /**
   * Constructor for TimeDuration token.
   *
   * @param value The text representation of time duration (e.g., "100ms").
   */
  public TimeDuration(String value) {
    super(value, TokenType.TIME_DURATION);
    Matcher matcher = TIME_DURATION_PATTERN.matcher(value);
    
    if (matcher.matches()) {
      this.value = Double.parseDouble(matcher.group(1));
      this.unit = matcher.group(2).toLowerCase(Locale.ENGLISH);
      this.nanoseconds = convertToNanoseconds(this.value, this.unit);
    } else {
      throw new IllegalArgumentException("Invalid time duration format: " + value);
    }
  }

  /**
   * Get the numerical value before the unit.
   *
   * @return The value as a double.
   */
  public double getValue() {
    return value;
  }

  /**
   * Get the unit of time duration (e.g., ms, s, m).
   *
   * @return The unit string.
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Get the duration in nanoseconds (canonical unit).
   *
   * @return The duration converted to nanoseconds.
   */
  public long getNanoseconds() {
    return nanoseconds;
  }

  /**
   * Get the duration in milliseconds.
   *
   * @return The duration converted to milliseconds.
   */
  public double getMilliseconds() {
    return nanoseconds / 1_000_000.0;
  }

  /**
   * Get the duration in seconds.
   *
   * @return The duration converted to seconds.
   */
  public double getSeconds() {
    return nanoseconds / 1_000_000_000.0;
  }

  /**
   * Get the duration in minutes.
   *
   * @return The duration converted to minutes.
   */
  public double getMinutes() {
    return nanoseconds / (60.0 * 1_000_000_000.0);
  }

  /**
   * Get the duration in hours.
   *
   * @return The duration converted to hours.
   */
  public double getHours() {
    return nanoseconds / (60.0 * 60.0 * 1_000_000_000.0);
  }

  /**
   * Get the duration in days.
   *
   * @return The duration converted to days.
   */
  public double getDays() {
    return nanoseconds / (24.0 * 60.0 * 60.0 * 1_000_000_000.0);
  }

  /**
   * Convert a value with a unit to nanoseconds.
   *
   * @param value The numerical value.
   * @param unit The unit (ms, s, m, h, d, w).
   * @return The value converted to nanoseconds.
   */
  private long convertToNanoseconds(double value, String unit) {
    switch (unit) {
      case "ms":
        return (long) (value * 1_000_000); // 1ms = 1,000,000ns
      case "s":
        return (long) (value * 1_000_000_000); // 1s = 1,000,000,000ns
      case "m":
        return (long) (value * 60 * 1_000_000_000); // 1m = 60s
      case "h":
        return (long) (value * 60 * 60 * 1_000_000_000); // 1h = 60m
      case "d":
        return (long) (value * 24 * 60 * 60 * 1_000_000_000); // 1d = 24h
      case "w":
        return (long) (value * 7 * 24 * 60 * 60 * 1_000_000_000); // 1w = 7d
      default:
        throw new IllegalArgumentException("Unsupported time duration unit: " + unit);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    TimeDuration that = (TimeDuration) o;
    return Double.compare(that.value, value) == 0 &&
      nanoseconds == that.nanoseconds &&
      unit.equals(that.unit);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    long temp;
    temp = Double.doubleToLongBits(value);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + unit.hashCode();
    result = 31 * result + (int) (nanoseconds ^ (nanoseconds >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "TimeDuration{" +
      "value=" + value +
      ", unit='" + unit + '\'' +
      ", nanoseconds=" + nanoseconds +
      '}';
  }
}
