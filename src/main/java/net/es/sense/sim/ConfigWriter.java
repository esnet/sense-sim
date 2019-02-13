package net.es.sense.sim;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.NotFoundException;
import javax.xml.bind.JAXBElement;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import net.es.nsi.common.Nml;
import net.es.nsi.common.SimpleLabel;
import net.es.nsi.common.SimpleLabels;
import net.es.nsi.common.SimpleStp;
import net.es.nsi.common.jaxb.nml.NmlBidirectionalPortType;
import net.es.nsi.common.jaxb.nml.NmlPortGroupType;
import net.es.nsi.common.jaxb.nml.NmlPortType;

/**
 * Process all the NSA and topology files from the NSI-DDS creating a set of
 * SENSE-NSI-RM and OpenNSA configuration files to simulate the network.
 *
 * @author hacksaw
 */
@Slf4j
@Builder
public class ConfigWriter {
  // The OpenNSA configuration file template.
  private static final String NRMCONF = "[service]\n"
          + "host=localhost\n"
          + "port=%d\n"
          + "network=%s\n"
          + "logfile=nsa%d.log\n"
          + "nrmmap=nsa%d.nrm\n"
          + "database=nsa%s\n"
          + "dbuser=%s\n"
          + "dbpassword=%s\n"
          + "tls=false\n"
          + "[dud]";

  // The OpenNSA TAC file template.
  private static final String NRMTAC = "#!/usr/bin/env python\n"
          + "from opennsa import setup\n"
          + "application = setup.createApplication('nsa%d.conf', payload=True, debug=True)\n";

  // The OpenNSA discovery URL for populating the NSI-DDS configuration.
  private static final String PEER
          = "<peerURL type=\"application/vnd.ogf.nsi.nsa.v1+xml\">http://localhost:%d/NSI/discovery.xml</peerURL>\n";

  // All the configuration we will need.
  private final String ddsUrl;
  private final String userId;
  private final String password;
  private final String schemaFile;
  private final String rmFile;
  private final String logFile;
  private final String outDir;

  /**
   * This is the main control loop for generating the needed configuration files.
   *
   * @throws NotFoundException A specified input file was not found.
   * @throws IOException Shit went bad.
   */
  public void write() throws NotFoundException, IOException {
    // Make sure the target directory is already there.
    if (!Strings.isNullOrEmpty(outDir)) {
      new File(outDir).mkdirs();
    }

    // Read in the SENSE-NSI-RM configuration template we will use to generate
    // the individual configurations.
    String rmTemplate = read(rmFile, Charset.defaultCharset());
    String logTemplate = read(logFile, Charset.defaultCharset());

    // Get a list of NSA documents from the DDS.
    DdsController dds = new DdsController(ddsUrl);
    Map<String, NsaMap> nsaMap = dds.getNsaDocuments();

    // For each NSA get all associated topology documents.
    Map<String, TopologyMap> topologyMap = dds.getTopologyDocuments(nsaMap.values());
    List<PortMap> portConfig = getPortConfig(topologyMap.values());

    // Write the SENSE-NSI-RM and OpenNSA configuration files for each network.
    int nsa_count = 0;
    for (NsaMap nsa : nsaMap.values()) {
      log.error("Processing NSA {}", nsa.nsaId);
      for (String networkId : nsa.getDocument().getNetworkId()) {
        log.error(">>> Processing NSA {}, topology {}", nsa.nsaId, networkId);
        if (writeNSA(rmTemplate, logTemplate, userId, password,
            nsa.getDocument().getId(), networkId, portConfig, nsa_count)) {
          nsa_count++;
        }
      }
    }

    // Write the OpenNSA TAC files.
    writeTac(nsa_count);

    // Write out the start-up script
    writeScripts();

    // Write out the database schema needed for both OpenNSA and SENSE-NSI-RM.
    writeSchema(schemaFile, userId, password, nsa_count);

    // Write out the peer discovery information to configure the DDS.
    writeDiscovery(nsa_count);
  }

