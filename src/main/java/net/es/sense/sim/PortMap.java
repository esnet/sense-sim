package net.es.sense.sim;

import lombok.Data;
import lombok.ToString;
import net.es.nsi.common.jaxb.nml.NmlBidirectionalPortType;

/**
 * A bean holding port related information needed to generate OpenNSA
 * topology configuration.
 * 
 * @author hacksaw
 */
@Data
@ToString
public class PortMap {
  private NmlBidirectionalPortType port;
  private String type;
  private String networkId;
  private String networkLabel;
  private String portId;
  private String portName;
  private String label;
  private String isAlias;
  private String remote;
  private long bandwidth;
  private String inter;
}
