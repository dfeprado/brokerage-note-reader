package dev.dfeprado.tool.domain;

public class Operation {
  private final String type;
  private final String shareName;
  private final double quantity;
  private final double price;
  double tax;
  double emolumentos;

  public Operation(
      String type, String shareName, double quantity, double price, NoteTotals totals) {
    this.type = type;
    this.shareName = shareName;
    this.quantity = quantity;
    this.price = price;

    setTaxAndEmolumentos(totals);
  }

  private void setTaxAndEmolumentos(NoteTotals footer) {
    double weight = getTotal() / footer.total();
    this.tax = footer.tax() * weight;
    this.emolumentos = footer.emolumentos() * weight;
  }

  public double getTotal() {
    return price * quantity + tax + emolumentos;
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

  public double getTax() {
    return tax;
  }

  public double getEmolumentos() {
    return emolumentos;
  }
}
