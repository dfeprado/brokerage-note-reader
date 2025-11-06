package dev.dfeprado.brokeragenote.core.input;

import dev.dfeprado.brokeragenote.core.*;
import dev.dfeprado.brokeragenote.core.exceptions.BrokerageNoteReadError;
import dev.dfeprado.brokeragenote.core.exceptions.ProtectedBrokerageNoteError;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripperByArea;

public class SinacorReader implements AutoCloseable {
  private static final DateTimeFormatter sinacorDateFormat = DateTimeFormatter.ofPattern("d/M/y");
  private static final String HEADER_REGION = "head";
  private static final String NEGOCIATION_SUMMARY_REGION = "negSumma";
  private static final String FINANCIAL_SUMMARY_REGION = "finSumma";
  private static final String OPERATIONS_REGION = "ops";

  private final PDDocument doc;
  private PDFTextStripperByArea stripper;
  private NoteHeader header;
  private NoteTotals totals;
  private List<Operation> operations;

  public SinacorReader(File file) throws BrokerageNoteReadError {
    this(file, "");
  }

  public SinacorReader(File file, String password) throws BrokerageNoteReadError {
    try {
      this.doc = Loader.loadPDF(file, password);
    } catch (InvalidPasswordException e) {
      throw new ProtectedBrokerageNoteError(e.getMessage());
    } catch (IOException e) {
      throw new BrokerageNoteReadError(e.getMessage());
    }

    // prepare regions
    PDPage firstPage = doc.getPage(0);
    try {
      stripper = new PDFTextStripperByArea();
    } catch (IOException e) {
      throw new BrokerageNoteReadError(e.getMessage());
    }
    stripper.setSortByPosition(true);
    stripper.addRegion(
        HEADER_REGION, new Rectangle(0, 0, (int) firstPage.getBBox().getWidth(), 90));
    stripper.addRegion(
        NEGOCIATION_SUMMARY_REGION,
        new Rectangle(
            0,
            400,
            (int) firstPage.getBBox().getWidth() / 2,
            (int) firstPage.getBBox().getHeight()));
    stripper.addRegion(
        FINANCIAL_SUMMARY_REGION,
        new Rectangle(
            (int) firstPage.getBBox().getWidth() / 2,
            400,
            (int) firstPage.getBBox().getWidth(),
            (int) firstPage.getBBox().getHeight()));
    stripper.addRegion(
        OPERATIONS_REGION, new Rectangle(0, 220, (int) firstPage.getBBox().getWidth(), 225));
  }

  @Override
  public void close() throws Exception {
    doc.close();
  }

  public NoteHeader parseHeader() throws BrokerageNoteReadError {
    if (header == null) {
      try {
        stripper.extractRegions(doc.getPage(0));
      } catch (IOException e) {
        throw new BrokerageNoteReadError(e.getMessage());
      }
      String headerText = stripper.getTextForRegion(HEADER_REGION);

      // 1 = number
      // 2 = date
      // 3 = broker name
      Pattern headerPattern =
          Pattern.compile(
              "NOTA DE NEGOCIAÇÃO"
                  + "\\r?\\n.*"
                  + "\\r?\\n(\\d+).+(\\d{2}/\\d{2}/\\d{4})\\r?\\n"
                  + "(.+)\\r?\\n.*",
              Pattern.MULTILINE);
      Matcher matcher = headerPattern.matcher(headerText);
      if (!matcher.find()) {
        throw new BrokerageNoteReadError("Could not find header");
      }

      header =
          new NoteHeader(
              matcher.group(3),
              matcher.group(1),
              LocalDate.parse(matcher.group(2), sinacorDateFormat));
    }

    return header;
  }

