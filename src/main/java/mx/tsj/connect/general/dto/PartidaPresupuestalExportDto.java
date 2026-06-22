package mx.tsj.connect.general.dto;

import java.math.BigDecimal;

public record PartidaPresupuestalExportDto(
        String cog,
        String descripcion,
        String ueg,
        String unidadEjecutora,
        BigDecimal monto,
        BigDecimal comprometido,
        BigDecimal ejercido,
        BigDecimal total) {
}
