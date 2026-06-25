package mx.tsj.connect.general.dto;

import java.util.List;

public record UsuariosCatalogosDto(
        List<CatalogOptionDto> unidades,
        List<CatalogOptionDto> roles) {
}
