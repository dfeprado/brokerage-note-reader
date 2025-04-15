package dev.dfeprado.tool.input.pdf.pdfbox;

import dev.dfeprado.tool.Utils;
import dev.dfeprado.tool.domain.NoteHeader;
import dev.dfeprado.tool.domain.NoteTotals;
import dev.dfeprado.tool.domain.Operation;
import dev.dfeprado.tool.exceptions.BrokerageNoteReadError;
import dev.dfeprado.tool.input.pdf.PdfReader;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripperByArea;

public class SinacorPdfBoxPdfReader implements PdfReader, AutoCloseable {
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
    stripper.addRegion(
        "footer",
        new Rectangle(
            (int) firstPage.getBBox().getWidth() / 2,
            400,
            (int) firstPage.getBBox().getWidth(),
            (int) firstPage.getBBox().getHeight()));
    stripper.addRegion(
        "operations", new Rectangle(0, 220, (int) firstPage.getBBox().getWidth(), 225));
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

      Pattern headerPattern =
          Pattern.compile(
              "NOTA DE NEGOCIAÇÃO"
                  + "\\n.*"
                  + "\\n(\\d+).+(\\d{2}/\\d{2}/\\d{4})\n"
                  + // número da nota e data
                  "(.+)\n"
                  + // nome da corretora
                  ".*",
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

  @Override
  public List<Operation> parseOperations() throws BrokerageNoteReadError {
    if (operations == null) {
      try {
        stripper.extractRegions(doc.getPage(0));
      } catch (IOException e) {
        throw new BrokerageNoteReadError(e.getMessage());
      }
      String opsText = stripper.getTextForRegion("operations");
      Pattern pattern =
          Pattern.compile("(.+?)\\s(C|D)\\s(VISTA|FRACIONARIO)\\s(.+)\\s(\\d+)\\s([0-9,]+)\\s.*");
      Matcher match = pattern.matcher(opsText);
      operations = new ArrayList<>();
      parseTotals();
      while (match.find()) {
        Operation op =
            new Operation(
                match.group(2),
                match.group(4),
                Integer.parseInt(match.group(5)),
                Utils.toNumber(match.group(6)),
                parseTotals());

        operations.add(op);
      }
    }

    return Collections.unmodifiableList(operations);
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

      Pattern pattern =
          Pattern.compile(
              "Resumo\\sFinanceiro\\n(?:.*\\n){2}Taxa\\sde\\sliquidação\\s(.+)\\sD\\n(?:.*\\n){5}Emolumentos\\s(.+)\\sD\\n(?:.*\\n){10}Líquido\\spara\\s.+?\\s(.+)\\sD\\n(.*\\n?)*",
              Pattern.MULTILINE);
      Matcher matcher = pattern.matcher(footerText);
      if (!matcher.find()) {
        throw new BrokerageNoteReadError("Could not find footer");
      }

      totals =
          new NoteTotals(
              Utils.toNumber(matcher.group(3)),
              Utils.toNumber(matcher.group(1)),
              Utils.toNumber(matcher.group(2)));
    }

    return totals;
  }
}
