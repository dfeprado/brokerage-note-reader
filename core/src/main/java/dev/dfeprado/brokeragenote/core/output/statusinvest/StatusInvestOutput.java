package dev.dfeprado.brokeragenote.core.output.statusinvest;

import dev.dfeprado.brokeragenote.core.exceptions.BrokerageNoteReadError;
import dev.dfeprado.brokeragenote.core.input.SinacorReader;
import dev.dfeprado.brokeragenote.core.Operation;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.*;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class StatusInvestOutput {
  private final SinacorReader note;
  private final Map<String, ShareSymbol> shareMap;
  private final Map<String, String> brokerMap;
  private final NumberFormat numberFmt;

  public StatusInvestOutput(SinacorReader note, Map<String, ShareSymbol> shareMap,
      Map<String, String> brokerMap) {
    this.note = note;
    this.shareMap = shareMap;
    this.brokerMap = brokerMap;
    numberFmt = NumberFormat.getNumberInstance(new Locale("pt", "BR"));
    numberFmt.setMaximumFractionDigits(2);
    numberFmt.setMinimumFractionDigits(2);
  }

  public void summarizeCreatedFile(File file) throws IOException {
    try (Workbook wb = WorkbookFactory.create(file)) {
      LocalDateTime date = null;
      Set<String> tickers = new HashSet<String>();
      double negociationTotal = 0.0;
      double totalFees = 0;
      double totalIrrf = 0;

      Sheet sheet = wb.getSheetAt(0);
      var ops = note.parseOperations();
      for (int i = 0; i < ops.size(); i++) {
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
    } catch (EncryptedDocumentException | BrokerageNoteReadError e) {
      throw new IOException(e.getMessage());
    }
  }

  public void writeToXslx(FileOutputStream output) throws FileNotFoundException, IOException {
    try (InputStream inModel = getClass().getResourceAsStream("statusinvest_model.xlsx")) {
      Workbook wb = WorkbookFactory.create(inModel);
      Sheet sheet = wb.getSheetAt(0);
      var ops = note.parseOperations();
      var header = note.parseHeader();
      String brokerName = brokerMap.get(header.brokerName());
      LocalDate noteDate = header.date();
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
    } catch (BrokerageNoteReadError e) {
      throw new IOException(e.getMessage());
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
