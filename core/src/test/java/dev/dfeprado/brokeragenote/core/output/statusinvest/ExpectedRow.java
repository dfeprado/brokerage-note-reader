package dev.dfeprado.brokeragenote.core.output.statusinvest;

public record ExpectedRow(String category, String ticker, char operation, int quantity,
    double price, String broker, double fees, double tax, double irrf) {

  static class Builder {
    // Persistent options
    private String category;
    private char operation;
    private String broker;
    private double noteOperationAmount;
    private double totalFees;
    private double irrfBase;
    private String ticker;

    // Volatile options
    private int quantity;
    private double price;
    private double irrf;

    public Builder(String broker, double noteOperationAmount, double totalFees, double irrfBase,
        double totalIrrf) {
      this.broker = broker;
      this.noteOperationAmount = noteOperationAmount;
      this.totalFees = totalFees;
      this.irrfBase = irrfBase;
      this.irrf = totalIrrf;
    }

    public Builder setCategoryAndOperation(String category, char operation) {
      this.category = category;
      this.operation = operation;
      return this;
    }

    public Builder setTicker(String ticker) {
      this.ticker = ticker;
      return this;
    }

    public Builder setQuantityAndPrice(int quantity, double price) {
      this.quantity = quantity;
      this.price = price;
      return this;
    }

    public ExpectedRow build() {
      double opTotal = quantity * price;
      var fees = opTotal / noteOperationAmount * totalFees;
      var opIrrf = operation == 'V' ? opTotal / irrfBase * irrf : 0.0;
      return new ExpectedRow(category, ticker, operation, quantity, price, broker, fees, 0.0,
          opIrrf);
    }
  }
}
