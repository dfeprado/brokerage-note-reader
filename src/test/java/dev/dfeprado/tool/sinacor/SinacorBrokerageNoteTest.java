package dev.dfeprado.tool.sinacor;

import dev.dfeprado.tool.ResourcesUtil;
import dev.dfeprado.tool.domain.Operation;
import dev.dfeprado.tool.domain.sinacor.SinacorBrokerageNote;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SinacorBrokerageNoteTest {
  private final ResourcesUtil resources = new ResourcesUtil();

  @Test
  public void readOperations() throws Exception {
    var note = SinacorBrokerageNote.readPdf(resources.getSinacorBrokerageNoteResourceFile());
    List<Operation> ops = note.getOps();
  }
}
