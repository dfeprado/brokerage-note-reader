package dev.dfeprado.tool.input.pdf.pdfbox;

import static org.junit.jupiter.api.Assertions.*;

import dev.dfeprado.tool.ResourcesUtil;
import dev.dfeprado.tool.domain.NoteHeader;
import dev.dfeprado.tool.domain.NoteTotals;
import dev.dfeprado.tool.domain.Operation;
import java.io.File;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SinacorPdfBoxPdfReaderTest {
  private ResourcesUtil resourcesUtil = new ResourcesUtil();

  @Test
  public void canReadResource() throws URISyntaxException {
    File resource = resourcesUtil.getSinacorBrokerageNoteResourceFile();
    assertNotNull(resource);
  }

  @Test
  public void canOpenBrokerageNote() throws Exception {
    try (SinacorPdfBoxPdfReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile())) {
      // File was opened
    }
  }

  @Test
  public void canReadHeader() throws Exception {
    LocalDate expectedDate = LocalDate.parse("2025-04-03");
    try (SinacorPdfBoxPdfReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile())) {
      NoteHeader header = reader.parseHeader();
      assertEquals(expectedDate, header.date());
      assertEquals("108846725", header.number());
      assertEquals("RICO CORRETORA DE TITULOS E VALORES MOBILIARIOS S.A.", header.brokerName());
    }
  }

  @Test
  public void canReadFooter() throws Exception {
    try (SinacorPdfBoxPdfReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile())) {
      NoteTotals totals = reader.parseTotals();
      assertEquals(6_704.53, totals.total());
      assertEquals(1.67, totals.tax());
      assertEquals(0.33, totals.emolumentos());
    }
  }

  @Test
  public void canReadOps() throws Exception {
    try (SinacorPdfBoxPdfReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile())) {
      List<Operation> ops = reader.parseOperations();
      assertEquals(12, ops.size());

      double sum = 0d;
      for (Operation op : ops) {
        sum += op.getTotal();
      }
      double diff = sum - reader.parseTotals().total();
      if (diff < 0) {
        diff *= -1;
      }
      assertTrue(diff < 1e-2);
    }
  }
}
