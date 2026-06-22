package mx.tsj.connect.general.dto;

import java.math.BigDecimal;

public record PartidaPresupuestalDetalleDto(
        String ueg,
        String unidadEjecutora,
        BigDecimal monto,
        BigDecimal comprometido,
        BigDecimal ejercido,
        BigDecimal total) {
}
