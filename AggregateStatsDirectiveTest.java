package io.cdap.wrangler.steps.transformation;

import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.parser.TextDirectives;
import io.cdap.wrangler.parser.TestingRig;
import io.cdap.wrangler.registry.DirectiveRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

/**
 * Tests for the AggregateStatsDirective class.
 */
public class AggregateStatsDirectiveTest {

  private DirectiveRegistry registry;
  private ExecutorContext mockContext;

  @Before
  public void setup() {
    registry = new DirectiveRegistry();
    registry.add(new AggregateStatsDirective());

    // Create a mock ExecutorContext
    mockContext = Mockito.mock(ExecutorContext.class);
    Map<String, Object> store = new HashMap<>();
    Map<String, Object> transientStore = new HashMap<>();
    when(mockContext.getStore()).thenReturn(store);
    when(mockContext.getTransientStore()).thenReturn(transientStore);
  }

  @Test
  public void testAggregateStatsWithStringValues() throws Exception {
    // Prepare test data
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("data_size", "10KB");
    row1.add("response_time", "100ms");
    rows.add(row1);

    Row row2 = new Row();
    row2.add("data_size", "20KB");
    row2.add("response_time", "200ms");
    rows.add(row2);

    Row row3 = new Row();
    row3.add("data_size", "30KB");
    row3.add("response_time", "300ms");
    rows.add(row3);

    // Set up directive
    String[] directives = new String[] {
        "aggregate-stats :data_size :response_time :total_size_mb :total_time_sec"
    };

    // Mark this as the last batch
    when(mockContext.getTransientStore().containsKey("last.batch")).thenReturn(true);
    when(mockContext.getTransientStore().get("last.batch")).thenReturn(true);

    // Execute the directive
    List<Row> results = TestingRig.execute(directives, rows, registry, mockContext);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row resultRow = results.get(0);

    // Expected: (10KB + 20KB + 30KB) = 60KB = 0.05859375 MB (rounded)
    // Expected: (100ms + 200ms + 300ms) = 600ms = 0.6 seconds
    Assert.assertEquals(0.05859375, resultRow.getValue("total_size_mb"), 0.0001);
    Assert.assertEquals(0.6, resultRow.getValue("total_time_sec"), 0.0001);
    Assert.assertEquals(3, resultRow.getValue("record_count"));
  }

  @Test
  public void testAggregateStatsWithTypedValues() throws Exception {
    // Prepare test data
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("data_size", new ByteSize("1MB"));
    row1.add("response_time", new TimeDuration("1s"));
    rows.add(row1);

    Row row2 = new Row();
    row2.add("data_size", new ByteSize("2MB"));
    row2.add("response_time", new TimeDuration("2s"));
    rows.add(row2);

    // Set up directive with custom units
    String[] directives = new String[] {
        "aggregate-stats :data_size :response_time :total_size_gb :total_time_ms 'GB' 'ms'"
    };

    // Mark this as the last batch
    when(mockContext.getTransientStore().containsKey("last.batch")).thenReturn(true);
    when(mockContext.getTransientStore().get("last.batch")).thenReturn(true);

    // Execute the directive
    List<Row> results = TestingRig.execute(directives, rows, registry, mockContext);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row resultRow = results.get(0);

    // Expected: (1MB + 2MB) = 3MB = 0.00293 GB (rounded)
    // Expected: (1s + 2s) = 3s = 3000ms
    Assert.assertEquals(0.00293, resultRow.getValue("total_size_gb"), 0.0001);
    Assert.assertEquals(3000.0, resultRow.getValue("total_time_ms"), 0.1);
    Assert.assertEquals(2, resultRow.getValue("record_count"));
  }

