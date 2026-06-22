package mx.tsj.connect.general.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import mx.tsj.connect.general.dto.PartidaPresupuestalExportDto;

@Service
public class PartidasExcelService {
    private static final String[] HEADERS = {
            "COG", "Descripción", "UEG", "Unidad Ejecutora",
            "Monto", "Comprometido", "Ejercido", "Total"
    };

    public byte[] createWorkbook(List<PartidaPresupuestalExportDto> partidas) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Partidas presupuestales");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);
            createHeader(sheet, headerStyle);

            int rowIndex = 1;
            for (PartidaPresupuestalExportDto partida : partidas) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(value(partida.cog()));
                row.createCell(1).setCellValue(value(partida.descripcion()));
                row.createCell(2).setCellValue(value(partida.ueg()));
                row.createCell(3).setCellValue(value(partida.unidadEjecutora()));
                createMoneyCell(row, 4, partida.monto(), moneyStyle);
                createMoneyCell(row, 5, partida.comprometido(), moneyStyle);
                createMoneyCell(row, 6, partida.ejercido(), moneyStyle);
                createMoneyCell(row, 7, partida.total(), moneyStyle);
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(rowIndex - 1, 0), 0, HEADERS.length - 1));
            int[] widths = {12, 42, 12, 38, 18, 18, 18, 18};
            for (int index = 0; index < widths.length; index++) {
                sheet.setColumnWidth(index, widths[index] * 256);
            }

            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("No fue posible generar el archivo Excel.", exception);
        }
    }

    private void createHeader(Sheet sheet, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < HEADERS.length; index++) {
            Cell cell = row.createCell(index);
            cell.setCellValue(HEADERS[index]);
            cell.setCellStyle(style);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createMoneyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        return style;
    }

    private void createMoneyCell(Row row, int index, BigDecimal amount, CellStyle style) {
        Cell cell = row.createCell(index);
        cell.setCellValue(amount == null ? 0 : amount.doubleValue());
        cell.setCellStyle(style);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
