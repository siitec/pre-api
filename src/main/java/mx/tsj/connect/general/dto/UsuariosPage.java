package mx.tsj.connect.general.dto;

import java.util.List;

public record UsuariosPage(
        List<UsuarioRowDto> data,
        int page,
        int pageSize,
        long total,
        int totalPages) {
}
