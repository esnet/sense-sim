package net.es.sense.sim;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class ConfigWriterTest {
  @Test
  public void testLocalId() throws Exception {
    assertEquals("nordunet-1",
            ConfigWriter.strip("nordunet-1"));
    assertEquals("2c_39_c1_38_e0_00-4-1 ",
            ConfigWriter.strip("2c:39:c1:38:e0:00-4-1 "));
    assertEquals("somerouter_1-1-1",
            ConfigWriter.strip("somerouter#1-1-1"));
    assertEquals("star-tb1_6_2_1_+",
            ConfigWriter.strip("star-tb1:6_2_1:+"));
  }

  @Test
  public void testNetworkId() throws Exception {
    assertEquals("netherlight7.surfnet.nl:1990",
            ConfigWriter.strip_networkUrn("urn:ogf:network:surfnet.nl:1990:netherlight7"));
    assertEquals("snvaca.pacificwave.net:2016",
            ConfigWriter.strip_networkUrn("urn:ogf:network:snvaca.pacificwave.net:2016:topology"));
    assertEquals("tb.es.net:2013",
            ConfigWriter.strip_networkUrn("urn:ogf:network:tb.es.net:2013:"));
  }
}
