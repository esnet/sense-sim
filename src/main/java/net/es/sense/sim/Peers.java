package net.es.sense.sim;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class Peers {
  public static Map<String, Peer> getPeers(String filename) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

    try {
        List<Peer> peers = mapper.readValue(new File(filename), new TypeReference<List<Peer>>() { });
        return peers.stream()
                .collect(Collectors.toMap(Peer::getNetworkId, x -> x, (oldValue, newValue) -> oldValue));
    } catch (IOException ex) {
      log.error("Could not read peers file {}", filename, ex);
      throw ex;
    }
  }
}
