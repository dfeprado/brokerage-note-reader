package dev.dfeprado.brokeragenote.core.input.sinacor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.FieldSource;
import dev.dfeprado.brokeragenote.core.NoteHeader;
import dev.dfeprado.brokeragenote.core.NoteReader;
import dev.dfeprado.brokeragenote.core.NoteTotals;
import dev.dfeprado.brokeragenote.core.Operation;
import dev.dfeprado.brokeragenote.core.ResourcesUtil;
import dev.dfeprado.brokeragenote.core.exceptions.BrokerageNoteReadError;
import dev.dfeprado.brokeragenote.core.exceptions.ProtectedBrokerageNoteError;

class SinacorPdfBoxPdfReaderTest {
  private final ResourcesUtil resourcesUtil = new ResourcesUtil();

  @Test
  void canReadResource() throws URISyntaxException {
    File resource = resourcesUtil.getSinacorBrokerageNoteResourceFile();
    assertNotNull(resource);
  }

  @Test
  void canOpenBrokerageNote() throws Exception {
    try (SinacorPdfBoxPdfReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile())) {
      // File was opened
    }
  }

  @Test
  void canReadHeader() throws Exception {
    LocalDate expectedDate = LocalDate.parse("2025-04-03");
    try (SinacorPdfBoxPdfReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile())) {
      NoteHeader header = reader.parseHeader();
      assertEquals(expectedDate, header.date());
      assertEquals("108846725", header.number());
      assertEquals("RICO CORRETORA DE TITULOS E VALORES MOBILIARIOS S.A.", header.brokerName());
    }
  }

  static List<Arguments> footerTestArgs = Arrays.asList(
      Arguments.of(ResourcesUtil.NOTE_SAMPLE_1, 6_704.53, 1.67, 0.33, 0.0, 0.0, 6_702.53),
      Arguments.of(ResourcesUtil.NOTE_SAMPLE_2, 7_972.45, 5.05, 1.01, 6_119.49, 0.30, 20_205.37));

  @ParameterizedTest
  @FieldSource("footerTestArgs")
  void canReadFooter(String noteFileName, double expectedTotal, double expectedFee,
      double expectedEmoluments, double expectedIrrfBase, double expectedIrrf,
      double expectedOpAmount) throws Exception {
    try (SinacorPdfBoxPdfReader reader = new SinacorPdfBoxPdfReader(
        resourcesUtil.getSinacorBrokerageNoteResourceFile(noteFileName))) {
      NoteTotals totals = reader.parseTotals();
      assertEquals(expectedTotal, totals.total());
      assertEquals(expectedFee, totals.fee());
      assertEquals(expectedEmoluments, totals.emoluments());
      assertEquals(expectedIrrfBase, totals.irrfBase());
      assertEquals(expectedIrrf, totals.irrf());
      assertEquals(expectedOpAmount, totals.noteOperationTotal());
    }
  }

  static List<Arguments> canReadOpsArgs = Arrays.asList(
      Arguments.of(ResourcesUtil.NOTE_SAMPLE_1, 8), Arguments.of(ResourcesUtil.NOTE_SAMPLE_2, 8));

  @ParameterizedTest
  @FieldSource("canReadOpsArgs")
  void canReadOps(String noteFileName, int expectedOpsSize) throws Exception {
    try (SinacorPdfBoxPdfReader reader = new SinacorPdfBoxPdfReader(
        resourcesUtil.getSinacorBrokerageNoteResourceFile(noteFileName))) {
      List<Operation> ops = reader.parseOperations();
      assertEquals(expectedOpsSize, ops.size());

      double sum = 0d;
      var totals = reader.parseTotals();
      for (Operation op : ops) {
        if (op.isBuy()) {
          sum += op.getTotal();
        } else {
          sum -= op.getTotal();
        }
      }
      sum += totals.fee() + totals.emoluments();
      assertTrue(areEquals(sum, reader.parseTotals().total()));
    }
  }

  @Test
  void oneOpTotals() throws Exception {
    try (NoteReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile())) {
      List<Operation> ops = reader.parseOperations();
      Operation op = ops.get(0);
      assertEquals("BRASIL ON NM", op.getShareName());
      assertEquals(16, op.getQuantity());
      assertEquals(28.44, op.getPrice());
      assertEquals(28.44 * 16, op.getTotal());
      assertEquals("0.11", String.format("%.2f", op.getFee()));
      assertEquals("0.02", String.format("%.2f", op.getEmoluments()));
      assertEquals("455.18", String.format("%.2f", op.getTotalIncludingFeesAndEmoluments()));
      assertTrue(op.isBuy());
    }
  }

  @Test
  void checkIrrfCalc() throws BrokerageNoteReadError, URISyntaxException, Exception {
    try (NoteReader reader = new SinacorPdfBoxPdfReader(
        resourcesUtil.getSinacorBrokerageNoteResourceFile(ResourcesUtil.NOTE_SAMPLE_2))) {
      List<Operation> ops = reader.parseOperations();
      Operation op = ops.get(5);
      assertTrue(op.getShareName().startsWith("FII VINCI LG"));
      assertEquals(6_119.49, op.getTotal());
      assertEquals(0.3, op.getIrrf());
    }
  }

  @Test
  void totalFeesAndEmolumentsForABuyOperation()
      throws BrokerageNoteReadError, URISyntaxException, Exception {
    try (NoteReader reader = new SinacorPdfBoxPdfReader(
        resourcesUtil.getSinacorBrokerageNoteResourceFile(ResourcesUtil.NOTE_SAMPLE_2))) {

      List<Operation> ops = reader.parseOperations();
      var op = ops.get(0);

      var expectedQuantity = 11.0;
      assertEquals(expectedQuantity, op.getQuantity());

      var expectedPrice = 158.87;
      var expectedTotal = expectedQuantity * expectedPrice;
      assertEquals(expectedTotal, op.getTotal());

      // https://www.b3.com.br/pt_br/produtos-e-servicos/tarifas/listados-a-vista-e-derivativos/renda-variavel/tarifas-de-acoes-e-fundos-de-investimento/a-vista/
      // This is the liquidation fee, B3
      var expectedFees = expectedTotal * 0.0250 / 100;
      assertAlmostEquals(expectedFees, op.getFee());

      // This is the negociation fee, B3
      var expectedEmoluments = expectedTotal * 0.0050 / 100;
      assertAlmostEquals(expectedEmoluments, op.getEmoluments());

      // Fees and emoluments can be calculated from the
      // operation amount, with the following formula:
      //
      // x = total(op) / operation_amount * total(x), where
      // x: fee/emoluments
      // op: the operation (like the buy of a stock)
      // operation_amount: the total operation amount
    }
  }

  @Test
  void totalFeesAndEmolumentsForASellOperation()
      throws BrokerageNoteReadError, URISyntaxException, Exception {
    try (NoteReader reader = new SinacorPdfBoxPdfReader(
        resourcesUtil.getSinacorBrokerageNoteResourceFile(ResourcesUtil.NOTE_SAMPLE_2))) {

      List<Operation> ops = reader.parseOperations();
      var op = ops.get(5);
      var expectedQuantity = 71.0;
      var expectedPrice = 86.19;
      assertEquals(expectedQuantity, op.getQuantity());

      var expectedTotal = expectedQuantity * expectedPrice;
      assertEquals(expectedTotal, op.getTotal());

      var expectedFee = expectedTotal * 0.0250 / 100;
      assertAlmostEquals(expectedFee, op.getFee());

      var expectedEmoluments = expectedTotal * 0.0050 / 100;
      assertAlmostEquals(expectedEmoluments, op.getEmoluments());
    }
  }

  @Test
  void openPasswordProtectedFile() throws BrokerageNoteReadError, URISyntaxException, Exception {
    try (NoteReader reader =
        new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile(
            ResourcesUtil.PROTECTED_NOTE_SAMPLE_1_123_PASSWORD), "123")) {
    }
  }

  @Test
  void shouldFailWhenPasswordProtectedFile() {
    assertThrows(ProtectedBrokerageNoteError.class, () -> {
      try (NoteReader reader =
          new SinacorPdfBoxPdfReader(resourcesUtil.getSinacorBrokerageNoteResourceFile(
              ResourcesUtil.PROTECTED_NOTE_SAMPLE_1_123_PASSWORD))) {
      }
    });
  }

  static void assertAlmostEquals(double x, double y) {
    assertTrue(areEquals(x, y), () -> String.format("Expected %f, found %f", x, y));
  }

  static boolean areEquals(double x, double y) {
    var diff = x - y;
    if (diff < 0) {
      diff *= -1;
    }
    return diff < 1e-3;
  }

}
