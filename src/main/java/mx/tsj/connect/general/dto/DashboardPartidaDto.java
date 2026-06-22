package mx.tsj.connect.general.dto;

import java.math.BigDecimal;

public record DashboardPartidaDto(
        String cog,
        String descripcion,
        BigDecimal monto,
        BigDecimal comprometido,
        BigDecimal ejercido) {
}
