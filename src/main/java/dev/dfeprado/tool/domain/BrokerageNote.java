package dev.dfeprado.tool.domain;

import dev.dfeprado.tool.exceptions.BrokerageNoteReadError;
import java.time.LocalDate;
import java.util.List;

public interface BrokerageNote {
  LocalDate getDate() throws BrokerageNoteReadError;

  String getBrokerName() throws BrokerageNoteReadError;

  String getNumber() throws BrokerageNoteReadError;

  List<Operation> getOps() throws BrokerageNoteReadError;

  double getTotalAmount() throws BrokerageNoteReadError;

  double getTax() throws BrokerageNoteReadError;

  double getEmolumentos() throws BrokerageNoteReadError;
}
