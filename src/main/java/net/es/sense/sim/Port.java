package net.es.sense.sim;

import com.google.common.base.Strings;
import lombok.Data;

/**
 *
 * @author hacksaw
 */
@Data
public class Port {
  private String type;
  private String name;
  private String remote;
  private String label;
  private String bandwidth;
  private String _interface;
  private String attributes;

  @Override
  public String toString() {
   return String.format("%s %s %s %s %s %s %s\n",
           Strings.isNullOrEmpty(type) ? "ethernet" : type,
           Strings.isNullOrEmpty(name) ? "undefined" : name,
           Strings.isNullOrEmpty(remote) ? "" : remote,
           Strings.isNullOrEmpty(label) ? "vlan:0-4095" : label,
           Strings.isNullOrEmpty(bandwidth) ? "10000" : bandwidth,
           Strings.isNullOrEmpty(_interface) ? "my0" : _interface,
           Strings.isNullOrEmpty(attributes) ? "-" : attributes);
  }
}
