package mx.tsj.connect.general.dto;

import java.util.List;

public record ModuloAutorizado(
        Integer id,
        String nombre,
        List<String> roles,
        String alcance,
        List<String> partidas,
        AccionesModulo acciones) {
}
