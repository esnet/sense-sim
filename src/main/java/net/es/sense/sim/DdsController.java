package net.es.sense.sim;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.mail.MessagingException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.constants.Nsi;
import net.es.nsi.common.jaxb.NmlParser;
import net.es.nsi.common.jaxb.NsaParser;
import net.es.nsi.common.jaxb.nml.NmlTopologyType;
import net.es.nsi.common.jaxb.nsa.NsaType;
import net.es.nsi.common.util.ContentTransferEncoding;
import net.es.nsi.dds.lib.client.DdsClient;
import net.es.nsi.dds.lib.client.DocumentResult;
import net.es.nsi.dds.lib.client.DocumentsResult;
import net.es.nsi.dds.lib.jaxb.dds.ContentType;
import net.es.nsi.dds.lib.jaxb.dds.DocumentType;

/**
 * An utility class for access the NSI-DDS service.
 *
 * @author hacksaw
 */
@Slf4j
public class DdsController {
  private final DdsClient dds = new DdsClient();
  private final String baseUrl;

  /**
   * Create the DDS controller.
   *
   * @param baseUrl The URL of the NSI-DDS service.
   */
  public DdsController(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  /**
   * Get a list of NSA documents.
   *
   * @return Returns a Map of all NSA documents indexed by NSA identifier.
   * @throws NotFoundException
   * @throws IOException
   */
  public Map<String, NsaMap> getNsaDocuments() throws NotFoundException, IOException {
    Map<String, NsaMap> map = new ConcurrentHashMap<>();

    DocumentsResult documents = dds.getDocumentsByType(baseUrl, Nsi.NSI_DOC_TYPE_NSA_V1);
    if (documents.getStatus() != Response.Status.OK) {
      throw new NotFoundException("DDS return status " + documents.getStatus());
    }

    for (DocumentType d : documents.getDocuments()) {
        ContentType content = d.getContent();
        try {
          NsaType nsa = NsaParser.getInstance().readDocument(
                  net.es.nsi.common.util.ContentType.decode(
                          content.getContentType(),
                          ContentTransferEncoding.decode(
                                  content.getContentTransferEncoding(),
                                  content.getValue()
                          )
                  )
          );

          NsaMap holder = new NsaMap();
          holder.setNsaId(nsa.getId());
          holder.setDocument(nsa);
          map.put(nsa.getId(), holder);

        } catch (IOException | MessagingException | JAXBException ex) {
          throw new IOException("Encountered exception processing document " + d.getHref(), ex);
        }
    }

    return map;
  }

  /**
   * Get a list of topology documents.
   *
   * @param list The list of NSA from which to retrieve documents.
   * @return Returns a Map of all topology documents indexed by network identifier.
   * @throws NotFoundException
   * @throws IOException
   */
  public Map<String, TopologyMap> getTopologyDocuments(Collection<NsaMap> list)
          throws NotFoundException, IOException {
    Map<String, TopologyMap> map = new ConcurrentHashMap<>();

    // Iterate through each NSA.
    for (NsaMap nsa : list) {
      // We need to retrieve each NML topology document associated with a networkid.
      for (String networkId : nsa.getDocument().getNetworkId()) {
        DocumentResult document = dds.getDocument(baseUrl, nsa.getNsaId(), Nsi.NSI_DOC_TYPE_TOPOLOGY_V2, networkId);
        if (null == document.getStatus()) {
          throw new NotFoundException(String.format("DDS return no status for nsaId = %s, networkId = %s",
                  document.getStatus(), nsa.getNsaId(), networkId));
        } else switch (document.getStatus()) {
          case OK:
            try {
              // We got a document so decode and parse into a NML structure.
              ContentType content = document.getDocument().getContent();
              NmlTopologyType topology = NmlParser.getInstance().readDocument(
                      net.es.nsi.common.util.ContentType.decode(
                              content.getContentType(),
                              ContentTransferEncoding.decode(
                                      content.getContentTransferEncoding(),
                                      content.getValue()
                              )
                      )
              );

              // Store the retrieved document against the networkId.
              TopologyMap holder = new TopologyMap();
              holder.setNetworkId(networkId);
              holder.setDocument(topology);
              map.put(topology.getId(), holder);
            } catch (IOException | MessagingException | JAXBException ex) {
              throw new IOException("Encountered exception processing networkId " + networkId, ex);
            }
            break;
          case NOT_FOUND:
            log.debug("DDS return status \"{}\" for nsaId = {}, networkId = {}",
                    document.getStatus(), nsa.getNsaId(), networkId);
            break;
          default:
            throw new NotFoundException(String.format("DDS return status \"%s\" for nsaId = %s, networkId = %s",
                    document.getStatus(), nsa.getNsaId(), networkId));
        }
      }
    }

    return map;
  }
}
