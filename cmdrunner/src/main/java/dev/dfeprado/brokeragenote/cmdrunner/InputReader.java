package dev.dfeprado.brokeragenote.cmdrunner;

import java.io.File;
import dev.dfeprado.brokeragenote.core.BrokerageNote;
import dev.dfeprado.brokeragenote.core.exceptions.BrokerageNoteReadError;
import dev.dfeprado.brokeragenote.core.input.sinacor.SinacorBrokerageNote;

class InputReader {
  public static BrokerageNote read(Arguments args) throws BrokerageNoteReadError {
    File file = new File(args.getInputNote());

    return switch (args.getInputType()) {
      case SINACOR -> readSinacor(args, file);
    };
  }

  private static BrokerageNote readSinacor(Arguments args, File file)
      throws BrokerageNoteReadError {
    return switch (args.getInputFormat()) {
      case PDF -> SinacorBrokerageNote.readPdf(file);
    };
  }
}
