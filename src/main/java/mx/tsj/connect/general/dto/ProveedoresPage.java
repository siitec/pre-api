package mx.tsj.connect.general.dto;

import java.util.List;

public record ProveedoresPage(
        List<ProveedorDto> data,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}
