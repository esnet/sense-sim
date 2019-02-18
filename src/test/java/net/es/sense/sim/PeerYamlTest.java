package net.es.sense.sim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class PeerYamlTest {
  private static final String PEERS_FILENAME = "src/test/resources/peers.yaml";

  @Test
  public void readTest() throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    Port port1 = new Port();
    port1.setType("ethernet");
    port1.setName("to_production7");
    port1.setRemote("production7.surfnet.nl:1990:topology#to_netherlight7-(in|out)");
    port1.setLabel("vlan:1-4095");
    port1.setBandwidth("100000");
    port1.set_interface("fk0");
    port1.setAttributes("-");

    Port port2 = new Port();
    port2.setType("ethernet");
    port2.setName("to_netherlight_testbed7");
    port2.setRemote("netherlight-testbed7.surfnet.nl:1990:topology#to_netherlight7-(in|out)");
    port2.setLabel("vlan:1-4095");
    port2.setBandwidth("100000");
    port2.set_interface("fk1");
    port2.setAttributes("-");

    Peer peer1 = new Peer();
    peer1.setNetworkId("urn:ogf:network:netherlight7.surfnet.nl:1990:topology");
    peer1.setPort(Lists.newArrayList(port1, port2));

    Port port3 = new Port();
    port3.setType("ethernet");
    port3.setName("to_netherlight7");
    port3.setRemote("netherlight7.surfnet.nl:1990:topology#to_production7-(in|out)");
    port3.setLabel("vlan:1-4095");
    port3.setBandwidth("100000");
    port3.set_interface("fk0");
    port3.setAttributes("-");

    Port port4 = new Port();
    port4.setType("ethernet");
    port4.setName("to_netherlight_testbed7");
    port4.setRemote("netherlight-testbed7.surfnet.nl:1990:topology#to_production7-(in|out)");
    port4.setLabel("vlan:1-4095");
    port4.setBandwidth("100000");
    port4.set_interface("fk1");
    port4.setAttributes("-");

    Peer peer2 = new Peer();
    peer2.setNetworkId("urn:ogf:network:production7.surfnet.nl:1990:topology");
    peer2.setPort(Lists.newArrayList(port3, port4));

    List<Peer> list = Lists.newArrayList(peer1, peer2);

    String yamlArray = mapper.writeValueAsString(list);
    System.out.println(yamlArray);

    List<Peer> peers;
    try {
         peers = mapper.readValue(new File(PEERS_FILENAME),
                 new TypeReference<List<Peer>>() { });

         Assert.assertEquals(list, peers);
    } catch (IOException ex) {
      System.err.println("Could not read peers file " + PEERS_FILENAME);
      throw ex;
    }
  }
}
