package dev.dfeprado.brokeragenote.cmdrunner;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import dev.dfeprado.brokeragenote.core.BrokerageNote;
import dev.dfeprado.brokeragenote.core.output.OutputNote;
import dev.dfeprado.brokeragenote.core.output.statusinvest.StatusInvestOutput;

public class OutputWriter {
  private Arguments args;
  private BrokerageNote note;
  private BrokersMap brokersMap;
  private SharesMap sharesMap;

  public OutputWriter(Arguments args, BrokerageNote note, BrokersMap brokersMap,
      SharesMap sharesMap) {
    super();
    this.args = args;
    this.note = note;
    this.brokersMap = brokersMap;
    this.sharesMap = sharesMap;
  }

  public OutputNote write() throws FileNotFoundException, IOException {
    return switch (args.getOutputType()) {
      case STATUSINVEST -> {
        var result = new StatusInvestOutput(note, sharesMap.getMap(), brokersMap.getMap());
        result.writeToXslx(new FileOutputStream(args.getOutputFile()));
        yield result;
      }
    };
  }
}
