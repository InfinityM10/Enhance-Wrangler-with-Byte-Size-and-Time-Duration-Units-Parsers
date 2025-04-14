package io.cdap.wrangler.api.parser;

/**
 * Types of tokens supported by the parser.
 */
public enum TokenType {
  COLUMN_NAME,
  IDENTIFIER,
  NUMERIC,
  STRING,
  EXPRESSION,
  // New token types added for byte size and time duration
  BYTE_SIZE,
  TIME_DURATION
}
