package app.contacts.service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

/**
 * The Export file: one sheet, a header row, then one row per Contact, every cell a text cell
 * (ADR-09). Rows are streamed to a temporary file, so memory stays flat for 10,000 Contacts.
 */
class ExportWorkbook {

  /** Rows kept in memory before they are flushed (ADR-09). */
  private static final int WINDOW = 100;

  /** Leading characters with which a spreadsheet would start a formula (SR-07). */
  private static final String FORMULA_STARTS = "=+-@\t\r";

  private final SXSSFWorkbook workbook = new SXSSFWorkbook(WINDOW);
  private final Sheet sheet = workbook.createSheet();
  private final CellStyle asText = workbook.createCellStyle();

  /**
   * A workbook with its header row.
   *
   * @param header the column titles
   */
  ExportWorkbook(List<String> header) {
    asText.setQuotePrefixed(true);
    addRow(header);
  }

  /**
   * Adds the next row; an empty value is an empty text cell.
   *
   * @param values the cell values, {@code null} for empty
   */
  final void addRow(List<String> values) {
    Row row = sheet.createRow(sheet.getLastRowNum() + 1);
    for (int i = 0; i < values.size(); i++) {
      String value = values.get(i) == null ? "" : values.get(i);
      Cell cell = row.createCell(i);
      cell.setCellValue(value);
      if (!value.isEmpty() && FORMULA_STARTS.indexOf(value.charAt(0)) >= 0) {
        cell.setCellStyle(asText);
      }
    }
  }

  /**
   * The finished file; closing the workbook removes its temporary file.
   *
   * @return the {@code .xlsx} content
   */
  byte[] toBytes() {
    try (workbook;
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
