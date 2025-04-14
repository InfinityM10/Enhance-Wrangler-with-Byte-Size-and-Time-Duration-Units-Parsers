package io.cdap.wrangler.steps.transformation;

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.ErrorRowException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Directive for aggregating byte size and time duration values.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Description("Aggregates byte size and time duration columns into summarized statistics.")
public class AggregateStatsDirective implements Directive {

  public static final String NAME = "aggregate-stats";
  private String sizeSrcCol;
  private String timeSrcCol;
  private String totalSizeCol;
  private String totalTimeCol;
  private String sizeUnit;
  private String timeUnit;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("size_column", TokenType.COLUMN_NAME);
    builder.define("time_column", TokenType.COLUMN_NAME);
    builder.define("total_size_column", TokenType.COLUMN_NAME);
    builder.define("total_time_column", TokenType.COLUMN_NAME);
    builder.define("size_unit", TokenType.IDENTIFIER, "MB");
    builder.define("time_unit", TokenType.IDENTIFIER, "s");
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveExecutionException {
    this.sizeSrcCol = ((ColumnName) args.value("size_column")).value();
    this.timeSrcCol = ((ColumnName) args.value("time_column")).value();
    this.totalSizeCol = ((ColumnName) args.value("total_size_column")).value();
    this.totalTimeCol = ((ColumnName) args.value("total_time_column")).value();
    this.sizeUnit = ((Text) args.value("size_unit")).value();
    this.timeUnit = ((Text) args.value("time_unit")).value();
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException, ErrorRowException {
    // Using ExecutorContext for maintaining running totals across batches
    String sizeAggregateKey = "aggregate-stats.total_size";
    String timeAggregateKey = "aggregate-stats.total_time";
    String countKey = "aggregate-stats.count";

    // Get existing totals from context store or initialize to zero
    Map<String, Object> store = context.getStore();
    Long totalBytes = (Long) store.getOrDefault(sizeAggregateKey, 0L);
    Long totalNanos = (Long) store.getOrDefault(timeAggregateKey, 0L);
    Integer count = (Integer) store.getOrDefault(countKey, 0);

    // Process each row and update totals
    for (Row row : rows) {
      Object sizeObj = row.getValue(sizeSrcCol);
      Object timeObj = row.getValue(timeSrcCol);

      try {
        // Process byte size column
        if (sizeObj != null) {
          long bytes;
          if (sizeObj instanceof ByteSize) {
            bytes = ((ByteSize) sizeObj).getBytes();
          } else if (sizeObj instanceof String) {
            bytes = new ByteSize((String) sizeObj).getBytes();
          } else if (sizeObj instanceof Number) {
            bytes = ((Number) sizeObj).longValue(); // Assume bytes
          } else {
            throw new DirectiveExecutionException(
                String.format("Column '%s' contains an invalid byte size format: %s", sizeSrcCol, sizeObj));
          }
          totalBytes += bytes;
        }

        // Process time duration column
        if (timeObj != null) {
          long nanos;
          if (timeObj instanceof TimeDuration) {
            nanos = ((TimeDuration) timeObj).getNanoseconds();
          } else if (timeObj instanceof String) {
            nanos = new TimeDuration((String) timeObj).getNanoseconds();
          } else if (timeObj instanceof Number) {
            nanos = ((Number) timeObj).longValue(); // Assume milliseconds
          } else {
            throw new DirectiveExecutionException(
                String.format("Column '%s' contains an invalid time duration format: %s", timeSrcCol, timeObj));
          }
          totalNanos += nanos;
        }

        count++;
      } catch (Exception e) {
        // Skip rows with invalid data but log the error
        context.getLogger().error("Error processing row: " + e.getMessage());
      }
    }

    // Store updated totals in context
    store.put(sizeAggregateKey, totalBytes);
    store.put(timeAggregateKey, totalNanos);
    store.put(countKey, count);

    // Check if this is the last batch - if so, return the final result
    if (context.getTransientStore().containsKey("last.batch") && (boolean) context.getTransientStore().get("last.batch")) {
      // Convert totals to specified units
      double convertedSize = convertSize(totalBytes, sizeUnit);
      double convertedTime = convertTime(totalNanos, timeUnit);

      // Create a new row with the results
      Row result = new Row();
      result.add(totalSizeCol, convertedSize);
      result.add(totalTimeCol, convertedTime);
      result.add("record_count", count);

      return Arrays.asList(result);
    }

    // Otherwise, return the original rows for further processing
    return rows;
  }

  /**
   * Convert bytes to the specified unit.
   *
   * @param bytes The size in bytes.
   * @param unit The target unit (KB, MB, GB, TB).
   * @return The converted size.
   */
  private double convertSize(long bytes, String unit) {
    switch (unit.toUpperCase()) {
      case "B":
        return bytes;
      case "KB":
        return bytes / 1024.0;
      case "MB":
        return bytes / (1024.0 * 1024.0);
      case "GB":
        return bytes / (1024.0 * 1024.0 * 1024.0);
      case "TB":
        return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
      default:
        return bytes / (1024.0 * 1024.0); // Default to MB
    }
  }

  /**
   * Convert nanoseconds to the specified unit.
   *
   * @param nanos The duration in nanoseconds.
   * @param unit The target unit (ms, s, m, h, d).
   * @return The converted duration.
   */
  private double convertTime(long nanos, String unit) {
    switch (unit.toLowerCase()) {
      case "ns":
        return nanos;
      case "ms":
        return nanos / 1_000_000.0;
      case "s":
        return nanos / 1_000_000_000.0;
      case "m":
        return nanos / (60.0 * 1_000_000_000.0);
      case "h":
        return nanos / (60.0 * 60.0 * 1_000_000_000.0);
      case "d":
        return nanos / (24.0 * 60.0 * 60.0 * 1_000_000_000.0);
      default:
        return nanos / 1_000_000_000.0; // Default to seconds
    }
  }

  @Override
  public String toString() {
    return NAME + " " + sizeSrcCol + " " + timeSrcCol + " " + totalSizeCol + " " 
           + totalTimeCol + " " + sizeUnit + " " + timeUnit;
  }
}
