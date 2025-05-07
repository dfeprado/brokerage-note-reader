package dev.dfeprado.brokeragenote.core.output.statusinvest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import dev.dfeprado.brokeragenote.core.BrokerageNote;
import dev.dfeprado.brokeragenote.core.ResourcesUtil;
import dev.dfeprado.brokeragenote.core.input.sinacor.SinacorBrokerageNote;

public class StatusInvestOutputTest {
  private ResourcesUtil resources = new ResourcesUtil();

  private Map<String, String> brokerMap =
      Map.of("RICO CORRETORA DE TITULOS E VALORES MOBILIARIOS S.A.", "RICO INVESTIMENTOS");
  private Map<String, ShareSymbol> shareMap =
      Map.of("BRASIL ON NM", new ShareSymbol("BBAS3", InvestmentCategory.ACAO), "CVC BRASIL ON NM",
          new ShareSymbol("CVCB3", InvestmentCategory.ACAO), "GERDAU ON N1",
          new ShareSymbol("GGBR3", InvestmentCategory.ACAO), "ITAUSA PN N1",
          new ShareSymbol("ITSA4", InvestmentCategory.ACAO), "SANEPAR ON N2",
          new ShareSymbol("SAPR3", InvestmentCategory.ACAO), "TAESA ON N2",
          new ShareSymbol("TAEE3", InvestmentCategory.ACAO), "VALE ON NM",
          new ShareSymbol("VALE3", InvestmentCategory.ACAO), "FII VINCI LG VILG11 CI ER",
          new ShareSymbol("VILG11", InvestmentCategory.FII), "FII HGLG PAX HGLG11 CI ER",
          new ShareSymbol("HGLG11", InvestmentCategory.FII), "FII MAXI REN MXRF11 CI ER",
          new ShareSymbol("MXRF11", InvestmentCategory.FII));

  @Test
  public void testCsvOutput() throws Exception {
    BrokerageNote note =
        SinacorBrokerageNote.readPdf(resources.getSinacorBrokerageNoteResourceFile(), "");

    var statusinvest = new StatusInvestOutput(note, shareMap, brokerMap);
    String expectedOutput =
        """
            Data operação;Categoria;Código Ativo;Operação C/V;Quantidade;Preço unitário;Corretora;Corretagem;Taxas;Impostos;IRRF\r
            03/04/2025;Ações;BBAS3;C;16;28,44;RICO INVESTIMENTOS;0,00;0,14;0,00;0,00\r
            03/04/2025;Ações;CVCB3;C;14;2,16;RICO INVESTIMENTOS;0,00;0,01;0,00;0,00\r
            03/04/2025;Ações;GGBR3;C;72;15,55;RICO INVESTIMENTOS;0,00;0,33;0,00;0,00\r
            03/04/2025;Ações;ITSA4;C;111;9,67;RICO INVESTIMENTOS;0,00;0,32;0,00;0,00\r
            03/04/2025;Ações;SAPR3;C;300;5,70;RICO INVESTIMENTOS;0,00;0,51;0,00;0,00\r
            03/04/2025;Ações;SAPR3;C;37;5,71;RICO INVESTIMENTOS;0,00;0,06;0,00;0,00\r
            03/04/2025;Ações;TAEE3;C;87;11,43;RICO INVESTIMENTOS;0,00;0,30;0,00;0,00\r
            03/04/2025;Ações;VALE3;C;20;55,43;RICO INVESTIMENTOS;0,00;0,33;0,00;0,00\r
            """;

    assertEquals(expectedOutput, statusinvest.getAsCsv());
  }

  @Test
  public void textXlsxOutputForSample1() throws Exception {
    BrokerageNote note =
        SinacorBrokerageNote.readPdf(resources.getSinacorBrokerageNoteResourceFile(), "");

    var statusinvest = new StatusInvestOutput(note, shareMap, brokerMap);
    File testFile = new File("/tmp/test.xlsx");
    FileOutputStream output = new FileOutputStream(testFile);
    statusinvest.writeToXslx(output);

    try (InputStream testStream = new FileInputStream(testFile)) {
      Workbook wb = WorkbookFactory.create(testStream);
      Sheet sheet = wb.getSheetAt(0);

      ExpectedRow.Builder builder =
          new ExpectedRow.Builder("RICO INVESTIMENTOS", 6_702.53, 1.67 + .33, 0.0, 0.0);

      ExpectedRow[] expectedRows = {
          builder.setCategoryAndOperation("Ações", 'C').setTicker("BBAS3")
              .setQuantityAndPrice(16, 28.44).build(),
          builder.setTicker("CVCB3").setQuantityAndPrice(14, 2.16).build(),
          builder.setTicker("GGBR3").setQuantityAndPrice(22 + 47 + 3, 15.55).build(),
          builder.setTicker("ITSA4").setQuantityAndPrice(111, 9.67).build(),
          builder.setTicker("SAPR3").setQuantityAndPrice(300, 5.70).build(),
          builder.setQuantityAndPrice(37, 5.71).build(),
          builder.setTicker("TAEE3").setQuantityAndPrice(87, 11.43).build(),
          builder.setTicker("VALE3").setQuantityAndPrice(20, 55.43).build()};

      for (int rowIdx = 0; rowIdx < expectedRows.length; rowIdx++) {
        int cellIdx = 0;
        var expectedRow = expectedRows[rowIdx];
        Row row = sheet.getRow(rowIdx + 1);
        assertNotNull(row);
        assertEquals(note.getDate(), LocalDate.ofInstant(
            row.getCell(cellIdx++).getDateCellValue().toInstant(), ZoneId.systemDefault()));
        assertEquals(expectedRow.category(), row.getCell(cellIdx++).getStringCellValue());
        assertEquals(expectedRow.ticker(), row.getCell(cellIdx++).getStringCellValue());
        assertEquals(expectedRow.operation(),
            row.getCell(cellIdx++).getStringCellValue().charAt(0));
        assertEquals(expectedRow.quantity(), (int) (row.getCell(cellIdx++).getNumericCellValue()));
        assertEquals(expectedRow.price(), row.getCell(cellIdx++).getNumericCellValue());
        assertEquals(expectedRow.broker(), row.getCell(cellIdx++).getStringCellValue());
        assertEquals(0.0, row.getCell(cellIdx++).getNumericCellValue());
        assertAlmostEquals(expectedRow.fees(), row.getCell(cellIdx++).getNumericCellValue());
        assertAlmostEquals(expectedRow.tax(), row.getCell(cellIdx++).getNumericCellValue());
        assertAlmostEquals(expectedRow.irrf(), row.getCell(cellIdx).getNumericCellValue());
      }
    }
  }

