package net.es.sense.sim;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Configure and process the SENSE-SIM command line options.
 *
 * @author hacksaw
 */
@Slf4j
public class CommandOptions {
  // Location of the SENSE RM configuration file template.
  private static final String SENSE_RM_CONFIG_FILE = "src/main/resources/sense-rm.yaml";
  private static final String SCHEMA_FILE = "src/main/resources/schema.sql";
  private static final String SENSE_RM_LOG_FILE = "src/main/resources/logback.xml";
  private static final String NSA_PEERS_FILE = "src/main/resources/peers.yaml";
  private static final String SENSE_ADDRESS = "localhost";

  public static final String DDS = "dds";
  public static final String USER = "user";
  public static final String PASSWORD = "pwd";
  public static final String SCHEMA = "schema";
  public static final String RM = "rm";
  public static final String LOG = "log";
  public static final String OUT = "out";
  public static final String ADDRESS = "addr";
  public static final String PEERS = "peers";

  private CommandLine clp;
  private final Options commandOptions;

  public CommandOptions() {
    commandOptions = getCommandOptions();
  }

  /**
   * Parse the command line options.
   *
   * @param args The command line arguments.
   * @throws IllegalArgumentException If there are missing parameters.
   */
  public void parse(String[] args) throws IllegalArgumentException {
    // Parse the command line options.
    CommandLineParser parser = new DefaultParser();

    try {
      clp = parser.parse(commandOptions, args);
    } catch (ParseException pe) {
      log.error("Error: You did not provide the correct arguments.");
      throw new IllegalArgumentException("Error: Invlaid arguments.");
    }
  }

  /**
   * Build the command line options.
   *
   * @return
   */
  private Options getCommandOptions() {
    // Create Options object to hold our command line options.
    Options options = new Options();

    Option ddsServer = new Option(DDS, true, "DDS server URL.");
    ddsServer.setRequired(true);
    options.addOption(ddsServer);

    Option user = new Option(USER, true, "Database user identifier for use by SENSE and OpenNSA.");
    user.setRequired(true);
    options.addOption(user);

    Option password = new Option(PASSWORD, true, "Database user password for use by SENSE and OpenNSA.");
    password.setRequired(true);
    options.addOption(password);

    Option schema = new Option(SCHEMA, true, "Location of OpenNSA database schema file.");
    schema.setOptionalArg(true);
    options.addOption(schema);

    Option rm = new Option(RM, true, "SENSE-NSI-RM configuration template.");
    rm.setOptionalArg(true);
    options.addOption(rm);

    Option out = new Option(OUT, true, "Directory to write genrated files.");
    rm.setOptionalArg(true);
    options.addOption(out);

    Option logfile = new Option(LOG, true, "Location of SENSE-RM log file template.");
    rm.setOptionalArg(true);
    options.addOption(logfile);

    Option addr = new Option(ADDRESS, true, "Address to bind SENSE-RM REST endpoint.");
    rm.setOptionalArg(true);
    options.addOption(addr);

    Option peers = new Option(PEERS, true, "File used to specify additional NSA port adjacencies.");
    rm.setOptionalArg(true);
    options.addOption(peers);

    return options;
  }

  /**
   *
   * @return
   */
  public Options getOptions() {
    return commandOptions;
  }

  /**
   *
   * @return
   */
  public String getDdsUrl() {
    return clp.getOptionValue(DDS);
  }

  /**
   *
   * @return
   */
  public String getUserId() {
    return clp.getOptionValue(USER);
  }

  /**
   *
   * @return
   */
  public String getPassword() {
    return clp.getOptionValue(PASSWORD);
  }

  /**
   *
   * @return
   */
  public String getSchema() {
    if (clp.hasOption(SCHEMA)) {
      return clp.getOptionValue(SCHEMA);
    }

    return SCHEMA_FILE;
  }

  /**
   *
   * @return
   */
  public String getRm() {
    if (clp.hasOption(RM)) {
      return clp.getOptionValue(RM);
    }
    return SENSE_RM_CONFIG_FILE;
  }

  /**
   *
   * @return
   */
  public String getOut() {
    if (clp.hasOption(OUT)) {
      return clp.getOptionValue(OUT);
    }
    return "";
  }

  /**
   *
   * @return
   */
  public String getLog() {
    if (clp.hasOption(LOG)) {
      return clp.getOptionValue(LOG);
    }
    return SENSE_RM_LOG_FILE;
  }

  /**
   *
   * @return
   */
  public String getAddress() {
    if (clp.hasOption(ADDRESS)) {
      return clp.getOptionValue(ADDRESS);
    }
    return SENSE_ADDRESS;
  }

  /**
   *
   * @return
   */
  public String getPeers() {
    if (clp.hasOption(PEERS)) {
      return clp.getOptionValue(PEERS);
    }
    return NSA_PEERS_FILE;
  }
}
