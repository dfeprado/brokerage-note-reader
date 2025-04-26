package dev.dfeprado.brokeragenote.core;

public class Operation {
  private final OperationType type;
  private final String shareName;
  private final double quantity;
  private final double price;
  private final NoteTotals totals;

  public Operation(OperationType type, String shareName, double quantity, double price,
      NoteTotals totals) {
    this.type = type;
    this.shareName = shareName;
    this.quantity = quantity;
    this.price = price;
    this.totals = totals;
  }

  public double getTotalIncludingFeesAndEmoluments() {
    return getTotal() + getFee() + getEmoluments();
  }

  public double getFee() {
    return totals.fee() * getTotal() / totals.total();
  }

  public double getEmoluments() {
    return totals.emoluments() * getTotal() / totals.total();
  }

  public double getTotal() {
    return price * quantity;
  }

  public boolean isBuy() {
    return type == OperationType.BUY;
  }

  public String getShareName() {
    return shareName;
  }

  public double getQuantity() {
    return quantity;
  }

  public double getPrice() {
    return price;
  }
}
