package mx.tsj.connect.general.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardPresupuestoDto(
        BigDecimal presupuestoAsignado,
        BigDecimal disponible,
        BigDecimal comprometido,
        BigDecimal ejercido,
        long partidas,
        List<DashboardPartidaDto> principalesPartidas,
        List<DashboardSolicitudDto> solicitudes) {
}
