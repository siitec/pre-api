package mx.tsj.connect.general.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.tsj.connect.general.dto.ModuloAutorizado;
import mx.tsj.connect.general.dto.PartidaCatalogoDto;
import mx.tsj.connect.general.dto.PartidaOptionDto;
import mx.tsj.connect.general.dto.PartidasCatalogoPage;
import mx.tsj.connect.general.dto.PartidasCatalogosDto;
import mx.tsj.connect.general.dto.SavePartidaRequest;
import mx.tsj.connect.general.entities.Catalogo;
import mx.tsj.connect.general.entities.Usuario;
import mx.tsj.connect.general.repositories.CatalogoRepository;
import mx.tsj.connect.general.repositories.UsuarioRepository;

@Service
public class PartidasCatalogoService {
    private static final String DEFAULT_MUESTRA = "NO";
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "id", "id",
            "partida", "partida",
            "partidaNombre", "partidaNombre",
            "descripcion", "descripcion",
            "um", "um",
            "muestra", "muestra");

    private final CatalogoRepository catalogoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Integer productosServiciosModuleId;

    public PartidasCatalogoService(
            CatalogoRepository catalogoRepository,
            UsuarioRepository usuarioRepository,
            AuthorizationService authorizationService,
            NamedParameterJdbcTemplate jdbcTemplate,
            @Value("${app.modules.productos-servicios-id:3}") Integer productosServiciosModuleId) {
        this.catalogoRepository = catalogoRepository;
        this.usuarioRepository = usuarioRepository;
        this.authorizationService = authorizationService;
        this.jdbcTemplate = jdbcTemplate;
        this.productosServiciosModuleId = productosServiciosModuleId;
    }

    public PartidasCatalogoPage findAll(
            String username,
            int requestedPage,
            int requestedPageSize,
            String search,
            String sortBy,
            String sortOrder) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar productos/servicios.");
        }

        int page = Math.max(requestedPage, 1);
        int pageSize = Math.min(Math.max(requestedPageSize, 1), 100);
        int startRow = ((page - 1) * pageSize) + 1;
        int endRow = page * pageSize;
        String sortField = SORT_FIELDS.getOrDefault(sortBy, "partida");
        String direction = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startRow", startRow)
                .addValue("endRow", endRow);
        String filters = buildSearchFilter(search, params);

        Long total = jdbcTemplate.queryForObject(
                """
                SELECT COUNT_BIG(*)
                FROM dbo.Catalogo c
                LEFT JOIN dbo.Partidas p ON p.clave = c.PARTIDA
                WHERE 1 = 1
                """ + filters,
                params,
                Long.class);

        String sql = """
                WITH partidasBase AS (
                    SELECT
                        c.ID AS id,
                        LTRIM(RTRIM(c.PARTIDA)) AS partida,
                        p.nombre AS partidaNombre,
                        c.DESCRIPCION AS descripcion,
                        c.UM AS um,
                        c.MUESTRA AS muestra
                    FROM dbo.Catalogo c
                    LEFT JOIN dbo.Partidas p ON p.clave = c.PARTIDA
                    WHERE 1 = 1
                    %s
                ),
                partidasPaginadas AS (
                    SELECT
                        ROW_NUMBER() OVER (ORDER BY %s %s, id ASC) AS rowNumber,
                        id, partida, partidaNombre, descripcion, um, muestra
                    FROM partidasBase
                )
                SELECT id, partida, partidaNombre, descripcion, um, muestra
                FROM partidasPaginadas
                WHERE rowNumber BETWEEN :startRow AND :endRow
                ORDER BY rowNumber
                """.formatted(filters, sortField, direction);

        var data = jdbcTemplate.query(sql, params, (resultSet, rowNum) ->
                new PartidaCatalogoDto(
                        resultSet.getInt("id"),
                        resultSet.getString("partida"),
                        resultSet.getString("partidaNombre"),
                        resultSet.getString("descripcion"),
                        resultSet.getString("um"),
                        resultSet.getString("muestra")));
        long totalRecords = total == null ? 0 : total;
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        return new PartidasCatalogoPage(data, page, pageSize, totalRecords, totalPages);
    }

    public PartidaCatalogoDto findOne(String username, Integer partidaId) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar() && !module.acciones().editar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar productos/servicios.");
        }
        return findById(partidaId);
    }

    public List<PartidaCatalogoDto> findExportRows(
            String username,
            String search,
            String sortBy,
            String sortOrder) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().exportar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para exportar productos/servicios.");
        }

        String sortField = SORT_FIELDS.getOrDefault(sortBy, "partida");
        String direction = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        String filters = buildSearchFilter(search, params);
        String sql = """
                SELECT
                    c.ID AS id,
                    LTRIM(RTRIM(c.PARTIDA)) AS partida,
                    p.nombre AS partidaNombre,
                    c.DESCRIPCION AS descripcion,
                    c.UM AS um,
                    c.MUESTRA AS muestra
                FROM dbo.Catalogo c
                LEFT JOIN dbo.Partidas p ON p.clave = c.PARTIDA
                WHERE 1 = 1
                %s
                ORDER BY %s %s, c.ID ASC
                """.formatted(filters, sortField, direction);

        return jdbcTemplate.query(sql, params, (resultSet, rowNum) ->
                new PartidaCatalogoDto(
                        resultSet.getInt("id"),
                        resultSet.getString("partida"),
                        resultSet.getString("partidaNombre"),
                        resultSet.getString("descripcion"),
                        resultSet.getString("um"),
                        resultSet.getString("muestra")));
    }

    public PartidasCatalogosDto getCatalogs(String username) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar() && !module.acciones().editar() && !module.acciones().crear()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar catálogos de productos/servicios.");
        }

        var partidas = jdbcTemplate.query(
                """
                SELECT LTRIM(RTRIM(clave)) AS clave, nombre
                FROM dbo.Partidas
                ORDER BY RIGHT('0000' + LTRIM(RTRIM(clave)), 4), LTRIM(RTRIM(clave))
                """,
                (resultSet, rowNum) -> new PartidaOptionDto(
                        resultSet.getString("clave"),
                        resultSet.getString("nombre")));
        return new PartidasCatalogosDto(partidas);
    }

    @Transactional
    public PartidaCatalogoDto create(String username, SavePartidaRequest request) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().crear()) {
            throw new PartidasAccessDeniedException("No tiene permiso para crear productos/servicios.");
        }

        String partidaClave = normalizePartida(request.partida());
        validatePartidaExists(partidaClave);

        Catalogo catalogo = new Catalogo();
        catalogo.setPartida(partidaClave);
        catalogo.setDescripcion(requireValue(request.descripcion(), "La descripción es requerida."));
        catalogo.setUm(trimToNull(request.um()));
        catalogo.setMuestra(DEFAULT_MUESTRA);
        Catalogo saved = catalogoRepository.saveAndFlush(catalogo);
        return findById(saved.getId());
    }

    @Transactional
    public PartidaCatalogoDto update(String username, Integer partidaId, SavePartidaRequest request) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().editar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para editar productos/servicios.");
        }

        Catalogo catalogo = findEntity(partidaId);
        String partidaClave = normalizePartida(request.partida());
        validatePartidaExists(partidaClave);

        catalogo.setPartida(partidaClave);
        catalogo.setDescripcion(requireValue(request.descripcion(), "La descripción es requerida."));
        catalogo.setUm(trimToNull(request.um()));
        catalogo.setMuestra(DEFAULT_MUESTRA);
        catalogoRepository.saveAndFlush(catalogo);
        return findById(partidaId);
    }

    private Catalogo findEntity(Integer catalogoId) {
        return catalogoRepository.findById(catalogoId)
                .orElseThrow(() -> new UsuariosException("El registro solicitado no existe."));
    }

    private PartidaCatalogoDto findById(Integer catalogoId) {
        String sql = """
                SELECT
                    c.ID AS id,
                    LTRIM(RTRIM(c.PARTIDA)) AS partida,
                    p.nombre AS partidaNombre,
                    c.DESCRIPCION AS descripcion,
                    c.UM AS um,
                    c.MUESTRA AS muestra
                FROM dbo.Catalogo c
                LEFT JOIN dbo.Partidas p ON p.clave = c.PARTIDA
                WHERE c.ID = :id
                """;
        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("id", catalogoId),
                (resultSet, rowNum) -> new PartidaCatalogoDto(
                        resultSet.getInt("id"),
                        resultSet.getString("partida"),
                        resultSet.getString("partidaNombre"),
                        resultSet.getString("descripcion"),
                        resultSet.getString("um"),
                        resultSet.getString("muestra")));
    }

    private void validatePartidaExists(String partidaClave) {
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT CASE WHEN EXISTS (SELECT 1 FROM dbo.Partidas WHERE clave = :partida) THEN 1 ELSE 0 END",
                new MapSqlParameterSource("partida", partidaClave),
                Integer.class);
        if (exists == null || exists == 0) {
            throw new UsuariosException("La partida seleccionada no existe en el catálogo de partidas.");
        }
    }

    private ModuloAutorizado resolveModule(String username) {
        Usuario usuario = usuarioRepository.findFirstByUsuarioIgnoreCase(username)
                .orElseThrow(() -> new PartidasAccessDeniedException("El usuario de la sesión no existe."));
        return authorizationService.getModulos(usuario.getId()).stream()
                .filter(module -> productosServiciosModuleId.equals(module.id()))
                .findFirst()
                .filter(module -> module.acciones().ejecutar())
                .orElseThrow(() -> new PartidasAccessDeniedException(
                        "No tiene acceso al módulo de productos/servicios."));
    }

    private String buildSearchFilter(String search, MapSqlParameterSource params) {
        if (search == null || search.isBlank()) {
            return "";
        }
        params.addValue("search", "%" + search.trim() + "%");
        return """
                 AND (
                    c.PARTIDA LIKE :search OR
                    p.nombre LIKE :search OR
                    c.DESCRIPCION LIKE :search OR
                    c.UM LIKE :search OR
                    c.MUESTRA LIKE :search
                 )
                """;
    }

    private String normalizePartida(String value) {
        String partida = requireValue(value, "La partida es requerida.").toUpperCase();
        if (partida.length() > 4) {
            throw new UsuariosException("La partida no puede exceder 4 caracteres.");
        }
        return partida;
    }

    private String requireValue(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new UsuariosException(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
