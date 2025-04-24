package dev.dfeprado.tool.domain;

public class Operation {
  private final String type;
  private final String shareName;
  private final double quantity;
  private final double price;
  private final NoteTotals totals;

  //  double tax;
  //  double emoluments;

  public Operation(
      String type, String shareName, double quantity, double price, NoteTotals totals) {
    this.type = type;
    this.shareName = shareName;
    this.quantity = quantity;
    this.price = price;
    this.totals = totals;

    //    setTaxAndEmolumentos(totals);
  }

  //  private void setTaxAndEmolumentos(NoteTotals totals) {
  //    double weight = getTotal() / totals.total();
  //    this.tax = totals.fee() * weight;
  //    this.emoluments = totals.emoluments() * weight;
  //  }

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
    return type.equals("C");
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
