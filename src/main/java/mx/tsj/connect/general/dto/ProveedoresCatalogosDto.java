package mx.tsj.connect.general.dto;

import java.util.List;

public record ProveedoresCatalogosDto(
        List<CatalogOptionDto> estados,
        List<MunicipioOptionDto> municipios) {
}
