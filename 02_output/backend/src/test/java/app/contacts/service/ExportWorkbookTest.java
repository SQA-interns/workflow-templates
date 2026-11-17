package app.contacts.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExportWorkbookTest {

  private static Sheet onlySheet(byte[] file) throws IOException {
    XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file));
    assertThat(workbook.getNumberOfSheets()).isEqualTo(1);
    return workbook.getSheetAt(0);
  }

  private static byte[] write(List<String> header, List<List<String>> rows) {
    ExportWorkbook workbook = new ExportWorkbook(header);
    rows.forEach(workbook::addRow);
    return workbook.toBytes();
  }

  @Test
  void r01601OneSheetWithTheHeaderAndOneRowPerContactInUtf8() throws IOException {
    Sheet sheet =
        onlySheet(
            write(
                List.of("Ime", "Priimek"),
                List.of(List.of("Žiga", "Čuk"), Arrays.asList("Maja", null))));

    assertThat(sheet.getLastRowNum()).isEqualTo(2);
    assertThat(texts(sheet.getRow(0))).containsExactly("Ime", "Priimek");
    assertThat(texts(sheet.getRow(1))).containsExactly("Žiga", "Čuk");
    assertThat(texts(sheet.getRow(2))).containsExactly("Maja", "");
  }

  @ParameterizedTest
  @ValueSource(strings = {"=1+1", "+1+1", "-1+1", "@SUM(1)", "\t=1", "\r=1"})
  void ex01603FormulaLikeValuesAreTextShownAsWritten(String value) throws IOException {
    Cell cell = onlySheet(write(List.of("Opomba"), List.of(List.of(value)))).getRow(1).getCell(0);

    assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
    assertThat(cell.getStringCellValue()).isEqualTo(value);
    assertThat(cell.getCellStyle().getQuotePrefixed()).isTrue();
  }

  @Test
  void ex01603OtherValuesAreTextWithoutQuotePrefix() throws IOException {
    Cell cell = onlySheet(write(List.of("Ime"), List.of(List.of("1000")))).getRow(1).getCell(0);

    assertThat(cell.getCellType()).isEqualTo(CellType.STRING);
    assertThat(cell.getCellStyle().getQuotePrefixed()).isFalse();
  }

  private static List<String> texts(Row row) {
    return java.util.stream.IntStream.range(0, row.getLastCellNum())
        .mapToObj(i -> row.getCell(i).getStringCellValue())
        .toList();
  }
}
