package dev.dfeprado.tool.domain.sinacor;

import dev.dfeprado.tool.domain.BrokerageNote;
import dev.dfeprado.tool.domain.Operation;
import dev.dfeprado.tool.exceptions.BrokerageNoteReadError;
import dev.dfeprado.tool.input.pdf.PdfReader;
import java.time.LocalDate;
import java.util.List;

public class SinacorBrokerageNote implements BrokerageNote {
  private PdfReader reader;

  public SinacorBrokerageNote(PdfReader reader) {
    this.reader = reader;
  }

  public LocalDate getDate() throws BrokerageNoteReadError {
    return this.reader.parseHeader().date();
  }

  public String getBrokerName() throws BrokerageNoteReadError {
    return this.reader.parseHeader().brokerName();
  }

  public String getNumber() throws BrokerageNoteReadError {
    return this.reader.parseHeader().number();
  }

  public List<Operation> getOps() throws BrokerageNoteReadError {
    return reader.parseOperations();
  }

  public double getTotalAmount() throws BrokerageNoteReadError {
    return reader.parseTotals().total();
  }

  public double getTax() throws BrokerageNoteReadError {
    return reader.parseTotals().tax();
  }

  public double getEmolumentos() throws BrokerageNoteReadError {
    return reader.parseTotals().emolumentos();
  }
}
