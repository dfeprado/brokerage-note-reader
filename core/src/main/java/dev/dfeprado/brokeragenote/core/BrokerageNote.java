package dev.dfeprado.brokeragenote.core;

import java.time.LocalDate;
import java.util.List;

public interface BrokerageNote {
  LocalDate getDate();

  String getBrokerName();

  String getNumber();

  List<Operation> getOps();

  String getFormattedTotalAmount();

  double getTotalAmount();

  double getFee();

  double getEmoluments();

  double getTotalFees();

  String getFormattedTotalFees();
}
