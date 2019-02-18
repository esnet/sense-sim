package net.es.sense.sim;

import java.util.List;
import lombok.Data;

/**
 *
 * @author hacksaw
 */

@Data
public class Peer {
  private String networkId;
  private List<Port> port;
}
