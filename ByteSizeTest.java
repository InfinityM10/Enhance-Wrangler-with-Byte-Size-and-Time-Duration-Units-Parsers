package io.cdap.wrangler.parser;

import io.cdap.wrangler.api.parser.ByteSize;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the ByteSize class.
 */
public class ByteSizeTest {

  @Test
  public void testByteSizeParsing() {
    ByteSize size1 = new ByteSize("10KB");
    Assert.assertEquals(10240, size1.getBytes());
    Assert.assertEquals(10, size1.getKilobytes(), 0.001);
    Assert.assertEquals(0.01, size1.getMegabytes(), 0.001);

    ByteSize size2 = new ByteSize("1.5MB");
    Assert.assertEquals(1572864, size2.getBytes());
    Assert.assertEquals(1536, size2.getKilobytes(), 0.001);
    Assert.assertEquals(1.5, size2.getMegabytes(), 0.001);

    ByteSize size3 = new ByteSize("2GB");
    Assert.assertEquals(2147483648L, size3.getBytes());
    Assert.assertEquals(2, size3.getGigabytes(), 0.001);

    ByteSize size4 = new ByteSize("0.5TB");
    Assert.assertEquals(549755813888L, size4.getBytes());
    Assert.assertEquals(0.5, size4.getTerabytes(), 0.001);
  }

  @Test
  public void testDifferentCasing() {
    ByteSize size1 = new ByteSize("10kb");
    ByteSize size2 = new ByteSize("10KB");
    ByteSize size3 = new ByteSize("10Kb");

    Assert.assertEquals(size1.getBytes(), size2.getBytes());
    Assert.assertEquals(size2.getBytes(), size3.getBytes());
  }

  @Test
  public void testWithoutB() {
    ByteSize size1 = new ByteSize("10K");
    ByteSize size2 = new ByteSize("10KB");
    
    Assert.assertEquals(size1.getBytes(), size2.getBytes());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidFormat() {
    new ByteSize("invalid");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testInvalidUnit() {
    new ByteSize("10XB");
  }

  @Test
  public void testEqualsAndHashCode() {
    ByteSize size1 = new ByteSize("10KB");
    ByteSize size2 = new ByteSize("10KB");
    ByteSize size3 = new ByteSize("20KB");

    Assert.assertEquals(size1, size2);
    Assert.assertNotEquals(size1, size3);
    Assert.assertEquals(size1.hashCode(), size2.hashCode());
  }
}
