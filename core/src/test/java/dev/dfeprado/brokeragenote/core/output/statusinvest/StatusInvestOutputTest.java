package dev.dfeprado.brokeragenote.core.output.statusinvest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
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
import dev.dfeprado.brokeragenote.core.exceptions.BrokerageNoteReadError;
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
          new ShareSymbol("VALE3", InvestmentCategory.ACAO));

  @Test
  public void testCsvOutput() throws BrokerageNoteReadError, URISyntaxException, IOException {
    BrokerageNote note =
        SinacorBrokerageNote.readPdf(resources.getSinacorBrokerageNoteResourceFile());

    var statusinvest = new StatusInvestOutput(note, shareMap, brokerMap);
    String expectedOutput =
        """
            Data operação;Categoria;Código Ativo;Operação C/V;Quantidade;Preço unitário;Corretora;Corretagem;Taxas;Impostos;IRRF\r
            03/04/2025;Ações;BBAS3;C;16;28,44;RICO INVESTIMENTOS;0,00;0,14;0,00;0,00\r
            03/04/2025;Ações;CVCB3;C;14;2,16;RICO INVESTIMENTOS;0,00;0,01;0,00;0,00\r
            03/04/2025;Ações;GGBR3;C;22;15,55;RICO INVESTIMENTOS;0,00;0,10;0,00;0,00\r
            03/04/2025;Ações;GGBR3;C;47;15,55;RICO INVESTIMENTOS;0,00;0,22;0,00;0,00\r
            03/04/2025;Ações;GGBR3;C;3;15,55;RICO INVESTIMENTOS;0,00;0,01;0,00;0,00\r
            03/04/2025;Ações;ITSA4;C;100;9,67;RICO INVESTIMENTOS;0,00;0,29;0,00;0,00\r
            03/04/2025;Ações;ITSA4;C;11;9,67;RICO INVESTIMENTOS;0,00;0,03;0,00;0,00\r
            03/04/2025;Ações;SAPR3;C;300;5,70;RICO INVESTIMENTOS;0,00;0,51;0,00;0,00\r
            03/04/2025;Ações;SAPR3;C;37;5,71;RICO INVESTIMENTOS;0,00;0,06;0,00;0,00\r
            03/04/2025;Ações;TAEE3;C;17;11,43;RICO INVESTIMENTOS;0,00;0,06;0,00;0,00\r
            03/04/2025;Ações;TAEE3;C;70;11,43;RICO INVESTIMENTOS;0,00;0,24;0,00;0,00\r
            03/04/2025;Ações;VALE3;C;20;55,43;RICO INVESTIMENTOS;0,00;0,33;0,00;0,00\r
            """;

    assertEquals(expectedOutput, statusinvest.getAsCsv());
  }

  @Test
  public void testXlsxFileOutput() throws BrokerageNoteReadError, URISyntaxException, IOException {
    BrokerageNote note =
        SinacorBrokerageNote.readPdf(resources.getSinacorBrokerageNoteResourceFile());

    var statusinvest = new StatusInvestOutput(note, shareMap, brokerMap);
    File testFile = new File("/tmp/test.xlsx");
    FileOutputStream output = new FileOutputStream(testFile);
    statusinvest.writeToXslx(output);

    try (InputStream testStream = new FileInputStream(testFile)) {
      var ops = note.getOps();
      Workbook wb = WorkbookFactory.create(testStream);
      Sheet sheet = wb.getSheetAt(0);

      int rowIdx = 1;
      int opIdx = rowIdx - 1;
      Row row = sheet.getRow(rowIdx);
      assertNotNull(row);
      assertEquals(note.getDate(), LocalDate
          .ofInstant(row.getCell(0).getDateCellValue().toInstant(), ZoneId.systemDefault()));
      assertEquals("BBAS3", row.getCell(2).getStringCellValue());
      assertEquals(ops.get(opIdx).getPrice(), row.getCell(5).getNumericCellValue());
      assertEquals("RICO INVESTIMENTOS", row.getCell(6).getStringCellValue());
      assertEquals(ops.get(opIdx).getFee() + ops.get(opIdx).getEmoluments(),
          row.getCell(8).getNumericCellValue());

      rowIdx = 7;
      opIdx = rowIdx - 1;
      row = sheet.getRow(rowIdx);
      assertNotNull(row);
      assertEquals(note.getDate(), LocalDate
          .ofInstant(row.getCell(0).getDateCellValue().toInstant(), ZoneId.systemDefault()));
      assertEquals("ITSA4", row.getCell(2).getStringCellValue());
      assertEquals(ops.get(opIdx).getPrice(), row.getCell(5).getNumericCellValue());
      assertEquals("RICO INVESTIMENTOS", row.getCell(6).getStringCellValue());
      assertEquals(ops.get(opIdx).getFee() + ops.get(opIdx).getEmoluments(),
          row.getCell(8).getNumericCellValue());
    }
  }
}