  /**
   * Processes the network topology and generates appropriate OpenNSA port
   * configuration files, OpenNSA runtime configuration file, and the
   * SENSE-NSI-RM configuration file.
   *
   * OpenNSA has some wonky port name rules so we need to make sure
   * not to violate them.  Also it automatically generates topology
   * and NSA names so we need to match the generation algorithm.
   *
   * @param rmTemplate
   * @param userId
   * @param password
   * @param providerNsaId
   * @param networkId
   * @param portConfig
   * @param count
   */
  private boolean writeNSA(String rmTemplate, String logTemplate, String userId, String password,
          String providerNsaId, String networkId, List<PortMap> portConfig, int count) {

    // Filter the list of ports to only those from the target network.
    List<String> lines = portConfig.stream()
            .filter(p -> networkId.equalsIgnoreCase(p.getNetworkId()))
            .map(p -> String.format("%s %s %s %s %d %s -\n",
              p.getType(),
              p.getPortName(),
              Strings.isNullOrEmpty(p.getRemote()) ? "-" : p.getRemote(),
              p.getLabel(),
              p.getBandwidth(),
              p.getInter()))
            .collect(Collectors.toList());

    if (lines.isEmpty()) {
      log.error("writeNSA: skipping providerId {}, networkId = {}", providerNsaId, networkId);
      return false;
    }

    // Write out the OpenNSA port configuration file for this network topology.
    write("nsa" + count + ".nrm", lines);

    // Write out the NRM config file associated with this topology.
    String stripped = strip_networkId(networkId);
    write("nsa" + count + ".conf",
            Lists.newArrayList(String.format(NRMCONF,
                    9000 + count, // port
                    stripped, // network
                    count, // logfile
                    count, // nrmmap
                    count, // database
                    userId,
                    password)));

    // Write out the SENSE-NSI-RM configuration file for this NSA.
    String nid = SimpleStp.NSI_NETWORK_URN_PREFIX + stripped.concat(":topology");
    String nsa = SimpleStp.NSI_NETWORK_URN_PREFIX + stripped.concat(":nsa");
    write("sense" + count + ".yaml",
            Lists.newArrayList(String.format(rmTemplate,
                    8000 + count, // server.port
                    8000 + count, // sense.root
                    count, // logging.config
                    count, // logging.file
                    count, // spring.datasource.url
                    userId, // spring.datasource.username
                    password, // spring.datasource.password
                    nid, // nsi.nsaId
                    8000 + count, // nsi.ddsUrl
                    nsa, // nsi.providerNsaId
                    9000 + count, // nsi.providerConnectionURL
                    8000 + count, // nsi.requesterConnectionURL
                    nid))); // networkId

    // Write out the SENSE-RM log configuration file.
    write("sense" + count + ".xml",
            Lists.newArrayList(logTemplate.replace(":filename:", "sense-rm" + count + ".log")));

    return true;
  }

  /**
   * Write the OpenNSA TAC configuration file for all instances.
   *
   * @param count
   */
  private void writeTac(int count) {
    // Write the OpenNSA TAC files.
    for (int i = 0; i < count; i++) {
      write("nsa" + i + ".tac", Lists.newArrayList(String.format(NRMTAC, i)));
    }
  }


  // An OpenNSA command line for use in the startup script.
  private static final String OPENNSA_START_SCRIPT =
          "#!/bin/bash\n" +
          "\n" +
          "for i in `ls nsa*.tac`; do\n" +
          "  if [ -f $i ]; then\n" +
          "    echo \"Starting $i with pid file ${i%.*}.pid\"\n" +
          "    nohup twistd -noy $i --pidfile ${i%.*}.pid &\n" +
          "  fi\n" +
          "done\n";

  private static final String OPENNSA_STOP_SCRIPT =
        "#!/bin/bash\n" +
        "       \n" +
        "for i in `ls nsa*.pid`; do\n" +
        "if [ -f $i ]; then\n" +
        "  echo \"Stopping $i.\"\n" +
        "  kill -9 `cat $i`\n" +
        "  fi\n" +
        "done\n";

  private static final String SENSE_START_SCRIPT =
          "#! /bin/bash\n" +
          "\n" +
          "export HOME=.\n" +
          "\n" +
          "for i in `ls $HOME/config/sense*.yaml`; do\n" +
          "  if [ -f $i ]; then\n" +
          "    path=${i%.*}\n" +
          "    root=${path##*/}\n" +
          "\n" +
          "    nohup /usr/bin/java \\\n" +
          "        -Xmx1024m -Djava.net.preferIPv4Stack=true  \\\n" +
          "        -Dcom.sun.xml.bind.v2.runtime.JAXBContextImpl.fastBoot=true \\\n" +
          "        -Dbasedir=$HOME \\\n" +
          "        -Dlogback.configurationFile=file:$path.xml \\\n" +
          "        -XX:+StartAttachListener \\\n" +
          "        -jar $HOME/rm/target/rm-0.1.0.jar \\\n" +
          "        --spring.config.name=$root > /dev/null 2>&1 &\n" +
          "    echo $! > $root.pid\n" +
          "  fi\n" +
          "done";

