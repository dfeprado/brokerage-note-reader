package dev.dfeprado.brokeragenote.core.output.statusinvest;

import java.io.Serializable;

public record ShareSymbol(String ticker, InvestmentCategory category) implements Serializable {
}
