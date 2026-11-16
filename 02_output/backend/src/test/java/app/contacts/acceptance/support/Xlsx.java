package app.contacts.acceptance.support;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * An Export file read the way {@code docs/02_contracts/export-xlsx.schema.json} describes it: one
 * sheet, every cell as text.
 *
 * @param sheetCount number of sheets
 * @param header the first row
 * @param rows the data rows
 * @param types cell types of the data rows (to check that values are stored as text, SR-07)
 */
public record Xlsx(
    int sheetCount, List<String> header, List<List<String>> rows, List<List<CellType>> types) {

  /** Reads an {@code .xlsx} file. */
  public static Xlsx read(byte[] file) {
    try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
      Sheet sheet = workbook.getSheetAt(0);
      DataFormatter formatter = new DataFormatter();
      List<List<String>> all = new ArrayList<>();
      List<List<CellType>> allTypes = new ArrayList<>();
      int width = sheet.getRow(0).getLastCellNum();
      for (Row row : sheet) {
        List<String> cells = new ArrayList<>();
        List<CellType> types = new ArrayList<>();
        for (int c = 0; c < width; c++) {
          Cell cell = row.getCell(c);
          cells.add(cell == null ? "" : textOf(cell, formatter));
          types.add(cell == null ? CellType.BLANK : cell.getCellType());
        }
        all.add(cells);
        allTypes.add(types);
      }
      return new Xlsx(
          workbook.getNumberOfSheets(),
          all.get(0),
          all.subList(1, all.size()),
          allTypes.subList(1, allTypes.size()));
    } catch (IOException e) {
      throw new IllegalStateException("not an .xlsx file", e);
    }
  }

  private static String textOf(Cell cell, DataFormatter formatter) {
    if (cell.getCellType() == CellType.FORMULA) {
      return "=" + cell.getCellFormula();
    }
    return formatter.formatCellValue(cell);
  }

  /** The column of the data rows under the header text. */
  public List<String> column(String headerText) {
    int index = header.indexOf(headerText);
    if (index < 0) {
      throw new AssertionError("no column " + headerText + " in " + header);
    }
    List<String> values = new ArrayList<>();
    for (List<String> row : rows) {
      values.add(row.get(index));
    }
    return values;
  }
}