  @Test
  public void testXlsxOutputForSample2() throws Exception {
    BrokerageNote note = SinacorBrokerageNote
        .readPdf(resources.getSinacorBrokerageNoteResourceFile(ResourcesUtil.NOTE_SAMPLE_2), "");

    var statusinvest = new StatusInvestOutput(note, shareMap, brokerMap);
    File testFile = new File("/tmp/test.xlsx");
    FileOutputStream output = new FileOutputStream(testFile);
    statusinvest.writeToXslx(output);

    ExpectedRow.Builder builder =
        new ExpectedRow.Builder("RICO INVESTIMENTOS", 20_205.37, 6.06, 6_119.49, 0.30);
    ExpectedRow[] expectedRows = {
        builder.setCategoryAndOperation("FII's", 'C').setTicker("HGLG11")
            .setQuantityAndPrice(11, 158.87).build(),
        builder.setQuantityAndPrice(7, 158.88).build(),
        builder.setQuantityAndPrice(3, 158.86).build(),
        builder.setTicker("MXRF11").setQuantityAndPrice(130 + 284, 9.33).build(),
        builder.setQuantityAndPrice(15, 9.32).build(),
        builder.setCategoryAndOperation("FII's", 'V').setTicker("VILG11")
            .setQuantityAndPrice(71, 86.19).build(),
        builder.setCategoryAndOperation("Ações", 'C').setTicker("ITSA4")
            .setQuantityAndPrice(600, 10.46).build(),
        builder.setQuantityAndPrice(45, 10.47).build()};

    try (InputStream testStream = new FileInputStream(testFile)) {
      Workbook wb = WorkbookFactory.create(testStream);
      Sheet sheet = wb.getSheetAt(0);

      for (int rowIdx = 0; rowIdx < expectedRows.length; rowIdx++) {
        int cellIdx = 0;
        var expectedRow = expectedRows[rowIdx];
        Row row = sheet.getRow(rowIdx + 1);
        assertNotNull(row);
        assertEquals(note.getDate(), LocalDate.ofInstant(
            row.getCell(cellIdx++).getDateCellValue().toInstant(), ZoneId.systemDefault()));
        assertEquals(expectedRow.category(), row.getCell(cellIdx++).getStringCellValue());
        assertEquals(expectedRow.ticker(), row.getCell(cellIdx++).getStringCellValue());
        assertEquals(expectedRow.operation(),
            row.getCell(cellIdx++).getStringCellValue().charAt(0));
        assertEquals(expectedRow.quantity(), (int) (row.getCell(cellIdx++).getNumericCellValue()));
        assertEquals(expectedRow.price(), row.getCell(cellIdx++).getNumericCellValue());
        assertEquals(expectedRow.broker(), row.getCell(cellIdx++).getStringCellValue());
        assertEquals(0.0, row.getCell(cellIdx++).getNumericCellValue());
        assertAlmostEquals(expectedRow.fees(), row.getCell(cellIdx++).getNumericCellValue());
        assertAlmostEquals(expectedRow.tax(), row.getCell(cellIdx++).getNumericCellValue());
        assertAlmostEquals(expectedRow.irrf(), row.getCell(cellIdx).getNumericCellValue());
      }
    }
  }

  @Test
  public void summarization() throws Exception {
    BrokerageNote note = SinacorBrokerageNote
        .readPdf(resources.getSinacorBrokerageNoteResourceFile(ResourcesUtil.NOTE_SAMPLE_2), "");

    var statusinvest = new StatusInvestOutput(note, shareMap, brokerMap);
    File testFile = new File("/tmp/test.xlsx");
    FileOutputStream output = new FileOutputStream(testFile);
    statusinvest.writeToXslx(output);
    statusinvest.summarizeCreatedFile(testFile);
  }

  static void assertAlmostEquals(double x, double y) {
    var diff = x - y;
    if (diff < 0) {
      diff *= -1;
    }

    assertTrue(diff < 1e-3, () -> String.format("Expected %f, found %f", x, y));
  }
}
