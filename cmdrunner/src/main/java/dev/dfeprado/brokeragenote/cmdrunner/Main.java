package dev.dfeprado.brokeragenote.cmdrunner;

import com.beust.jcommander.JCommander;
import dev.dfeprado.brokeragenote.core.NoteHeader;
import dev.dfeprado.brokeragenote.core.Operation;
import dev.dfeprado.brokeragenote.core.exceptions.ProtectedBrokerageNoteError;
import dev.dfeprado.brokeragenote.core.input.SinacorReader;
import dev.dfeprado.brokeragenote.core.output.statusinvest.InvestmentCategory;
import dev.dfeprado.brokeragenote.core.output.statusinvest.ShareSymbol;
import dev.dfeprado.brokeragenote.core.output.statusinvest.StatusInvestOutput;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
        SinacorReader note;
        String password = "";
        var inputFile = new File(arguments.getInputNote());
        while (true) {
            try {
                note = new SinacorReader(inputFile, password);
                break;
            } catch (ProtectedBrokerageNoteError e) {
                System.out.println(e.getMessage());
                System.out.print("Please, insert note's password: ");
                password = scanner.nextLine();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                throw e;
            }
        }

        final var header = note.parseHeader();
        final var totals = note.parseTotals();

        System.out.println("Broker: " + header.brokerName());
        System.out.println("Date: " + header.date());
        System.out.println("Total: " + totals.noteOperationTotal() + " ("
                + totals.emoluments() + totals.fee() + " in fees)");
        System.out.println();

        // TODO checks for the share names and brokers names maps
        BrokersMap brokersMap = null;
        try {
            brokersMap = new BrokersMap(OutputFormat.SINACOR);
            checkBrokersName(header, brokersMap);
        } catch (IOException e) {
            System.out.println("Error while processing brokers map: " + e.getMessage());
            System.exit(2);
        }

        SharesMap sharesMap = null;
        final var operations = note.parseOperations();
        try {
            sharesMap = new SharesMap();
            checkShares(operations, sharesMap);
        } catch (IOException e) {
            System.out.println("Error while processing shares map: " + e.getMessage());
            System.exit(3);
        }

        // Output
        System.out.println("Outputing...");
        final var statusinvest = new StatusInvestOutput(note, sharesMap.getMap(), brokersMap.getMap());
        statusinvest.writeToXslx(new FileOutputStream(arguments.getOutputFile()));

        // Output validation
        statusinvest.summarizeCreatedFile(new File(arguments.getOutputFile()));
        System.out.println("Done. Have a nice day!");

    }

    private static void checkBrokersName(NoteHeader noteHeader, BrokersMap map)
            throws IOException {
        String brokerName = noteHeader.brokerName();
        boolean askName = !map.has(brokerName) || prompt(String
                .format("Would you like to change \"%s\" alias [\"%s\"?", brokerName, map.get(brokerName)));

        if (!askName) {
            return;
        }

        String brokerAlias = null;
        do {
            System.out.printf("Write \"%s\" alias: ", brokerName);
            brokerAlias = scanner.nextLine().strip();
        } while (brokerAlias.isBlank());

        map.set(brokerName, brokerAlias);
        map.save();
    }

    private static void checkShares(List<Operation> operations, SharesMap map) throws IOException {
        Set<String> shares = operations.stream().map(Operation::getShareName).collect(Collectors.toSet());

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
            if (answer.equalsIgnoreCase("Y")) {
                return true;
            } else if (answer.equalsIgnoreCase("N") || answer.isBlank()) {
                return false;
            }
            System.out.println();
        }
    }
}