  private static final String SENSE_STOP_SCRIPT =
          "#!/bin/bash\n" +
          "\n" +
          "for i in `ls sense*.pid`; do\n" +
          "  if [ -f $i ]; then\n" +
          "    echo \"Stopping $i.\"\n" +
          "    kill -9 `cat $i`\n" +
          "  fi\n" +
          "done\n";

  /**
   * Write the OpenNSA startup and shutdown script for each NSA instance.
   *
   * @param count
   */
  private void writeScripts() {
    // Write out the start-up script
    write("opennsa_start.sh", Lists.newArrayList(OPENNSA_START_SCRIPT));
    write("opennsa_stop.sh", Lists.newArrayList(OPENNSA_STOP_SCRIPT));
    write("sense_start.sh", Lists.newArrayList(SENSE_START_SCRIPT));
    write("sense_stop.sh", Lists.newArrayList(SENSE_STOP_SCRIPT));
  }



  // Database configuration schema.  Each simulated network will require a
  // dedicated SENSE RM and OpenNSA database. The SENSE-RM schema is
  // automatically applied by the runtime, however, we have to manually apply
  // the OpenNSA schema here as well.  A single "sense" database user will be
  // by all SENSE RM and OpenNSA instances.
  private static final String DB_USER = "CREATE USER %s WITH ENCRYPTED PASSWORD '%s';\n";
  private static final String DB_CREATE = "CREATE DATABASE sense%d;\n"
          + "GRANT ALL PRIVILEGES ON DATABASE sense%d TO %s;\n"
          + "CREATE DATABASE nsa%d;\n"
          + "GRANT ALL PRIVILEGES ON DATABASE nsa%d TO %s;\n";

  private static final String DB_SCRIPT_START =
          "#!/bin/bash\n" +
          "set -e\n" +
          "if [ $# != 1 ]; then\n" +
          "    echo \"usage: $0 <postgres user> \"\n" +
          "    exit\n" +
          "fi\n" +
          "echo \"Creating user account sense databases.\"\n" +
          "psql -U $1 <<'EOF'\n";

  private static final String DB_SCRIPT_MID = "EOF;\nexport PGPASSWORD='%s'\n";

  private static final String DB_SCRIPT_END =
          "for i in {0..%d}\n" +
          "do\n" +
          "   echo \"Populating schema into database nsa$i.\"\n" +
          "   psql -U %s -d nsa$i < opennsa-schema.sql\n" +
          "done";

  /**
   * Write database schema for configuration.
   *
   * @param schema
   * @param userId
   * @param password
   * @param count
   * @throws IOException
   */
  private void writeSchema(String schema, String userId, String password, int count) throws IOException {
    // Write the script to create users and databases.
    List<String> lines = new ArrayList<>();

    // Add the start of the script.
    lines.add(DB_SCRIPT_START);
    lines.add(String.format(DB_USER, userId, password));
    for (int i = 0; i < count; i++) {
      // Create the needed users and databases.
      lines.add(String.format(DB_CREATE, i, i, userId, i, i, userId, i));
    }

    // Add the sense user password for the next set of commands.
    lines.add(String.format(DB_SCRIPT_MID, password));

    // Add the last script block to load schemas into individual database.
    lines.add(String.format(DB_SCRIPT_END, count - 1, userId));

    // Write the script to file.
    write("database.sh", lines);

    // We need to copy the source OpenNSA schema.
    String sql = read(schema, Charset.defaultCharset());
    write("opennsa-schema.sql", Arrays.asList(sql));
  }

