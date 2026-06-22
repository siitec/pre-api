package mx.tsj.connect.general.services;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import mx.tsj.connect.general.dto.AccionesModulo;
import mx.tsj.connect.general.dto.ModuloAutorizado;
import mx.tsj.connect.general.repositories.ModuloPermisoProjection;
import mx.tsj.connect.general.repositories.PermisoRepository;

@Service
public class AuthorizationService {
    private final PermisoRepository permisoRepository;

    public AuthorizationService(PermisoRepository permisoRepository) {
        this.permisoRepository = permisoRepository;
    }

    public List<ModuloAutorizado> getModulos(Integer usuarioId) {
        Map<Integer, ModuloAcumulado> modulos = new LinkedHashMap<>();

        for (ModuloPermisoProjection row : permisoRepository.findModulosByUsuarioId(usuarioId)) {
            ModuloAcumulado modulo = modulos.computeIfAbsent(
                    row.getModuloId(),
                    id -> new ModuloAcumulado(id, row.getModuloNombre()));
            modulo.add(row);
        }

        return modulos.values().stream()
                .map(ModuloAcumulado::toDto)
                .toList();
    }

    private static boolean allowed(String value) {
        return value != null && "S".equals(value.trim().toUpperCase(Locale.ROOT));
    }

    private static final class ModuloAcumulado {
        private final Integer id;
        private final String nombre;
        private final Set<String> roles = new LinkedHashSet<>();
        private final Set<String> partidas = new LinkedHashSet<>();
        private String alcance = "LOCAL";
        private boolean consultar;
        private boolean crear;
        private boolean editar;
        private boolean eliminar;
        private boolean ejecutar;
        private boolean autorizar;
        private boolean exportar;

        private ModuloAcumulado(Integer id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        private void add(ModuloPermisoProjection row) {
            if (row.getRolNombre() != null) {
                roles.add(row.getRolNombre().trim());
            }
            if ("GLOBAL".equalsIgnoreCase(row.getAlcance())) {
                alcance = "GLOBAL";
            }
            if (row.getPartida() != null && !row.getPartida().isBlank()) {
                partidas.add(row.getPartida().trim());
            }
            consultar |= allowed(row.getConsultar());
            crear |= allowed(row.getCrear());
            editar |= allowed(row.getEditar());
            eliminar |= allowed(row.getEliminar());
            ejecutar |= allowed(row.getEjecutar());
            autorizar |= allowed(row.getAutorizar());
            exportar |= allowed(row.getExportar());
        }

        private ModuloAutorizado toDto() {
            return new ModuloAutorizado(
                    id,
                    nombre,
                    new ArrayList<>(roles),
                    alcance,
                    new ArrayList<>(partidas),
                    new AccionesModulo(
                            consultar,
                            crear,
                            editar,
                            eliminar,
                            ejecutar,
                            autorizar,
                            exportar));
        }
    }
}
