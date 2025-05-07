package dev.dfeprado.brokeragenote.core.output.statusinvest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import dev.dfeprado.brokeragenote.core.BrokerageNote;
import dev.dfeprado.brokeragenote.core.Operation;
import dev.dfeprado.brokeragenote.core.output.OutputNote;
import dev.dfeprado.brokeragenote.core.output.XlsxWriter;

public class StatusInvestOutput implements XlsxWriter, OutputNote {
  private final BrokerageNote note;
  private final Map<String, ShareSymbol> shareMap;
  private final Map<String, String> brokerMap;
  private final NumberFormat numberFmt;

  public StatusInvestOutput(BrokerageNote note, Map<String, ShareSymbol> shareMap,
      Map<String, String> brokerMap) {
    this.note = note;
    this.shareMap = shareMap;
    this.brokerMap = brokerMap;
    numberFmt = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
    numberFmt.setMaximumFractionDigits(2);
    numberFmt.setMinimumFractionDigits(2);
  }

  public String getAsCsv() throws IOException {
    var sb = new StringBuilder();
    CSVPrinter printer = new CSVPrinter(sb, CSVFormat.DEFAULT.builder().setDelimiter(';').get());
    var ops = note.getOps();

    // header
    printer.printRecord("Data operação", "Categoria", "Código Ativo", "Operação C/V", "Quantidade",
        "Preço unitário", "Corretora", "Corretagem", "Taxas", "Impostos", "IRRF");

    String noteDate = note.getDate().format(DateTimeFormatter.ofPattern("dd/MM/y"));
    String brokerName = brokerMap.get(note.getBrokerName());
    final var decimalZero = numberFmt.format(0.0);

    for (Operation o : ops) {
      var symbol = shareMap.get(o.getShareName());
      printer.printRecord(noteDate, getCategory(symbol), symbol.ticker(), getOperationType(o),
          (int) o.getQuantity(), numberFmt.format(o.getPrice()), brokerName, decimalZero,
          numberFmt.format(getOperationFees(o)), decimalZero, decimalZero);
    }

    printer.close();

    return sb.toString();

  }

  @Override
  public void summarizeCreatedFile(File file) throws IOException {
    try (Workbook wb = WorkbookFactory.create(file)) {
      LocalDateTime date = null;
      Set<String> tickers = new HashSet<String>();
      double negociationTotal = 0.0;
      double totalFees = 0;
      double totalIrrf = 0;

      Sheet sheet = wb.getSheetAt(0);
      for (int i = 0; i < note.getOps().size(); i++) {
        Row row = sheet.getRow(i + 1);

        if (date == null) {
          date = row.getCell(0).getLocalDateTimeCellValue();
        }

        tickers.add(row.getCell(2).getStringCellValue());
        negociationTotal +=
            row.getCell(4).getNumericCellValue() * row.getCell(5).getNumericCellValue();
        totalFees += row.getCell(8).getNumericCellValue();
        totalIrrf += row.getCell(10).getNumericCellValue();
      }

      System.out.println("Date: " + date.toLocalDate());
      System.out.println("Tickers: " + tickers);
      System.out.printf("Total operation amount: %s%n", numberFmt.format(negociationTotal));
      System.out.printf("Total fees: %s%n", numberFmt.format(totalFees));
      System.out.printf("Total irrf: %s%n", numberFmt.format(totalIrrf));
    } catch (EncryptedDocumentException e) {
      throw new IOException(e.getMessage());
    }
  }

  @Override
  public void writeToXslx(FileOutputStream output) throws FileNotFoundException, IOException {
    try (InputStream inModel = getClass().getResourceAsStream("statusinvest_model.xlsx")) {
      Workbook wb = WorkbookFactory.create(inModel);
      Sheet sheet = wb.getSheetAt(0);
      var ops = note.getOps();
      String brokerName = brokerMap.get(note.getBrokerName());
      LocalDate noteDate = note.getDate();
      for (int i = 1; i <= ops.size(); i++) {
        Operation op = ops.get(i - 1);
        var symbol = shareMap.get(op.getShareName());
        int cellIdx = 0;
        Row row = sheet.createRow(i);
        row.createCell(cellIdx++).setCellValue(noteDate);
        row.createCell(cellIdx++).setCellValue(getCategory(symbol));
        row.createCell(cellIdx++).setCellValue(symbol.ticker());
        row.createCell(cellIdx++).setCellValue(getOperationType(op));
        row.createCell(cellIdx++).setCellValue((int) op.getQuantity());
        row.createCell(cellIdx++).setCellValue(op.getPrice());
        row.createCell(cellIdx++).setCellValue(brokerName);
        row.createCell(cellIdx++).setCellValue(0.0);
        row.createCell(cellIdx++).setCellValue(getOperationFees(op));
        row.createCell(cellIdx++).setCellValue(0.0);
        row.createCell(cellIdx).setCellValue(op.getIrrf());
      }

      wb.write(output);
      wb.close();
    }
  }

  private String getCategory(ShareSymbol symbol) {
    return switch (symbol.category()) {
      case FII -> "FII's";
      case ACAO -> "Ações";
    };
  }

  private String getOperationType(Operation op) {
    return op.isBuy() ? "C" : "V";
  }

  private double getOperationFees(Operation op) {
    return op.getFee() + op.getEmoluments();
  }
}
