package net.es.sense.sim;

import lombok.Data;
import net.es.nsi.common.jaxb.nsa.NsaType;

/**
 * A simple bean mapping NSA identifier to NSA document.
 * 
 * @author hacksaw
 */
@Data
public class NsaMap {
  String nsaId;
  NsaType document;
}
