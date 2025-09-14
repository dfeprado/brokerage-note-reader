package dev.dfeprado.brokeragenote.core;

public record NoteTotals(
    double total,
    double fee,
    double emoluments,
    double irrfBase,
    double irrf,
    double noteOperationTotal,
    double transferTax) {
  public double getTotalEmoluments() {
    return emoluments + transferTax;
  }
}
