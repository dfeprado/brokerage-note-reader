package dev.dfeprado.brokeragenote.core.input.sinacor;

import java.io.File;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import dev.dfeprado.brokeragenote.core.BrokerageNote;
import dev.dfeprado.brokeragenote.core.NoteHeader;
import dev.dfeprado.brokeragenote.core.NoteReader;
import dev.dfeprado.brokeragenote.core.NoteTotals;
import dev.dfeprado.brokeragenote.core.Operation;
import dev.dfeprado.brokeragenote.core.exceptions.BrokerageNoteReadError;

public class SinacorBrokerageNote implements BrokerageNote {
  public static SinacorBrokerageNote readPdf(File brokerageNoteFile, String password)
      throws Exception {
    try (NoteReader reader = new SinacorPdfBoxPdfReader(brokerageNoteFile, password)) {
      return new SinacorBrokerageNote(reader);
    }
  }

  private final NoteHeader header;
  private final NoteTotals totals;
  private final List<Operation> operations;
  private final NumberFormat currencyFmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

  private SinacorBrokerageNote(NoteReader reader) throws BrokerageNoteReadError {
    header = reader.parseHeader();
    totals = reader.parseTotals();
    operations = Collections.unmodifiableList(reader.parseOperations());
  }

  @Override
  public LocalDate getDate() {
    return header.date();
  }

  @Override
  public String getBrokerName() {
    return header.brokerName();
  }

  @Override
  public String getNumber() {
    return header.number();
  }

  @Override
  public List<Operation> getOps() {
    return operations;
  }

  @Override
  public String getFormattedTotalAmount() {
    return currencyFmt.format(getTotalAmount());
  }

  @Override
  public double getTotalAmount() {
    return totals.total();
  }

  @Override
  public String getFormattedTotalFees() {
    return currencyFmt.format(getTotalFees());
  }

  @Override
  public double getTotalFees() {
    return getFee() + getEmoluments();
  }

  @Override
  public double getFee() {
    return totals.fee();
  }

  @Override
  public double getEmoluments() {
    return totals.emoluments();
  }

}