  /**
   * Write NSI-DDS peer discovery list for all simulated OpenNSA.
   *
   * @param count
   */
  private void writeDiscovery(int count) {
    // Write out the peer discovery information to configure the DDS.
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      lines.add(String.format(PEER, 9000 + i));
    }
    write("peer.xml", lines);
  }

  /**
   * Extract the list of ports from all the topologies.
   *
   * @param list
   * @return
   */
  private List<PortMap> getPortConfig(Collection<TopologyMap> list) {
    // Bidirectional ports.
    List<PortMap> biMap = new ArrayList<>();
    Map<String, PortMap> uniToBiMap = new HashMap<>();

    // For each topology we need to get the bidirectional ports,
    // list of associated VLANs (from unidirectional ports), and
    // any isAlias relationships.
    for (TopologyMap topology : list) {
      Nml nml = new Nml(topology.getDocument());
      List<NmlBidirectionalPortType> biList = nml.getBidirectionalPorts();

      // Create a reverse map from unidirectional ports to bidirectional parent.
      int inter = 0;
      for (NmlBidirectionalPortType bi : biList) {
        // We will collect all the needed configuration in this structure.
        PortMap pm = new PortMap();

        // Parse the port identifier into an STP so we can decompose the
        // elements into what will be needed by OpenNSA configuration.
        SimpleStp stp = new SimpleStp(bi.getId());

        pm.setPort(bi);
        pm.setPortId(stp.getId());
        pm.setPortName(strip(stp.getLocalId()));
        pm.setType("ethernet");
        pm.setInter("em" + Integer.toString(inter++));
        pm.setBandwidth(100000);
        pm.setNetworkId(stp.getNetworkId());
        pm.setNetworkLabel(stp.getNetworkLabel());

        biMap.add(pm);

        // Now find the unidirectional members and create an index to the port
        // map entry.
        List<Object> rest = bi.getRest();
        for (Object obj : rest) {
          if (obj instanceof JAXBElement) {
            JAXBElement<?> element = (JAXBElement<?>) obj;
            if (element.getValue() instanceof NmlPortGroupType) {
              NmlPortGroupType pg = (NmlPortGroupType) element.getValue();
              uniToBiMap.put(pg.getId(), pm);
            } else if (element.getValue() instanceof NmlPortType) {
              NmlPortType p = (NmlPortType) element.getValue();
              uniToBiMap.put(p.getId(), pm);
            }
          }
        }
      }

      // Now we index all the unidirectional inbound port groups.
      for (NmlPortGroupType pg : nml.getInboundPortGroups()) {
        try {
          // Parse the port group into a usable STP identifier.
          SimpleStp stp = new SimpleStp(pg);

          // Find the parent bidirectional port.
          PortMap bi = uniToBiMap.get(stp.getId());
          if (bi == null) {
            log.error("Could not find bidirectional port matching {} ", stp.getStpId());
            continue;
          }

          // Now we check to make sure this is a usable port label for this
          // demo.  We need a labelType == vlan and a integer label range.
          Optional<SimpleLabel> label = stp.getLabels().stream().findFirst();
          if (!label.isPresent() ||
                  (SimpleLabel.NSI_EVTS_LABEL_TYPE.contentEquals(label.get().getType()) &&
                  Strings.isNullOrEmpty(label.get().getValue()))) {
            // Invalid label sequence for OpenNSA so fudge it.
            SimpleLabel newLabel = new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "0");
            Set<SimpleLabel> labelSet = new HashSet<>();
            labelSet.add(newLabel);
            stp.setLabels(labelSet);
          }

          try {
            String labels = SimpleLabels.toString(stp.getLabels());
            if (!Strings.isNullOrEmpty(labels)) {
              bi.setLabel(labels.replace("=", ":"));
            }
          } catch (Exception ex) {
            log.error("Failed to process stpId = {} : {}", stp.getId(), ex.getLocalizedMessage());
          }

          // Look for isAlias entry.
          String isAlias = Nml.getIsAlias(pg.getRelation());
          if (!Strings.isNullOrEmpty(isAlias)) {
            bi.setIsAlias(isAlias);
          }
        } catch (IllegalArgumentException ex) {
          log.error("Skipping stp: {} : {}", ex.getMessage(), ex.getLocalizedMessage());
        }
      }

      // Now we index all the unidirectional inbound port.
      for (NmlPortType p : nml.getInboundPorts()) {
        try {
          // Parse the port into a usable STP identifier.
          SimpleStp stp = new SimpleStp(p);

          // Find the parent bidirectional port.
          PortMap bi = uniToBiMap.get(stp.getId());
          if (bi == null) {
            log.error("Could not find bidirectional port matching {} ", stp.getStpId());
            continue;
          }

          // Now we check to make sure this is a usable port label for this
          // demo.  We need a labelType == vlan and a integer label range.
          Optional<SimpleLabel> label = stp.getLabels().stream().findFirst();
          if (!label.isPresent() ||
                  (SimpleLabel.NSI_EVTS_LABEL_TYPE.contentEquals(label.get().getType()) &&
                  Strings.isNullOrEmpty(label.get().getValue()))) {
            // Invalid label sequence for OpenNSA so fudge it.
            SimpleLabel newLabel = new SimpleLabel(SimpleLabel.NSI_EVTS_LABEL_TYPE, "0");
            Set<SimpleLabel> labelSet = new HashSet<>();
            labelSet.add(newLabel);
            stp.setLabels(labelSet);
          }

          bi.setLabel(SimpleLabels.toString(stp.getLabels()).replace("=", ":"));

          // Look for isAlias entry.
          String isAlias = Nml.getIsAliasPort(p.getRelation());
          if (!Strings.isNullOrEmpty(isAlias)) {
            log.error(">>>>>>>> isAlias: " + isAlias);
            bi.setIsAlias(isAlias);
          }
        } catch (IllegalArgumentException ex) {
          log.error("Skipping stp: {} : {}", ex.getMessage(), ex.getLocalizedMessage());
        }
      }
    }

    // Now we go back over the bidirectional ports and use the inbound isAlias
    // to look up the remote bidirection port entry.
    for (PortMap bi : biMap) {
      if (!Strings.isNullOrEmpty(bi.getIsAlias())) {
        // We have an isAlias entry so find the assoicated bidirectional port.
        PortMap match = uniToBiMap.get(bi.getIsAlias());
        if (match == null) {
          log.error("Could not find bidirectional port mapping isAlias {}", bi.getIsAlias());

          // Try to derive the alias using magic.
          try {
            String replaceAll = bi.getIsAlias().replaceAll("[:-]in$", "").replaceAll("[:-]out$", "");
            SimpleStp stp = new SimpleStp(replaceAll);
            bi.setRemote(stp.getNetworkLabel() + "#" + strip(stp.getLocalId()) + "-(in|out)");
            log.error(bi.getRemote());
          } catch (IllegalArgumentException ex) {
            log.error("Bad stpId {} : {}", bi.getIsAlias(), ex.getLocalizedMessage());
          }
        } else {
          SimpleStp stp = new SimpleStp(match.getPortId());
          bi.setRemote(stp.getNetworkLabel() + "#" + strip(stp.getLocalId()) + "-(in|out)");
        }
      }
    }

    return biMap;
  }

  /**
   * Output the specified lines to the file location.
   *
   * @param outputName
   * @param lines
   * @throws IllegalArgumentException If an error outputting to device is encountered.
   */
  private void write(String outputName, List<String> lines) throws IllegalArgumentException {
    BufferedWriter writer; // Output writer for desired stream.
    try {
      // If no filename we wrtie to stdout.
      if (outputName == null) {
        writer = new BufferedWriter(new OutputStreamWriter(System.out));
      } else {
        Path path;
        if (Strings.isNullOrEmpty(outDir)) {
          path = Paths.get(outputName);
        } else {
          path = Paths.get(outDir).resolve(outputName);
        }

        writer = new BufferedWriter(new FileWriter(path.toString()));
      }

      for (String line : lines) {
        writer.write(line);
      }

      // Flush output and close device if not stdout.
      writer.flush();

      if (outputName != null) {
        writer.close();
      }
    } catch (IOException io) {
      throw new IllegalArgumentException(
              String.format("ERROR: Output file could not be created: %s\n%s\n",
                      outputName, io.getMessage())
      );
    }
  }

  /**
   * Read the specified file.
   *
   * @param path
   * @param encoding
   * @return
   * @throws IOException
   */
  private String read(String file, Charset encoding) throws IOException {
    byte[] encoded = Files.readAllBytes(Paths.get(file));
    return new String(encoded, encoding);
  }

  public static String strip(String id) {
    return id.replace(":", "_").replace("#", "_");
  }

  public static String strip_networkId(String id) {
    // We have to strip the URN bit off the front and any "topology" off the end.
    String result = id.substring(SimpleStp.NSI_NETWORK_URN_PREFIX.length()).replaceAll(":topology$", ":");

    // Now move any end topology network string after the year to the start
    // so we don't violate the URN rules.
    if (result.endsWith(":")) {
      int point = result.lastIndexOf(':');
      result = result.substring(0, point);
    } else {
      // We need to move the string.
      int point = result.lastIndexOf(':');
      String start = result.substring(point + 1, result.length());
      String end = result.substring(0, point);
      result = start + "." + end;
    }

    return result;
  }
}
