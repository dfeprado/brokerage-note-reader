package dev.dfeprado.brokeragenote.cmdrunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.stream.Collectors;
import com.beust.jcommander.JCommander;
import dev.dfeprado.brokeragenote.core.BrokerageNote;
import dev.dfeprado.brokeragenote.core.Operation;
import dev.dfeprado.brokeragenote.core.exceptions.ProtectedBrokerageNoteError;
import dev.dfeprado.brokeragenote.core.output.statusinvest.InvestmentCategory;
import dev.dfeprado.brokeragenote.core.output.statusinvest.ShareSymbol;

public class Main {
  private static Scanner scanner = new Scanner(System.in);

  public static void main(String[] args) throws Exception {
    // parses the arguments
    var arguments = new Arguments();
    var jcmd = JCommander.newBuilder().addObject(arguments).build();
    jcmd.parse(args);
    if (arguments.isHelp()) {
      jcmd.usage();
      System.exit(1);
    }

    // Reads the note
    System.out.println("Reading the note...");
    BrokerageNote note;
    String password = "";
    while (true) {
      try {
        note = InputReader.read(arguments, password);
        break;
      } catch (ProtectedBrokerageNoteError e) {
        System.out.println(e.getMessage());
        System.out.print("Please, insert note's password: ");
        password = scanner.nextLine();
      } catch (Exception e) {
        System.out.println(e instanceof ProtectedBrokerageNoteError);
        System.out.println(e.getMessage());
        throw e;
      }
    }
    System.out.println("Broker: " + note.getBrokerName());
    System.out.println("Date: " + note.getDate());
    System.out.println("Total: " + note.getFormattedTotalAmount() + " ("
        + note.getFormattedTotalFees() + " in fees)");
    System.out.println();

    // TODO checks for the share names and brokers names maps
    BrokersMap brokersMap = null;
    try {
      brokersMap = new BrokersMap(arguments.getOutputType());
      checkBrokersName(arguments, note, brokersMap);
    } catch (IOException e) {
      System.out.println("Error while processing brokers map: " + e.getMessage());
      System.exit(2);
    }

    SharesMap sharesMap = null;
    try {
      sharesMap = new SharesMap();
      checkShares(note, sharesMap);
    } catch (IOException e) {
      System.out.println("Error while processing shares map: " + e.getMessage());
      System.exit(3);
    }

    // Output
    System.out.println("Outputing...");
    new OutputWriter(arguments, note, brokersMap, sharesMap).write();
    System.out.println("Done. Have a nice day!");

  }

  private static void checkBrokersName(Arguments args, BrokerageNote note, BrokersMap map)
      throws IOException {
    String brokerName = note.getBrokerName();
    boolean askName = !map.has(brokerName) || prompt(String
        .format("Would you like to change \"%s\" alias [\"%s\"?", brokerName, map.get(brokerName)));

    if (!askName) {
      return;
    }

    String brokerAlias = null;
    while (askName) {
      System.out.printf("Write \"%s\" alias: ", brokerName);
      brokerAlias = scanner.nextLine().strip();
      if (!brokerAlias.isBlank()) {
        break;
      }
    }

    map.set(brokerName, brokerAlias);
    map.save();
  }

  private static void checkShares(BrokerageNote note, SharesMap map) throws IOException {
    Set<String> shares = new HashSet<>(
        note.getOps().stream().map(Operation::getShareName).collect(Collectors.toList()));

    var it = shares.iterator();
    String shareName = it.next();
    while (shareName != null) {
      ShareSymbol syb = map.get(shareName);

      boolean askShare = syb == null || prompt(
          String.format("Would you like to update share \"%s\" ticker and type (%s - %s)?",
              shareName, syb.ticker(), syb.category()));

      if (!askShare) {
        System.out.println("-".repeat(10));
        shareName = it.hasNext() ? it.next() : null;
        continue;
      }

      String shareTicker = null;
      InvestmentCategory category = null;
      while (true) {
        System.out.printf("Write \"%s\" shares ticker: ", shareName);
        shareTicker = scanner.nextLine().strip();
        if (shareTicker.isBlank()) {
          continue;
        }

        System.out.printf("Choose share's category: %s",
            Arrays.toString(InvestmentCategory.values()));
        try {
          category = InvestmentCategory.valueOf(scanner.nextLine().strip().toUpperCase());
        } catch (Exception e) {
          continue;
        }
        break;
      }
      map.set(shareName, new ShareSymbol(shareTicker, category));
      shareName = it.hasNext() ? it.next() : null;
      System.out.println("-".repeat(10));
    }
    map.save();
  }

  private static boolean prompt(String msg) {
    while (true) {
      System.out.printf("%s (y/N) [N]: ", msg);
      String answer = scanner.nextLine();
      if (answer.toUpperCase().equals("Y")) {
        return true;
      } else if (answer.toUpperCase().equals("N") || answer.isBlank()) {
        return false;
      }
      System.out.println();
    }
  }
}
