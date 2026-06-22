package mx.tsj.connect.general.dto;

import java.util.List;

public record DashboardSolicitudDto(
        String tipo,
        String nombre,
        long total,
        List<DashboardEstatusDto> estatus) {
}
