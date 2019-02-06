package net.es.sense.sim;

import lombok.Data;
import net.es.nsi.common.jaxb.nml.NmlTopologyType;

/**
 * A simple bean mapping between network identifier and NM document.
 *
 * @author hacksaw
 */
@Data
public class TopologyMap {
  String networkId;
  NmlTopologyType document;
}
