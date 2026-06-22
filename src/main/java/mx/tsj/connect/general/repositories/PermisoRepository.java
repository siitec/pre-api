package mx.tsj.connect.general.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import mx.tsj.connect.general.entities.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Integer> {
    @Query(value = """
            SELECT
                m.id AS moduloId,
                m.nombre AS moduloNombre,
                r.nombre AS rolNombre,
                pf.alcance AS alcance,
                partida.clave AS partida,
                p.consultar AS consultar,
                p.crear AS crear,
                p.editar AS editar,
                p.eliminar AS eliminar,
                p.ejecutar AS ejecutar,
                p.autorizar AS autorizar,
                p.exportar AS exportar
            FROM dbo.Perfiles pf
            INNER JOIN dbo.Roles r ON r.id = pf.id_Rol
            INNER JOIN dbo.Permisos p ON p.id_Rol = r.id
            INNER JOIN dbo.Modulos m ON m.id = p.id_Modulo
            LEFT JOIN dbo.PartidasRol partidaRol ON partidaRol.id_Rol = r.id AND m.id = 1
            LEFT JOIN dbo.Partidas partida ON partida.id = partidaRol.id_Partida
            WHERE pf.id_Usuario = :usuarioId
            ORDER BY m.id, r.nombre
            """, nativeQuery = true)
    List<ModuloPermisoProjection> findModulosByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
