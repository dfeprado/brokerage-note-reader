package dev.dfeprado.brokeragenote.cmdrunner;

import java.io.File;
import dev.dfeprado.brokeragenote.core.BrokerageNote;
import dev.dfeprado.brokeragenote.core.input.sinacor.SinacorBrokerageNote;

class InputReader {
  public static BrokerageNote read(Arguments args, String password) throws Exception {
    File file = new File(args.getInputNote());

    return switch (args.getInputType()) {
      case SINACOR -> readSinacor(args, file, password);
    };
  }

  private static BrokerageNote readSinacor(Arguments args, File file, String password)
      throws Exception {
    return switch (args.getInputFormat()) {
      case PDF -> SinacorBrokerageNote.readPdf(file, password);
    };
  }
}
