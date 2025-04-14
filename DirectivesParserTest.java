package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.SyntaxError;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenGroup;
import io.cdap.wrangler.api.parser.UsageDefinition;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for parsing directives containing byte size and time duration tokens.
 */
public class DirectivesParserTest {

  @Test
  public void testParseDirectiveWithByteSizeAndTimeDuration() throws Exception {
    String directive = "aggregate-stats :col1 :col2 :total_size :total_time 'MB' 's'";
    
    TokenGroup tokenGroups = new TextDirectives(directive).get();
    
    Assert.assertEquals(1, tokenGroups.size());
    TokenGroup group = tokenGroups.get(0);
    
    Assert.assertEquals("aggregate-stats", group.getDirectiveName());
    
    UsageDefinition definition = new UsageDefinition("aggregate-stats");
    definition.define("col1", io.cdap.wrangler.api.parser.TokenType.COLUMN_NAME);
    definition.define("col2", io.cdap.wrangler.api.parser.TokenType.COLUMN_NAME);
    definition.define("result1", io.cdap.wrangler.api.parser.TokenType.COLUMN_NAME);
    definition.define("result2", io.cdap.wrangler.api.parser.TokenType.COLUMN_NAME);
    definition.define("unit1", io.cdap.wrangler.api.parser.TokenType.STRING);
    definition.define("unit2", io.cdap.wrangler.api.parser.TokenType.STRING);
    
    // Validate against usage
    group.validate(definition);
    
    Assert.assertTrue(group.get(0) instanceof ColumnName);
    Assert.assertTrue(group.get(1) instanceof ColumnName);
    Assert.assertTrue(group.get(2) instanceof ColumnName);
    Assert.assertTrue(group.get(3) instanceof ColumnName);
    Assert.assertTrue(group.get(4) instanceof Text);
    Assert.assertTrue(group.get(5) instanceof Text);
    
    Assert.assertEquals("col1", ((ColumnName) group.get(0)).value());
    Assert.assertEquals("col2", ((ColumnName) group.get(1)).value());
    Assert.assertEquals("total_size", ((ColumnName) group.get(2)).value());
    Assert.assertEquals("total_time", ((ColumnName) group.get(3)).value());
    Assert.assertEquals("MB", ((Text) group.get(4)).value());
    Assert.assertEquals("s", ((Text) group.get(5)).value());
  }

  @Test
  public void testParseDirectiveWithByteSizeValues() throws Exception {
    // Using another example directive to test the ByteSize token
    String directive = "transform-size :col1 '10KB'";
    
    TokenGroup tokenGroups = new TextDirectives(directive).get();
    
    Assert.assertEquals(1, tokenGroups.size());
    TokenGroup group = tokenGroups.get(0);
    
    Assert.assertEquals("transform-size", group.getDirectiveName());
    
    UsageDefinition definition = new UsageDefinition("transform-size");
    definition.define("column", io.cdap.wrangler.api.parser.TokenType.COLUMN_NAME);
    definition.define("size", io.cdap.wrangler.api.parser.TokenType.BYTE_SIZE);
    
    // This should validate successfully when ByteSize tokens are properly implemented
    group.validate(definition);
    
    Assert.assertTrue(group.get(0) instanceof ColumnName);
    Assert.assertTrue(group.get(1) instanceof ByteSize);
    
    Assert.assertEquals("col1", ((ColumnName) group.get(0)).value());
    ByteSize byteSize = (ByteSize) group.get(1);
    Assert.assertEquals(10240, byteSize.getBytes());
  }

  @Test
  public void testParseDirectiveWithTimeDurationValues() throws Exception {
    // Testing a directive with time duration token
    String directive = "transform-time :col1 '500ms'";
    
    TokenGroup tokenGroups = new TextDirectives(directive).get();
    
    Assert.assertEquals(1, tokenGroups.size());
    TokenGroup group = tokenGroups.get(0);
    
    Assert.assertEquals("transform-time", group.getDirectiveName());
    
    UsageDefinition definition = new UsageDefinition("transform-time");
    definition.define("column", io.cdap.wrangler.api.parser.TokenType.COLUMN_NAME);
    definition.define("duration", io.cdap.wrangler.api.parser.TokenType.TIME_DURATION);
    
    // This should validate successfully when TimeDuration tokens are properly implemented
    group.validate(definition);
    
    Assert.assertTrue(group.get(0) instanceof ColumnName);
    Assert.assertTrue(group.get(1) instanceof TimeDuration);
    
    Assert.assertEquals("col1", ((ColumnName) group.get(0)).value());
    TimeDuration duration = (TimeDuration) group.get(1);
    Assert.assertEquals(500 * 1_000_000, duration.getNanoseconds());
  }

  @Test(expected = SyntaxError.class)
  public void testInvalidByteSizeFormat() throws Exception {
    // Using a directive with an invalid byte size format
    String directive = "transform-size :col1 '10XB'"; // Invalid unit XB
    
    new TextDirectives(directive).get();
  }

  @Test(expected = SyntaxError.class)
  public void testInvalidTimeDurationFormat() throws Exception {
    // Using a directive with an invalid time duration format
    String directive = "transform-time :col1 '500xy'"; // Invalid unit xy
    
    new TextDirectives(directive).get();
  }
}
