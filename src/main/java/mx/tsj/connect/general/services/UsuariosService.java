package mx.tsj.connect.general.services;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.tsj.connect.general.dto.CatalogOptionDto;
import mx.tsj.connect.general.dto.CreateUsuarioRequest;
import mx.tsj.connect.general.dto.ModuloAutorizado;
import mx.tsj.connect.general.dto.UpdateUsuarioRequest;
import mx.tsj.connect.general.dto.UsuarioRowDto;
import mx.tsj.connect.general.dto.UsuariosCatalogosDto;
import mx.tsj.connect.general.dto.UsuariosPage;
import mx.tsj.connect.general.entities.Usuario;
import mx.tsj.connect.general.repositories.UsuarioRepository;

@Service
public class UsuariosService {
    private static final String MODULE_NAME = "USUARIOS";
    private static final String DEFAULT_PASSWORD = "12345678";
    private static final String DEFAULT_PERMISOS =
            "111100111110000000000000000000000000000000000000000111000000000000100000";
    private static final String DEFAULT_LEGACY_PROFILE = "USUARIO";
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "status", "status",
            "usuario", "usuario",
            "nombre", "nombre",
            "ua", "ua",
            "area", "area",
            "roles", "roles",
            "puesto", "puesto",
            "responsable", "responsable");

    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UsuariosService(
            UsuarioRepository usuarioRepository,
            AuthorizationService authorizationService,
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.authorizationService = authorizationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public UsuariosPage findAll(
            String username,
            int requestedPage,
            int requestedPageSize,
            String search,
            String sortBy,
            String sortOrder) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar usuarios.");
        }

        int page = Math.max(requestedPage, 1);
        int pageSize = Math.min(Math.max(requestedPageSize, 1), 100);
        int startRow = ((page - 1) * pageSize) + 1;
        int endRow = page * pageSize;
        String sortField = SORT_FIELDS.getOrDefault(sortBy, "usuario");
        String direction = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startRow", startRow)
                .addValue("endRow", endRow);
        String filters = buildSearchFilter(search, params);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT_BIG(*) FROM dbo.Usuarios u WHERE 1 = 1 " + filters,
                params,
                Long.class);

        String sql = """
                WITH usuariosBase AS (
                    SELECT
                        u.Id AS id,
                        u.Status AS status,
                        u.Usuario AS usuario,
                        u.Nombre AS nombre,
                        u.UA AS ua,
                        ua.Nombre AS unidadEjecutoraGasto,
                        u.Area AS area,
                        roles.roles AS roles,
                        primaryRole.roleId AS roleId,
                        u.Campus AS campus,
                        u.Puesto AS puesto,
                        u.Responsable AS responsable
                    FROM dbo.Usuarios u
                    LEFT JOIN dbo.UA ua ON CONVERT(VARCHAR(10), ua.ID) = LTRIM(RTRIM(u.UA))
                    OUTER APPLY (
                        SELECT STUFF((
                            SELECT DISTINCT ', ' + r.nombre
                            FROM dbo.Perfiles pf
                            INNER JOIN dbo.Roles r ON r.id = pf.id_Rol
                            WHERE pf.id_Usuario = u.Id
                            FOR XML PATH(''), TYPE
                        ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS roles
                    ) roles
                    OUTER APPLY (
                        SELECT TOP 1 pf.id_Rol AS roleId
                        FROM dbo.Perfiles pf
                        WHERE pf.id_Usuario = u.Id
                        ORDER BY pf.id
                    ) primaryRole
                    WHERE 1 = 1
                    %s
                ),
                usuariosPaginados AS (
                    SELECT
                        ROW_NUMBER() OVER (ORDER BY %s %s, id ASC) AS rowNumber,
                        id, status, usuario, nombre, ua, unidadEjecutoraGasto, area, roles,
                        roleId, campus, puesto, responsable
                    FROM usuariosBase
                )
                SELECT id, status, usuario, nombre, ua, unidadEjecutoraGasto, area, roles,
                       roleId, campus, puesto, responsable
                FROM usuariosPaginados
                WHERE rowNumber BETWEEN :startRow AND :endRow
                ORDER BY rowNumber
                """.formatted(filters, sortField, direction);

        var data = jdbcTemplate.query(sql, params, (resultSet, rowNum) ->
                new UsuarioRowDto(
                        resultSet.getInt("id"),
                        resultSet.getString("status"),
                        resultSet.getString("usuario"),
                        resultSet.getString("nombre"),
                        resultSet.getString("ua"),
                        resultSet.getString("unidadEjecutoraGasto"),
                        resultSet.getString("area"),
                        resultSet.getString("roles"),
                        getNullableInteger(resultSet, "roleId"),
                        resultSet.getString("campus"),
                        resultSet.getString("puesto"),
                        resultSet.getString("responsable")));
        long totalRecords = total == null ? 0 : total;
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        return new UsuariosPage(data, page, pageSize, totalRecords, totalPages);
    }

    public UsuariosCatalogosDto getCatalogs(String username) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar() && !module.acciones().editar() && !module.acciones().crear()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar catálogos de usuarios.");
        }

        var unidades = jdbcTemplate.query(
                "SELECT ID AS id, Nombre AS nombre FROM dbo.UA ORDER BY UPPER(LTRIM(RTRIM(Nombre)))",
                (resultSet, rowNum) -> new CatalogOptionDto(
                        resultSet.getInt("id"),
                        resultSet.getString("nombre")));
        var roles = jdbcTemplate.query(
                "SELECT id, nombre FROM dbo.Roles ORDER BY UPPER(LTRIM(RTRIM(nombre)))",
                (resultSet, rowNum) -> new CatalogOptionDto(
                        resultSet.getInt("id"),
                        resultSet.getString("nombre")));
        return new UsuariosCatalogosDto(unidades, roles);
    }

    public List<UsuarioRowDto> findExportRows(
            String username,
            String search,
            String sortBy,
            String sortOrder) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().exportar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para exportar usuarios.");
        }

        String sortField = SORT_FIELDS.getOrDefault(sortBy, "usuario");
        String direction = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        String filters = buildSearchFilter(search, params);
        String sql = """
                SELECT
                    u.Id AS id,
                    u.Status AS status,
                    u.Usuario AS usuario,
                    u.Nombre AS nombre,
                    u.UA AS ua,
                    ua.Nombre AS unidadEjecutoraGasto,
                    u.Area AS area,
                    roles.roles AS roles,
                    primaryRole.roleId AS roleId,
                    u.Campus AS campus,
                    u.Puesto AS puesto,
                    u.Responsable AS responsable
                FROM dbo.Usuarios u
                LEFT JOIN dbo.UA ua ON CONVERT(VARCHAR(10), ua.ID) = LTRIM(RTRIM(u.UA))
                OUTER APPLY (
                    SELECT STUFF((
                        SELECT DISTINCT ', ' + r.nombre
                        FROM dbo.Perfiles pf
                        INNER JOIN dbo.Roles r ON r.id = pf.id_Rol
                        WHERE pf.id_Usuario = u.Id
                        FOR XML PATH(''), TYPE
                    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS roles
                ) roles
                OUTER APPLY (
                    SELECT TOP 1 pf.id_Rol AS roleId
                    FROM dbo.Perfiles pf
                    WHERE pf.id_Usuario = u.Id
                    ORDER BY pf.id
                ) primaryRole
                WHERE 1 = 1
                %s
                ORDER BY %s %s, u.Id ASC
                """.formatted(filters, sortField, direction);

        return jdbcTemplate.query(sql, params, (resultSet, rowNum) ->
                new UsuarioRowDto(
                        resultSet.getInt("id"),
                        resultSet.getString("status"),
                        resultSet.getString("usuario"),
                        resultSet.getString("nombre"),
                        resultSet.getString("ua"),
                        resultSet.getString("unidadEjecutoraGasto"),
                        resultSet.getString("area"),
                        resultSet.getString("roles"),
                        getNullableInteger(resultSet, "roleId"),
                        resultSet.getString("campus"),
                        resultSet.getString("puesto"),
                        resultSet.getString("responsable")));
    }

    public UsuarioRowDto findOne(String username, Integer userId) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar usuarios.");
        }
        return findById(userId);
    }

    @Transactional
    public UsuarioRowDto create(String username, CreateUsuarioRequest request) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().crear()) {
            throw new PartidasAccessDeniedException("No tiene permiso para crear usuarios.");
        }

        String normalizedUsername = requireValue(request.usuario(), "El usuario es requerido.");
        if (usuarioRepository.findFirstByUsuarioIgnoreCase(normalizedUsername).isPresent()) {
            throw new UsuariosException("Ya existe un usuario registrado con ese nombre de usuario.");
        }

        Usuario usuario = new Usuario();
        usuario.setStatus("ACTIVO");
        usuario.setUsuario(normalizedUsername);
        usuario.setPassword(DEFAULT_PASSWORD);
        usuario.setNombre(requireValue(request.nombre(), "El nombre es requerido."));
        usuario.setUa(trimToNull(request.ua()));
        usuario.setCampus(findUnidadNombre(request.ua()).orElse(null));
        usuario.setArea(trimToNull(request.area()));
        usuario.setPuesto(trimToNull(request.puesto()));
        usuario.setResponsable(trimToNull(request.responsable()));
        usuario.setPermisos(DEFAULT_PERMISOS);
        usuario.setSia(DEFAULT_LEGACY_PROFILE);
        usuario.setRh(DEFAULT_LEGACY_PROFILE);
        usuario.setTesoreria(DEFAULT_LEGACY_PROFILE);
        usuario.setPaa(DEFAULT_LEGACY_PROFILE);
        usuario.setSistemas(DEFAULT_LEGACY_PROFILE);
        usuario.setViaticos(DEFAULT_LEGACY_PROFILE);
        usuario.setFondo(DEFAULT_LEGACY_PROFILE);
        Usuario saved = usuarioRepository.saveAndFlush(usuario);
        updateRole(saved.getId(), request.roleId());
        return findById(saved.getId());
    }

    @Transactional
    public UsuarioRowDto update(String username, Integer userId, UpdateUsuarioRequest request) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().editar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para editar usuarios.");
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new PartidasAccessDeniedException("El usuario solicitado no existe."));
        usuario.setStatus("ACTIVO");
        usuario.setNombre(trimToNull(request.nombre()));
        usuario.setUa(trimToNull(request.ua()));
        usuario.setCampus(findUnidadNombre(request.ua()).orElse(null));
        usuario.setArea(trimToNull(request.area()));
        usuario.setPuesto(trimToNull(request.puesto()));
        usuario.setResponsable(trimToNull(request.responsable()));
        usuarioRepository.saveAndFlush(usuario);
        updateRole(userId, request.roleId());
        return findById(userId);
    }

    @Transactional
    public UsuarioRowDto deactivate(String username, Integer userId) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().eliminar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para desactivar usuarios.");
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new PartidasAccessDeniedException("El usuario solicitado no existe."));
        usuario.setStatus("INACTIVO");
        usuarioRepository.saveAndFlush(usuario);
        return findById(userId);
    }

    @Transactional
    public UsuarioRowDto activate(String username, Integer userId) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().eliminar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para activar usuarios.");
        }

        Usuario usuario = usuarioRepository.findById(userId)
                .orElseThrow(() -> new PartidasAccessDeniedException("El usuario solicitado no existe."));
        usuario.setStatus("ACTIVO");
        usuarioRepository.saveAndFlush(usuario);
        return findById(userId);
    }

    private UsuarioRowDto findById(Integer userId) {
        String sql = """
                SELECT
                    u.Id AS id,
                    u.Status AS status,
                    u.Usuario AS usuario,
                    u.Nombre AS nombre,
                    u.UA AS ua,
                    ua.Nombre AS unidadEjecutoraGasto,
                    u.Area AS area,
                    roles.roles AS roles,
                    primaryRole.roleId AS roleId,
                    u.Campus AS campus,
                    u.Puesto AS puesto,
                    u.Responsable AS responsable
                FROM dbo.Usuarios u
                LEFT JOIN dbo.UA ua ON CONVERT(VARCHAR(10), ua.ID) = LTRIM(RTRIM(u.UA))
                OUTER APPLY (
                    SELECT STUFF((
                        SELECT DISTINCT ', ' + r.nombre
                        FROM dbo.Perfiles pf
                        INNER JOIN dbo.Roles r ON r.id = pf.id_Rol
                        WHERE pf.id_Usuario = u.Id
                        FOR XML PATH(''), TYPE
                    ).value('.', 'NVARCHAR(MAX)'), 1, 2, '') AS roles
                ) roles
                OUTER APPLY (
                    SELECT TOP 1 pf.id_Rol AS roleId
                    FROM dbo.Perfiles pf
                    WHERE pf.id_Usuario = u.Id
                    ORDER BY pf.id
                ) primaryRole
                WHERE u.Id = :id
                """;
        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("id", userId),
                (resultSet, rowNum) -> new UsuarioRowDto(
                        resultSet.getInt("id"),
                        resultSet.getString("status"),
                        resultSet.getString("usuario"),
                        resultSet.getString("nombre"),
                        resultSet.getString("ua"),
                        resultSet.getString("unidadEjecutoraGasto"),
                        resultSet.getString("area"),
                        resultSet.getString("roles"),
                        getNullableInteger(resultSet, "roleId"),
                        resultSet.getString("campus"),
                        resultSet.getString("puesto"),
                        resultSet.getString("responsable")));
    }

    private void updateRole(Integer userId, Integer roleId) {
        if (roleId == null) {
            throw new UsuariosException("El rol es requerido.");
        }
        Integer existingProfileId = jdbcTemplate.query(
                """
                SELECT TOP 1 id
                FROM dbo.Perfiles
                WHERE id_Usuario = :userId
                ORDER BY id
                """,
                new MapSqlParameterSource("userId", userId),
                resultSet -> resultSet.next() ? resultSet.getInt("id") : null);
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("roleId", roleId);
        if (existingProfileId != null) {
            params.addValue("profileId", existingProfileId);
            jdbcTemplate.update(
                    "UPDATE dbo.Perfiles SET id_Rol = :roleId WHERE id = :profileId",
                    params);
        } else {
            jdbcTemplate.update(
                    "INSERT INTO dbo.Perfiles (id_Rol, id_Usuario, alcance) VALUES (:roleId, :userId, 'LOCAL')",
                    params);
        }
    }

    private Optional<String> findUnidadNombre(String uaId) {
        if (uaId == null || uaId.isBlank()) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                "SELECT TOP 1 Nombre FROM dbo.UA WHERE CONVERT(VARCHAR(10), ID) = :uaId",
                new MapSqlParameterSource("uaId", uaId.trim()),
                resultSet -> resultSet.next()
                        ? Optional.ofNullable(resultSet.getString("Nombre"))
                        : Optional.empty());
    }

    private Integer getNullableInteger(java.sql.ResultSet resultSet, String column) throws java.sql.SQLException {
        int value = resultSet.getInt(column);
        return resultSet.wasNull() ? null : value;
    }

    private ModuloAutorizado resolveModule(String username) {
        Usuario usuario = usuarioRepository.findFirstByUsuarioIgnoreCase(username)
                .orElseThrow(() -> new PartidasAccessDeniedException("El usuario de la sesión no existe."));
        return authorizationService.getModulos(usuario.getId()).stream()
                .filter(module -> MODULE_NAME.equals(normalize(module.nombre())))
                .findFirst()
                .filter(module -> module.acciones().ejecutar())
                .orElseThrow(() -> new PartidasAccessDeniedException("No tiene acceso al módulo de usuarios."));
    }

    private String buildSearchFilter(String search, MapSqlParameterSource params) {
        if (search == null || search.isBlank()) {
            return "";
        }
        params.addValue("search", "%" + search.trim() + "%");
        return """
                 AND (
                    u.Usuario LIKE :search OR
                    u.Nombre LIKE :search OR
                    u.Status LIKE :search OR
                    u.UA LIKE :search OR
                    u.Area LIKE :search OR
                    EXISTS (
                        SELECT 1
                        FROM dbo.Perfiles pf
                        INNER JOIN dbo.Roles r ON r.id = pf.id_Rol
                        WHERE pf.id_Usuario = u.Id
                          AND r.nombre LIKE :search
                    ) OR
                    u.Puesto LIKE :search OR
                    u.Responsable LIKE :search
                 )
                """;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String requireValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new UsuariosException(message);
        }
        return value.trim();
    }
}