  @Test
  public void testAggregateStatsWithMixedValues() throws Exception {
    // Prepare test data with mixed types
    List<Row> rows = new ArrayList<>();
    Row row1 = new Row();
    row1.add("data_size", "1.5MB");
    row1.add("response_time", 500); // Numeric milliseconds
    rows.add(row1);

    Row row2 = new Row();
    row2.add("data_size", new ByteSize("2.5MB"));
    row2.add("response_time", new TimeDuration("1.5s"));
    rows.add(row2);

    // Set up directive
    String[] directives = new String[] {
        "aggregate-stats :data_size :response_time :total_size_mb :total_time_sec"
    };

    // Mark this as the last batch
    when(mockContext.getTransientStore().containsKey("last.batch")).thenReturn(true);
    when(mockContext.getTransientStore().get("last.batch")).thenReturn(true);

    // Execute the directive
    List<Row> results = TestingRig.execute(directives, rows, registry, mockContext);

    // Verify results
    Assert.assertEquals(1, results.size());
    Row resultRow = results.get(0);

    // Expected: 1.5MB + 2.5MB = 4MB
    // Expected: 500ms + 1.5s = 2000ms = 2s
    Assert.assertEquals(4.0, resultRow.getValue("total_size_mb"), 0.0001);
    Assert.assertEquals(2.0, resultRow.getValue("total_time_sec"), 0.0001);
    Assert.assertEquals(2, resultRow.getValue("record_count"));
  }

  @Test
  public void testMultipleBatches() throws Exception {
    // Create a real ExecutorContext that persists between batches
    Map<String, Object> store = new HashMap<>();
    Map<String, Object> transientStore = new HashMap<>();
    ExecutorContext realContext = Mockito.mock(ExecutorContext.class);
    when(realContext.getStore()).thenReturn(store);
    when(realContext.getTransientStore()).thenReturn(transientStore);

    // Set up directive
    String[] directives = new String[] {
        "aggregate-stats :data_size :response_time :total_size_mb :total_time_sec"
    };

    // First batch
    List<Row> batch1 = new ArrayList<>();
    Row row1 = new Row();
    row1.add("data_size", "1MB");
    row1.add("response_time", "100ms");
    batch1.add(row1);

    // Not the last batch yet
    when(realContext.getTransientStore().containsKey("last.batch")).thenReturn(false);
    
    // Execute first batch
    List<Row> results1 = TestingRig.execute(directives, batch1, registry, realContext);
    
    // Should return the original rows since it's not the last batch
    Assert.assertEquals(1, results1.size());

    // Second batch
    List<Row> batch2 = new ArrayList<>();
    Row row2 = new Row();
    row2.add("data_size", "2MB");
    row2.add("response_time", "200ms");
    batch2.add(row2);

    // Now it's the last batch
    when(realContext.getTransientStore().containsKey("last.batch")).thenReturn(true);
    when(realContext.getTransientStore().get("last.batch")).thenReturn(true);
    
    // Execute second batch
    List<Row> results2 = TestingRig.execute(directives, batch2, registry, realContext);
    
    // Should return aggregated results
    Assert.assertEquals(1, results2.size());
    Row resultRow = results2.get(0);
    
    // Expected: 1MB + 2MB = 3MB
    // Expected: 100ms + 200ms = 300ms = 0.3s
    Assert.assertEquals(3.0, resultRow.getValue("total_size_mb"), 0.0001);
    Assert.assertEquals(0.3, resultRow.getValue("total_time_sec"), 0.0001);
    Assert.assertEquals(2, resultRow.getValue("record_count"));
  }

  @Test(expected = DirectiveExecutionException.class)
  public void testInvalidSizeFormat() throws Exception {
    // Prepare test data with invalid size
    List<Row> rows = new ArrayList<>();
    Row row = new Row();
    row.add("data_size", "invalid");
    row.add("response_time", "100ms");
    rows.add(row);

    // Set up directive
    String[] directives = new String[] {
        "aggregate-stats :data_size :response_time :total_size_mb :total_time_sec"
    };

    // Execute - should throw exception
    TestingRig.execute(directives, rows, registry, mockContext);
  }
}
