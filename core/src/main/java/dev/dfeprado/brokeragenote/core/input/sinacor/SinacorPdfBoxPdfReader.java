package dev.dfeprado.brokeragenote.core.input.sinacor;

import java.awt.Rectangle;
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
import org.apache.pdfbox.text.PDFTextStripperByArea;
import dev.dfeprado.brokeragenote.core.NoteHeader;
import dev.dfeprado.brokeragenote.core.NoteReader;
import dev.dfeprado.brokeragenote.core.NoteTotals;
import dev.dfeprado.brokeragenote.core.Operation;
import dev.dfeprado.brokeragenote.core.OperationType;
import dev.dfeprado.brokeragenote.core.Utils;
import dev.dfeprado.brokeragenote.core.exceptions.BrokerageNoteReadError;

class SinacorPdfBoxPdfReader implements NoteReader {
  private static final DateTimeFormatter sinacorDateFormat = DateTimeFormatter.ofPattern("d/M/y");
  private final PDDocument doc;
  private final PDFTextStripperByArea stripper;
  private NoteHeader header;
  private NoteTotals totals;
  private List<Operation> operations;

  public SinacorPdfBoxPdfReader(File file) throws BrokerageNoteReadError {
    try {
      this.doc = Loader.loadPDF(file);
    } catch (IOException e) {
      throw new BrokerageNoteReadError(e.getMessage());
    }
    PDPage firstPage = doc.getPage(0);

    // prepare regions
    try {
      stripper = new PDFTextStripperByArea();
    } catch (IOException e) {
      throw new BrokerageNoteReadError(e.getMessage());
    }
    stripper.setSortByPosition(true);
    stripper.addRegion("header", new Rectangle(0, 0, (int) firstPage.getBBox().getWidth(), 90));
    stripper.addRegion("footer", new Rectangle((int) firstPage.getBBox().getWidth() / 2, 400,
        (int) firstPage.getBBox().getWidth(), (int) firstPage.getBBox().getHeight()));
    stripper.addRegion("operations",
        new Rectangle(0, 220, (int) firstPage.getBBox().getWidth(), 225));
  }

  @Override
  public void close() throws Exception {
    doc.close();
  }

  @Override
  public NoteHeader parseHeader() throws BrokerageNoteReadError {
    if (header == null) {
      try {
        stripper.extractRegions(doc.getPage(0));
      } catch (IOException e) {
        throw new BrokerageNoteReadError(e.getMessage());
      }
      String headerText = stripper.getTextForRegion("header");

      // 1 = number
      // 2 = date
      // 3 = broker name
      Pattern headerPattern = Pattern.compile(
          "NOTA DE NEGOCIAÇÃO" + "\\n.*" + "\\n(\\d+).+(\\d{2}/\\d{2}/\\d{4})\n" + "(.+)\n.*",
          Pattern.MULTILINE);
      Matcher matcher = headerPattern.matcher(headerText);
      if (!matcher.find()) {
        throw new BrokerageNoteReadError("Could not find header");
      }

      header = new NoteHeader(matcher.group(3), matcher.group(1),
          LocalDate.parse(matcher.group(2), sinacorDateFormat));
    }

    return header;
  }

  @Override
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
        String opsText = stripper.getTextForRegion("operations");
        // System.out.println(opsText);
        // 1 = NOT USED
        // 2 = Operation type (C/V)
        // 3 = Market type (VISTA/FRACIONARIO)
        // 4 = share name
        // 5 = quantity
        // 6 = price
        Pattern pattern = Pattern.compile(
            "(.+?)\\s(C|V)\\s(VISTA|FRACIONARIO)\\s(.+?)(?:\\s#)?\\s(\\d+)\\s([0-9,]+)\\s.*");
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
            operationGroupedByPrice.put(operationKey,
                new Operation(
                    match.group(2).matches("[cC]") ? OperationType.BUY : OperationType.SELL,
                    shareName, quantity, price, parseTotals()));
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
      String footerText = stripper.getTextForRegion("footer");
      // System.out.println(footerText);

      // 1 = fees
      // 2 = emoluments
      // 3 = Base IRRF
      // 4 = IRRF
      // 5 = total (including fees and emoluments)
      Pattern pattern = Pattern.compile(
          "Resumo\\sFinanceiro\\n(?:.*\\n){2}Taxa\\sde\\sliquidação\\s(.+)\\sD\\n(?:.*\\n){5}Emolumentos\\s(.+)\\sD\\n(?:.*\\n){7}I.R.R.F.\\ss\\/\\soperações,\\sbase\\sR\\$([0-9.,]+)\\s([0-9.,]+)\\n(?:.*\\n){2}Líquido\\spara\\s.+?\\s(.+)\\sD",
          Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(footerText);
      if (!matcher.find()) {
        throw new BrokerageNoteReadError("Could not find footer");
      }

      totals = new NoteTotals(Utils.toNumber(matcher.group(5)), Utils.toNumber(matcher.group(1)),
          Utils.toNumber(matcher.group(2)), Utils.toNumber(matcher.group(3)),
          Utils.toNumber(matcher.group(4)));
    }

    return totals;
  }
}
