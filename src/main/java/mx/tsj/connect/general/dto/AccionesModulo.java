package mx.tsj.connect.general.dto;

public record AccionesModulo(
        boolean consultar,
        boolean crear,
        boolean editar,
        boolean eliminar,
        boolean ejecutar,
        boolean autorizar,
        boolean exportar) {
}
