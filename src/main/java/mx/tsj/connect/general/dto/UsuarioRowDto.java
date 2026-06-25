package mx.tsj.connect.general.dto;

public record UsuarioRowDto(
        Integer id,
        String status,
        String usuario,
        String nombre,
        String ua,
        String unidadEjecutoraGasto,
        String area,
        String roles,
        Integer roleId,
        String campus,
        String puesto,
        String responsable) {
}
