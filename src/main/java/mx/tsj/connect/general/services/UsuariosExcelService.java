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

import mx.tsj.connect.general.dto.UsuarioRowDto;

@Service
public class UsuariosExcelService {
    private static final String[] HEADERS = {
            "Nombre", "Usuario", "Rol", "Unidad Ejecutora", "Status"
    };

    public byte[] createWorkbook(List<UsuarioRowDto> usuarios) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Usuarios");
            CellStyle headerStyle = createHeaderStyle(workbook);
            createHeader(sheet, headerStyle);

            int rowIndex = 1;
            for (UsuarioRowDto usuario : usuarios) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(value(usuario.nombre()));
                row.createCell(1).setCellValue(value(usuario.usuario()));
                row.createCell(2).setCellValue(value(usuario.roles()));
                row.createCell(3).setCellValue(value(usuario.unidadEjecutoraGasto()));
                row.createCell(4).setCellValue(value(usuario.status()));
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, Math.max(rowIndex - 1, 0), 0, HEADERS.length - 1));
            int[] widths = {42, 28, 36, 44, 18};
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
