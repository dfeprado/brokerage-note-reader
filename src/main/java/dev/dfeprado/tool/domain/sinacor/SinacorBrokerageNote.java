package dev.dfeprado.tool.domain.sinacor;

import dev.dfeprado.tool.domain.BrokerageNote;
import dev.dfeprado.tool.domain.NoteHeader;
import dev.dfeprado.tool.domain.NoteTotals;
import dev.dfeprado.tool.domain.Operation;
import dev.dfeprado.tool.exceptions.BrokerageNoteReadError;
import dev.dfeprado.tool.input.pdf.PdfReader;
import dev.dfeprado.tool.input.pdf.pdfbox.SinacorPdfBoxPdfReader;

import java.io.File;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

public class SinacorBrokerageNote implements BrokerageNote {
  public static SinacorBrokerageNote readPdf(File brokerageNoteFile) throws BrokerageNoteReadError {
    try (PdfReader reader = new SinacorPdfBoxPdfReader(brokerageNoteFile)) {
      return new SinacorBrokerageNote(reader);
    } catch (Exception e) {
      throw new BrokerageNoteReadError(e.getMessage());
    }
  }

  private final NoteHeader header;
  private final NoteTotals totals;
  private final List<Operation> operations;

  private SinacorBrokerageNote(PdfReader reader) throws BrokerageNoteReadError {
    header = reader.parseHeader();
    totals = reader.parseTotals();
    operations = Collections.unmodifiableList(reader.parseOperations());
  }

  public LocalDate getDate() {
    return header.date();
  }

  public String getBrokerName() {
    return header.brokerName();
  }

  public String getNumber() {
    return header.number();
  }

  public List<Operation> getOps() {
    return operations;
  }

  public double getTotalAmount() {
    return totals.total();
  }

  public double getFee() {
    return totals.fee();
  }

  public double getEmoluments() {
    return totals.emoluments();
  }
}
