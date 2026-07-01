package mx.tsj.connect.general.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

import mx.tsj.connect.general.dto.ProveedorDto;

@Service
public class ProveedoresExcelService {
    private static final String[] HEADERS = {
            "RFC", "Razón Social", "Teléfono", "Colonia", "Código Postal", "Ciudad", "Estado"
    };

    public byte[] createWorkbook(List<ProveedorDto> proveedores) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Proveedores");
            CellStyle headerStyle = createHeaderStyle(workbook);
            createHeader(sheet, headerStyle);

            int rowIndex = 1;
            for (ProveedorDto proveedor : proveedores) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(value(proveedor.rfc()));
                row.createCell(1).setCellValue(value(proveedor.nombre()));
                row.createCell(2).setCellValue(value(proveedor.telefono1()));
                row.createCell(3).setCellValue(value(proveedor.colonia()));
                row.createCell(4).setCellValue(value(proveedor.codigoP()));
                row.createCell(5).setCellValue(value(proveedor.ciudad()));
                row.createCell(6).setCellValue(value(proveedor.estado()));
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(rowIndex - 1, 0), 0, HEADERS.length - 1));
            int[] widths = {20, 48, 20, 28, 18, 24, 24};
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

    private String value(String value) {
        return value == null ? "" : value;
    }
}
