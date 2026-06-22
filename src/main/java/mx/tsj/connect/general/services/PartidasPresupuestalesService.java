package mx.tsj.connect.general.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import mx.tsj.connect.general.dto.DashboardEstatusDto;
import mx.tsj.connect.general.dto.DashboardPartidaDto;
import mx.tsj.connect.general.dto.DashboardPresupuestoDto;
import mx.tsj.connect.general.dto.DashboardSolicitudDto;
import mx.tsj.connect.general.dto.ModuloAutorizado;
import mx.tsj.connect.general.dto.PartidaPresupuestalDetalleDto;
import mx.tsj.connect.general.dto.PartidaPresupuestalExportDto;
import mx.tsj.connect.general.dto.PartidaPresupuestalResumenDto;
import mx.tsj.connect.general.dto.PartidasPresupuestalesPage;
import mx.tsj.connect.general.entities.Usuario;
import mx.tsj.connect.general.repositories.UsuarioRepository;

@Service
public class PartidasPresupuestalesService {
    private static final int MODULE_ID = 1;
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "cog", "cog",
            "descripcion", "descripcion",
            "monto", "monto",
            "comprometido", "comprometido",
            "ejercido", "ejercido",
            "total", "total");
    private static final List<SolicitudType> SOLICITUD_TYPES = List.of(
            new SolicitudType(
                    "REQUISICIONES",
                    "Requisiciones",
                    List.of("AUTORIZADA", "CONCLUIDA", "EN ESPERA DE AUTORIZACION", "RECHAZADA")),
            new SolicitudType(
                    "CAJA_CHICA",
                    "Caja chica",
                    List.of("AUTORIZADA", "CONCLUIDA", "EN ESPERA DE AUTORIZACION", "RECHAZADA")),
            new SolicitudType(
                    "CAPITULO_CUATRO",
                    "Capítulo cuatro",
                    List.of("AUTORIZADA", "CONCLUIDA", "EN ESPERA DE AUTORIZACION", "RECHAZADA")),
            new SolicitudType(
                    "VIATICOS",
                    "Viáticos",
                    List.of("VALIDADA", "SOLICITUD", "COMPROBACION")));

    private static final String FROM_AND_FILTERS = """
            FROM dbo.Clave c
            INNER JOIN dbo.UA ua ON ua.UEG = c.UEG
            INNER JOIN (
                SELECT clave, MAX(nombre) AS nombre
                FROM dbo.Partidas
                GROUP BY clave
            ) partida ON partida.clave = c.COG
            WHERE c.COG IN (:partidas)
            """;

    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public PartidasPresupuestalesService(
            UsuarioRepository usuarioRepository,
            AuthorizationService authorizationService,
            NamedParameterJdbcTemplate jdbcTemplate) {
        this.usuarioRepository = usuarioRepository;
        this.authorizationService = authorizationService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public PartidasPresupuestalesPage findAll(
            String username,
            int requestedPage,
            int requestedPageSize,
            String search,
            String sortBy,
            String sortOrder) {
        AccessContext access = resolveAccess(username);
        if (!access.hasDataScope()) {
            return emptyPage(requestedPage, requestedPageSize);
        }

        int page = Math.max(requestedPage, 1);
        int pageSize = Math.min(Math.max(requestedPageSize, 1), 100);
        int startRow = ((page - 1) * pageSize) + 1;
        int endRow = page * pageSize;
        String sortField = SORT_FIELDS.getOrDefault(sortBy, "cog");
        String direction = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        MapSqlParameterSource params = createParams(access)
                .addValue("startRow", startRow)
                .addValue("endRow", endRow);
        String filters = buildFilters(access, search, params);

        String countSql = "SELECT COUNT(DISTINCT c.COG) " + FROM_AND_FILTERS + filters;
        Long total = jdbcTemplate.queryForObject(countSql, params, Long.class);

        String dataSql = """
                WITH resumenBase AS (
                    SELECT
                        c.COG AS cog,
                        MAX(partida.nombre) AS descripcion,
                        SUM(COALESCE(c.Monto, 0)) AS monto,
                        SUM(COALESCE(c.Comprometido, 0)) AS comprometido,
                        SUM(COALESCE(c.Ejercido, 0)) AS ejercido,
                        SUM(COALESCE(c.Monto, 0) + COALESCE(c.Comprometido, 0) +
                            COALESCE(c.Ejercido, 0)) AS total
                    %s%s
                    GROUP BY c.COG
                ),
                resumenPartidas AS (
                    SELECT
                        ROW_NUMBER() OVER (ORDER BY %s %s, cog ASC) AS rowNumber,
                        cog, descripcion,
                        monto, comprometido, ejercido, total
                    FROM resumenBase
                )
                SELECT cog, descripcion,
                       monto, comprometido, ejercido, total
                FROM resumenPartidas
                WHERE rowNumber BETWEEN :startRow AND :endRow
                ORDER BY rowNumber
                """.formatted(FROM_AND_FILTERS, filters, sortField, direction);

        List<PartidaPresupuestalResumenDto> data = jdbcTemplate.query(dataSql, params, (resultSet, rowNum) ->
                new PartidaPresupuestalResumenDto(
                        resultSet.getString("cog"),
                        resultSet.getString("descripcion"),
                        resultSet.getBigDecimal("monto"),
                        resultSet.getBigDecimal("comprometido"),
                        resultSet.getBigDecimal("ejercido"),
                        resultSet.getBigDecimal("total")));
        long totalRecords = total == null ? 0 : total;
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        return new PartidasPresupuestalesPage(data, page, pageSize, totalRecords, totalPages);
    }

    public List<PartidaPresupuestalDetalleDto> findDetails(String username, String cog) {
        AccessContext access = resolveAccess(username);
        String normalizedCog = cog == null ? "" : cog.trim();
        if (!access.hasDataScope() || !access.module().partidas().contains(normalizedCog)) {
            throw new PartidasAccessDeniedException("No tiene acceso a la partida solicitada.");
        }

        MapSqlParameterSource params = createParams(access).addValue("cog", normalizedCog);
        String filters = buildFilters(access, null, params) + " AND c.COG = :cog";
        String detailSql = """
                SELECT
                    c.UEG AS ueg,
                    ua.Nombre AS unidadEjecutora,
                    SUM(COALESCE(c.Monto, 0)) AS monto,
                    SUM(COALESCE(c.Comprometido, 0)) AS comprometido,
                    SUM(COALESCE(c.Ejercido, 0)) AS ejercido,
                    SUM(COALESCE(c.Monto, 0) + COALESCE(c.Comprometido, 0) +
                        COALESCE(c.Ejercido, 0)) AS total
                %s%s
                GROUP BY c.UEG, ua.Nombre
                ORDER BY c.UEG
                """.formatted(FROM_AND_FILTERS, filters);

        return jdbcTemplate.query(detailSql, params, (resultSet, rowNum) ->
                new PartidaPresupuestalDetalleDto(
                        resultSet.getString("ueg"),
                        resultSet.getString("unidadEjecutora"),
                        resultSet.getBigDecimal("monto"),
                        resultSet.getBigDecimal("comprometido"),
                        resultSet.getBigDecimal("ejercido"),
                        resultSet.getBigDecimal("total")));
    }

    public List<PartidaPresupuestalExportDto> findExportRows(String username, String search) {
        AccessContext access = resolveAccess(username);
        if (!access.module().acciones().exportar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para exportar partidas presupuestales.");
        }
        if (!access.hasDataScope()) {
            return List.of();
        }

        MapSqlParameterSource params = createParams(access);
        String filters = buildFilters(access, search, params);
        String exportSql = """
                SELECT
                    c.COG AS cog,
                    MAX(partida.nombre) AS descripcion,
                    c.UEG AS ueg,
                    ua.Nombre AS unidadEjecutora,
                    SUM(COALESCE(c.Monto, 0)) AS monto,
                    SUM(COALESCE(c.Comprometido, 0)) AS comprometido,
                    SUM(COALESCE(c.Ejercido, 0)) AS ejercido,
                    SUM(COALESCE(c.Monto, 0) + COALESCE(c.Comprometido, 0) +
                        COALESCE(c.Ejercido, 0)) AS total
                %s%s
                GROUP BY c.COG, c.UEG, ua.Nombre
                ORDER BY c.COG, c.UEG
                """.formatted(FROM_AND_FILTERS, filters);

        return jdbcTemplate.query(exportSql, params, (resultSet, rowNum) ->
                new PartidaPresupuestalExportDto(
                        resultSet.getString("cog"),
                        resultSet.getString("descripcion"),
                        resultSet.getString("ueg"),
                        resultSet.getString("unidadEjecutora"),
                        resultSet.getBigDecimal("monto"),
                        resultSet.getBigDecimal("comprometido"),
                        resultSet.getBigDecimal("ejercido"),
                        resultSet.getBigDecimal("total")));
    }

    public DashboardPresupuestoDto getDashboard(String username) {
        AccessContext access = resolveAccess(username);
        List<DashboardSolicitudDto> solicitudes = getSolicitudSummary(username);
        if (!access.hasDataScope()) {
            return emptyDashboard(solicitudes);
        }

        MapSqlParameterSource params = createParams(access);
        String filters = buildFilters(access, null, params);
        String totalsSql = """
                SELECT
                    COALESCE(SUM(COALESCE(c.Monto, 0) + COALESCE(c.Comprometido, 0) +
                        COALESCE(c.Ejercido, 0)), 0) AS presupuestoAsignado,
                    COALESCE(SUM(COALESCE(c.Monto, 0)), 0) AS disponible,
                    COALESCE(SUM(COALESCE(c.Comprometido, 0)), 0) AS comprometido,
                    COALESCE(SUM(COALESCE(c.Ejercido, 0)), 0) AS ejercido,
                    COUNT(DISTINCT c.COG) AS partidas
                %s%s
                """.formatted(FROM_AND_FILTERS, filters);
        DashboardPresupuestoDto totals = jdbcTemplate.queryForObject(totalsSql, params, (resultSet, rowNum) ->
                new DashboardPresupuestoDto(
                        resultSet.getBigDecimal("presupuestoAsignado"),
                        resultSet.getBigDecimal("disponible"),
                        resultSet.getBigDecimal("comprometido"),
                        resultSet.getBigDecimal("ejercido"),
                        resultSet.getLong("partidas"),
                        List.of(),
                        List.of()));

        String chartSql = """
                SELECT
                    c.COG AS cog,
                    MAX(partida.nombre) AS descripcion,
                    SUM(COALESCE(c.Monto, 0)) AS monto,
                    SUM(COALESCE(c.Comprometido, 0)) AS comprometido,
                    SUM(COALESCE(c.Ejercido, 0)) AS ejercido
                %s%s
                GROUP BY c.COG
                ORDER BY SUM(COALESCE(c.Monto, 0)) DESC, c.COG
                """.formatted(FROM_AND_FILTERS, filters);
        List<DashboardPartidaDto> principales = jdbcTemplate.query(chartSql, params, (resultSet, rowNum) ->
                new DashboardPartidaDto(
                        resultSet.getString("cog"),
                        resultSet.getString("descripcion"),
                        resultSet.getBigDecimal("monto"),
                        resultSet.getBigDecimal("comprometido"),
                        resultSet.getBigDecimal("ejercido")));
        if (totals == null) {
            return emptyDashboard(solicitudes);
        }
        return new DashboardPresupuestoDto(
                totals.presupuestoAsignado(),
                totals.disponible(),
                totals.comprometido(),
                totals.ejercido(),
                totals.partidas(),
                principales,
                solicitudes);
    }

    private List<DashboardSolicitudDto> getSolicitudSummary(String username) {
        Map<String, LinkedHashMap<String, Long>> counts = new LinkedHashMap<>();
        for (SolicitudType type : SOLICITUD_TYPES) {
            LinkedHashMap<String, Long> statuses = new LinkedHashMap<>();
            type.statuses().forEach(status -> statuses.put(status, 0L));
            counts.put(type.id(), statuses);
        }

        String sql = """
                SELECT tipo, estatus, COUNT_BIG(*) AS total
                FROM (
                    SELECT 'REQUISICIONES' AS tipo,
                           COALESCE(NULLIF(UPPER(LTRIM(RTRIM(Status))), ''), 'SIN ESTATUS') AS estatus
                    FROM dbo.KardexSolicitudes
                    WHERE UPPER(LTRIM(RTRIM(Usuario))) = UPPER(:username)
                    UNION ALL
                    SELECT 'CAJA_CHICA',
                           COALESCE(NULLIF(UPPER(LTRIM(RTRIM(Status))), ''), 'SIN ESTATUS')
                    FROM dbo.KardexSolicitudesCaja
                    WHERE UPPER(LTRIM(RTRIM(Usuario))) = UPPER(:username)
                    UNION ALL
                    SELECT 'CAPITULO_CUATRO',
                           COALESCE(NULLIF(UPPER(LTRIM(RTRIM(Status))), ''), 'SIN ESTATUS')
                    FROM dbo.KardexSolicitudesCapituloCuatro
                    WHERE UPPER(LTRIM(RTRIM(Usuario))) = UPPER(:username)
                    UNION ALL
                    SELECT 'VIATICOS',
                           COALESCE(NULLIF(UPPER(LTRIM(RTRIM(Status))), ''), 'SIN ESTATUS')
                    FROM dbo.KardexViaticos
                    WHERE UPPER(LTRIM(RTRIM(Usuario))) = UPPER(:username)
                ) registros
                WHERE estatus <> 'DOCUMENTOS EN DRIVE'
                GROUP BY tipo, estatus
                """;
        MapSqlParameterSource params = new MapSqlParameterSource().addValue("username", username.trim());
        jdbcTemplate.query(sql, params, resultSet -> {
            LinkedHashMap<String, Long> statuses = counts.get(resultSet.getString("tipo"));
            if (statuses != null) {
                statuses.put(resultSet.getString("estatus"), resultSet.getLong("total"));
            }
        });

        List<DashboardSolicitudDto> result = new ArrayList<>();
        for (SolicitudType type : SOLICITUD_TYPES) {
            LinkedHashMap<String, Long> statuses = counts.get(type.id());
            List<DashboardEstatusDto> statusRows = statuses.entrySet().stream()
                    .map(entry -> new DashboardEstatusDto(entry.getKey(), entry.getValue()))
                    .toList();
            long total = statuses.values().stream().mapToLong(Long::longValue).sum();
            result.add(new DashboardSolicitudDto(type.id(), type.name(), total, statusRows));
        }
        return result;
    }

    private AccessContext resolveAccess(String username) {
        Usuario usuario = usuarioRepository.findFirstByUsuarioIgnoreCase(username)
                .orElseThrow(() -> new PartidasAccessDeniedException("El usuario de la sesión no existe."));
        ModuloAutorizado module = authorizationService.getModulos(usuario.getId()).stream()
                .filter(item -> item.id().equals(MODULE_ID))
                .findFirst()
                .orElseThrow(() -> new PartidasAccessDeniedException(
                        "No tiene acceso al módulo de partidas presupuestales."));

        if (!module.acciones().ejecutar() || !module.acciones().consultar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar partidas presupuestales.");
        }

        boolean global = "GLOBAL".equals(module.alcance());
        Integer localUaId = global ? null : parseUaId(usuario.getUa());
        return new AccessContext(module, global, localUaId);
    }

    private MapSqlParameterSource createParams(AccessContext access) {
        return new MapSqlParameterSource().addValue("partidas", access.module().partidas());
    }

    private String buildFilters(
            AccessContext access,
            String search,
            MapSqlParameterSource params) {
        StringBuilder filters = new StringBuilder();
        if (!access.global()) {
            filters.append(" AND ua.ID = :uaId");
            params.addValue("uaId", access.localUaId());
        }
        if (search != null && !search.isBlank()) {
            filters.append("""
                     AND (
                        c.COG LIKE :search OR
                        partida.nombre LIKE :search
                     )
                    """);
            params.addValue("search", "%" + search.trim() + "%");
        }
        return filters.toString();
    }

    private Integer parseUaId(String userUa) {
        if (userUa == null || userUa.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(userUa.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private PartidasPresupuestalesPage emptyPage(int requestedPage, int requestedPageSize) {
        int page = Math.max(requestedPage, 1);
        int pageSize = Math.min(Math.max(requestedPageSize, 1), 100);
        return new PartidasPresupuestalesPage(List.of(), page, pageSize, 0, 0);
    }

    private DashboardPresupuestoDto emptyDashboard(List<DashboardSolicitudDto> solicitudes) {
        return new DashboardPresupuestoDto(
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0,
                List.of(),
                solicitudes);
    }

    private record AccessContext(ModuloAutorizado module, boolean global, Integer localUaId) {
        private boolean hasDataScope() {
            return !module.partidas().isEmpty() && (global || localUaId != null);
        }
    }

    private record SolicitudType(String id, String name, List<String> statuses) {
    }
}
