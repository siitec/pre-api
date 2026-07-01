package mx.tsj.connect.general.dto;

import java.util.List;

public record PartidasCatalogoPage(
        List<PartidaCatalogoDto> data,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}
