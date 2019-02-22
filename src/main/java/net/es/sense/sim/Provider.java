package net.es.sense.sim;

import lombok.Builder;
import lombok.Data;

/**
 *
 * @author hacksaw
 */
@Builder
@Data
public class Provider {
  private String id;
  private String url;
  private String portPrefix;

  private final static String FORMAT =
          "{ id = \"%s\"\n" +
          "  url = \"%s\"\n" +
          "  portPrefix = \"%s:\"\n" +
          "}\n";

  @Override
  public String toString() {
    return String.format(FORMAT, id, url, portPrefix);
  }
}
