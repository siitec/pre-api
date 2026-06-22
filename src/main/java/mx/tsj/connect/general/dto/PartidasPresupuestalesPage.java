package mx.tsj.connect.general.dto;

import java.util.List;

public record PartidasPresupuestalesPage(
        List<PartidaPresupuestalResumenDto> data,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}
