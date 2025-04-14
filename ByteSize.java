package io.cdap.wrangler.api.parser;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class representing a byte size token with unit conversion functionality.
 */
public class ByteSize extends Token {
  private static final Pattern BYTE_SIZE_PATTERN = Pattern.compile("([-+]?\\d*\\.?\\d+)\\s*([kKmMgGtTpP][bB]?)");
  
  private final double value;
  private final String unit;
  private final long bytes;

  /**
   * Constructor for ByteSize token.
   *
   * @param value The text representation of byte size (e.g., "10KB").
   */
  public ByteSize(String value) {
    super(value, TokenType.BYTE_SIZE);
    Matcher matcher = BYTE_SIZE_PATTERN.matcher(value);
    
    if (matcher.matches()) {
      this.value = Double.parseDouble(matcher.group(1));
      this.unit = matcher.group(2).toUpperCase(Locale.ENGLISH);
      this.bytes = convertToBytes(this.value, this.unit);
    } else {
      throw new IllegalArgumentException("Invalid byte size format: " + value);
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
   * Get the unit of byte size (e.g., KB, MB).
   *
   * @return The unit string.
   */
  public String getUnit() {
    return unit;
  }

  /**
   * Get the size in bytes (canonical unit).
   *
   * @return The size converted to bytes.
   */
  public long getBytes() {
    return bytes;
  }

  /**
   * Get the size in kilobytes.
   *
   * @return The size converted to kilobytes.
   */
  public double getKilobytes() {
    return bytes / 1024.0;
  }

  /**
   * Get the size in megabytes.
   *
   * @return The size converted to megabytes.
   */
  public double getMegabytes() {
    return bytes / (1024.0 * 1024.0);
  }

  /**
   * Get the size in gigabytes.
   *
   * @return The size converted to gigabytes.
   */
  public double getGigabytes() {
    return bytes / (1024.0 * 1024.0 * 1024.0);
  }

  /**
   * Get the size in terabytes.
   *
   * @return The size converted to terabytes.
   */
  public double getTerabytes() {
    return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
  }

  /**
   * Convert a value with a unit to bytes.
   *
   * @param value The numerical value.
   * @param unit The unit (KB, MB, GB, TB, etc.).
   * @return The value converted to bytes.
   */
  private long convertToBytes(double value, String unit) {
    switch (unit) {
      case "B":
        return (long) value;
      case "K":
      case "KB":
        return (long) (value * 1024);
      case "M":
      case "MB":
        return (long) (value * 1024 * 1024);
      case "G":
      case "GB":
        return (long) (value * 1024 * 1024 * 1024);
      case "T":
      case "TB":
        return (long) (value * 1024 * 1024 * 1024 * 1024);
      case "P":
      case "PB":
        return (long) (value * 1024 * 1024 * 1024 * 1024 * 1024);
      default:
        throw new IllegalArgumentException("Unsupported byte size unit: " + unit);
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

    ByteSize byteSize = (ByteSize) o;
    return Double.compare(byteSize.value, value) == 0 &&
      bytes == byteSize.bytes &&
      unit.equals(byteSize.unit);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    long temp;
    temp = Double.doubleToLongBits(value);
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    result = 31 * result + unit.hashCode();
    result = 31 * result + (int) (bytes ^ (bytes >>> 32));
    return result;
  }

  @Override
  public String toString() {
    return "ByteSize{" +
      "value=" + value +
      ", unit='" + unit + '\'' +
      ", bytes=" + bytes +
      '}';
  }
}