  public List<Operation> parseOperations() throws BrokerageNoteReadError {
    if (operations == null) {
      // I use a Map so I can group operations of a share with the same price together,
      // reducing the number of total operations.
      Map<String, Operation> operationGroupedByPrice = new LinkedHashMap<>();
      for (int page = 0; page < doc.getNumberOfPages(); page++) {
        try {
          stripper.extractRegions(doc.getPage(page));
        } catch (IOException e) {
          throw new BrokerageNoteReadError(e.getMessage());
        }
        String opsText = stripper.getTextForRegion(OPERATIONS_REGION);
        // System.out.println(opsText);
        // 1 = NOT USED
        // 2 = Operation type (C/V)
        // 3 = Market type (VISTA/FRACIONARIO)
        // 4 = share name
        // 5 = quantity
        // 6 = price
        Pattern pattern =
            Pattern.compile(
                "(.+?)\\s(C|V)\\s(VISTA|FRACIONARIO)\\s(.+?)(?:\\s@?#?)?(?:\\s#)?\\s(\\d+)\\s([0-9,]+)\\s.*");
        Matcher match = pattern.matcher(opsText);
        parseTotals();
        while (match.find()) {
          var price = Utils.toNumber(match.group(6));
          var shareName = match.group(4).replaceAll("\s+", " ");
          // I use the sharename + it's price (removing decimal places)
          // as the key to the map
          var operationKey = shareName + (int) (price * 100);
          var quantity = Integer.parseInt(match.group(5));

          if (operationGroupedByPrice.containsKey(operationKey)) {
            operationGroupedByPrice.get(operationKey).addQuantity(quantity);
          } else {
            operationGroupedByPrice.put(
                operationKey,
                new Operation(
                    match.group(2).matches("[cC]") ? OperationType.BUY : OperationType.SELL,
                    shareName,
                    quantity,
                    price,
                    parseTotals()));
          }
        }
      }
      operations = List.copyOf(operationGroupedByPrice.values());
    }

    return operations;
  }

  public NoteTotals parseTotals() throws BrokerageNoteReadError {
    if (totals == null) {
      PDPage lastPage = doc.getPage(doc.getNumberOfPages() - 1);
      try {
        stripper.extractRegions(lastPage);
      } catch (IOException e) {
        throw new BrokerageNoteReadError(e.getMessage());
      }

      String financialSummaryText = stripper.getTextForRegion(FINANCIAL_SUMMARY_REGION);
      var total = extractValueOfField("Líquido\\spara\\s.+?\\s(.+)\\s(?:D|C)", financialSummaryText);
      var fee = extractValueOfField("Taxa de liquidação\\s(.+)\\sD", financialSummaryText);
      var emoluments = extractValueOfField("Emolumentos\\s(.+)\\sD", financialSummaryText);
      var transferTax =
          extractOptionalValueOfField(
              "Taxa\\sde\\sTransf\\.\\sde\\sAtivos\\s(.+)\\sD", financialSummaryText);
      var irrfBase =
          extractValueOfField(
              "I\\.R\\.R\\.F\\.\\ss\\/\\soperações,\\sbase\\sR\\$(.+?)\\s.+", financialSummaryText);
      var irrf =
          extractValueOfField(
              "I\\.R\\.R\\.F\\.\\ss\\/\\soperações,\\sbase.+?\\s(.+)", financialSummaryText);

      String negociationSummaryText = stripper.getTextForRegion(NEGOCIATION_SUMMARY_REGION);
      var opAmount =
          extractValueOfField("Valor\\sdas\\soperações\\s([0-9,.]+)", negociationSummaryText);

      totals = new NoteTotals(total, fee, emoluments, irrfBase, irrf, opAmount, transferTax);
    }

    return totals;
  }

  private double extractOptionalValueOfField(String regex, String content) {
    try {
      return extractValueOfField(regex, content);
    } catch (BrokerageNoteReadError e) {
      return 0D;
    }
  }

  private double extractValueOfField(String regex, String content) throws BrokerageNoteReadError {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(content);
    if (!matcher.find()) {
      throw new BrokerageNoteReadError("Could not find field " + regex + " from brokerage totals");
    }
    return Utils.toNumber(matcher.group(1));
  }
}
