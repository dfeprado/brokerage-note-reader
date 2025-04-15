package dev.dfeprado.tool.sinacor;

import dev.dfeprado.tool.ResourcesUtil;
import dev.dfeprado.tool.domain.Operation;
import dev.dfeprado.tool.domain.sinacor.SinacorBrokerageNote;
import dev.dfeprado.tool.input.pdf.pdfbox.SinacorPdfBoxPdfReader;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SinacorBrokerageNoteTest {
  private ResourcesUtil resources = new ResourcesUtil();

  @Test
  public void readOperations() throws Exception {
    try (SinacorPdfBoxPdfReader reader =
        new SinacorPdfBoxPdfReader(resources.getSinacorBrokerageNoteResourceFile())) {
      SinacorBrokerageNote note = new SinacorBrokerageNote(reader);
      List<Operation> ops = note.getOps();
    }
  }
}
