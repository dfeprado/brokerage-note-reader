package dev.dfeprado.tool.domain;

import java.time.LocalDate;
import java.util.List;

public interface BrokerageNote {
  LocalDate getDate();

  String getBrokerName();

  String getNumber();

  List<Operation> getOps();

  double getTotalAmount();

  double getFee();

  double getEmoluments();
}
