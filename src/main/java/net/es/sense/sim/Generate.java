package net.es.sense.sim;

import java.io.IOException;
import javax.ws.rs.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

/**
 * Main class for the SENSE-SIM program.
 *
 * @author hacksaw
 */
@Slf4j
public class Generate {

  /**
   * Main for the SENSE-SIM program.
   *
   * @param args
   * @throws NotFoundException
   * @throws IOException
   */
  public static void main(String[] args) throws NotFoundException, IOException {

    // Create Options object to hold our command line options.
    CommandOptions options = new CommandOptions();
    try {
      // Parse the command line options.  Exception is thrown if any are missing.
      options.parse(args);
    } catch (IllegalArgumentException ex) {
      // Incorrect parameters so exit.
      exitWithError(options.getOptions());
    }

    // Configuration writer does all the heavy lifting.
    ConfigWriter cw = ConfigWriter.builder()
            .userId(options.getUserId())
            .password(options.getPassword())
            .schemaFile(options.getSchema())
            .rmFile(options.getRm())
            .ddsUrl(options.getDdsUrl())
            .outDir(options.getOut())
            .logFile(options.getLog())
            .address(options.getAddress())
            .build();
    cw.write();
  }

  /**
   * Write the command line error and terminate execution.
   *
   * @param options
   */
  static void exitWithError(Options options) {
    HelpFormatter formatter = new HelpFormatter();
    formatter.printHelp("generate.sh -dds <dds server url> ...", options);
    System.exit(0);
  }
}
