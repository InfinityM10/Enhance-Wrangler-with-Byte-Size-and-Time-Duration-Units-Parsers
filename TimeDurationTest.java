package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the TimeDuration class.
 */
public class TimeDurationTest {

  @Test
  public void testTimeDurationParsing() {
    TimeDuration time1 = new TimeDuration("100ms");
    Assert.assertEquals(100_000_000, time1.getNanoseconds());
    Assert.assertEquals(100, time1.getMilliseconds(), 0.001);
    Assert.assertEquals(0.1, time1.getSeconds(), 0.001);

    TimeDuration time2 = new TimeDuration("2.5s");
    Assert.assertEquals(2_500_000_000L, time2.getNanoseconds());
    Assert.assertEquals(2500, time2.getMilliseconds(), 0.001);
    Assert.assertEquals(2.5, time2.getSeconds(), 0.001);

    TimeDuration time3 = new TimeDuration("1m");
    Assert.assertEquals(60_000_000_000L, time3.getNanoseconds());
    Assert.assertEquals(60, time3.getSeconds(), 0.001);
    Assert.assertEquals(1, time3.getMinutes(), 0.001);

    TimeDuration time4 = new TimeDuration("0.5h");
    Assert.assertEquals(1_800_000_000_000L, time4.getNanoseconds());
    Assert.assertEquals(30, time4.getMinutes(), 0.001);
    Assert.assertEquals(0.5, time4.getHours(), 0.001);

    TimeDuration time5 = new TimeDuration("1d");
    Assert.assertEquals(86_400_000_000_000L, time5.getNanoseconds());
    Assert.assertEquals(24, time5.getHours(), 0.001);
    Assert.assertEquals(1, time5.getDays(), 0.001);

    TimeDuration time6 = new TimeDuration("1w");
    Assert.assertEquals(604_800_000_000_000L, time6.getNanoseconds());
    Assert.assertEquals(7, time6.getDays(), 0.001);
  }

  @Test
  public void testWithSpaces() {
    TimeDuration time1 = new TimeDuration("100 ms");
    Assert.assertEquals(100_000_000, time1.getNanoseconds());
    Assert.assertEquals(100, time1.getMilliseconds(), 0.001);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new TimeDuration("invalid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new TimeDuration("10xy");
  }

  @Test
  public void testEqualsAndHashCode() {
    TimeDuration time1 = new TimeDuration("100ms");
    TimeDuration time2 = new TimeDuration("100ms");
    TimeDuration time3 = new TimeDuration("200ms");

    Assert.assertEquals(time1, time2);
    Assert.assertNotEquals(time1, time3);
    Assert.assertEquals(time1.hashCode(), time2.hashCode());
  }
}