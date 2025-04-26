package dev.dfeprado.brokeragenote.core;

import java.util.List;
import dev.dfeprado.brokeragenote.core.exceptions.BrokerageNoteReadError;

public interface NoteReader extends AutoCloseable {
  NoteHeader parseHeader() throws BrokerageNoteReadError;

  List<Operation> parseOperations() throws BrokerageNoteReadError;

  NoteTotals parseTotals() throws BrokerageNoteReadError;
}
