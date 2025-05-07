package dev.dfeprado.brokeragenote.core;

public class Operation {
  private final OperationType type;
  private final String shareName;
  private double quantity;
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
    if (isBuy()) {
      return getTotal() + getFee() + getEmoluments();
    } else {
      return getTotal() - (getFee() + getEmoluments());
    }
  }

  public double getFee() {
    return totals.fee() * getTotal() / totals.noteOperationTotal();
  }

  public double getEmoluments() {
    return totals.emoluments() * getTotal() / totals.noteOperationTotal();
  }

  public double getIrrf() {
    if (isBuy()) {
      return 0.0;
    }

    double value = totals.irrf() * getTotal() / totals.irrfBase();

    return value;
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

  public void addQuantity(double quantity) {
    if (quantity < 0) {
      throw new IllegalArgumentException("Quantity cannot be less than zero.");
    }
    this.quantity += quantity;
  }

  public double getPrice() {
    return price;
  }

  @Override
  public String toString() {
    return "Operation [type=" + type + ", shareName=" + shareName + ", quantity=" + quantity
        + ", price=" + price + ", totals=" + totals + "]";
  }


}
