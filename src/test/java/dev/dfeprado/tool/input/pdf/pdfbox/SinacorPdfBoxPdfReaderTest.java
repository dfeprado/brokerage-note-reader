package dev.dfeprado.tool.input.pdf.pdfbox;

import static org.junit.jupiter.api.Assertions.*;

import dev.dfeprado.tool.ResourcesUtil;
import dev.dfeprado.tool.domain.NoteHeader;
import dev.dfeprado.tool.domain.NoteTotals;
import dev.dfeprado.tool.domain.Operation;
import dev.dfeprado.tool.input.pdf.PdfReader;
import java.io.File;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SinacorPdfBoxPdfReaderTest {
  private final ResourcesUtil resourcesUtil = new ResourcesUtil();

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
      assertEquals(1.67, totals.fee());
      assertEquals(0.33, totals.emoluments());
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
        sum += op.getTotalIncludingFeesAndEmoluments();
      }
      double diff = sum - reader.parseTotals().total();
      if (diff < 0) {
        diff *= -1;
      }
      assertTrue(diff < 1e-2);
    }
  }

  @Test
  public void oneOpTotals() throws Exception {
    try (PdfReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile())) {
      List<Operation> ops = reader.parseOperations();
      Operation op = ops.get(0);
      assertTrue(op.getShareName().startsWith("BRASIL"));
      assertEquals(16, op.getQuantity());
      assertEquals(28.44, op.getPrice());
      assertEquals(28.44 * 16, op.getTotal());
      assertEquals("0.11", String.format("%.2f", op.getFee()));
      assertEquals("0.02", String.format("%.2f", op.getEmoluments()));
      assertEquals("455.18", String.format("%.2f", op.getTotalIncludingFeesAndEmoluments()));
    }
  }
}
