package dev.dfeprado.tool.input.pdf;

import dev.dfeprado.tool.domain.NoteHeader;
import dev.dfeprado.tool.domain.NoteTotals;
import dev.dfeprado.tool.domain.Operation;
import dev.dfeprado.tool.exceptions.BrokerageNoteReadError;
import java.util.List;

public interface PdfReader {
  NoteHeader parseHeader() throws BrokerageNoteReadError;

  List<Operation> parseOperations() throws BrokerageNoteReadError;

  NoteTotals parseTotals() throws BrokerageNoteReadError;
}
