# Wrangler Library Enhancement

This repository contains an enhanced version of the CDAP Wrangler library with support for parsing and utilizing byte size and time duration units within recipes.

## New Features

### 1. Byte Size and Time Duration Unit Parsers
The Wrangler library now supports native handling of units like:
- **Byte Sizes**: KB, MB, GB, TB, PB
- **Time Durations**: ms, s, m, h, d, w

### 2. New Aggregate-Stats Directive
A new directive has been implemented to demonstrate the usage of these new parsers:

**Arguments**:
- `size_column`: Source column with byte sizes
- `time_column`: Source column with time durations
- `total_size_column`: Target column for aggregated size
- `total_time_column`: Target column for aggregated time
- `size_unit` (optional): Output unit for size (B, KB, MB, GB, TB - default: MB)
- `time_unit` (optional): Output unit for time (ns, ms, s, m, h, d - default: s)

**Example**:
```
aggregate-stats :size_column :time_column :total_size_column :total_time_column [:size_unit] [:time_unit]
aggregate-stats :data_transfer_size :response_time :total_size_mb :total_time_sec 'MB' 's'
```

This directive processes all rows, accumulates the byte sizes and time durations, and outputs a single row with the total values converted to the specified units.

## Usage Examples

### Working with Byte Sizes
```
// Parse a byte size from a string column
parse-as-byte-size :raw_size :parsed_size

// The value can now be used in calculations and will automatically handle unit conversions
set-column :size_in_mb parsed_size.getMegabytes()
```

### Working with Time Durations
```
// Parse a time duration from a string column
parse-as-time-duration :raw_time :parsed_time

// The value can now be used in calculations and will automatically handle unit conversions
set-column :time_in_seconds parsed_time.getSeconds()
```

### Aggregating Statistics
```
// Aggregate byte sizes and time durations from all rows
aggregate-stats :data_size :response_time :total_size_gb :total_time_ms 'GB' 'ms'
```

## Implementation Details

The enhancement includes:
- New lexer rules for BYTE_SIZE and TIME_DURATION tokens
- New Java classes for handling these token types
- Updated parser to recognize and validate these tokens
- A new aggregate directive that utilizes these token types

The implementation is fully backward compatible with existing Wrangler directives.
