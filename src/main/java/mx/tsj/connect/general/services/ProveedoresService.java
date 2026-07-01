package mx.tsj.connect.general.services;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mx.tsj.connect.general.dto.CatalogOptionDto;
import mx.tsj.connect.general.dto.ModuloAutorizado;
import mx.tsj.connect.general.dto.MunicipioOptionDto;
import mx.tsj.connect.general.dto.ProveedorDto;
import mx.tsj.connect.general.dto.ProveedoresCatalogosDto;
import mx.tsj.connect.general.dto.ProveedoresPage;
import mx.tsj.connect.general.dto.SaveProveedorRequest;
import mx.tsj.connect.general.entities.Proveedor;
import mx.tsj.connect.general.entities.Usuario;
import mx.tsj.connect.general.repositories.ProveedorRepository;
import mx.tsj.connect.general.repositories.UsuarioRepository;

@Service
public class ProveedoresService {
    private static final Map<String, String> SORT_FIELDS = Map.of(
            "codigo", "codigo",
            "nombre", "nombre",
            "rfc", "rfc",
            "email", "email",
            "telefono1", "telefono1",
            "colonia", "colonia",
            "codigoP", "codigoP",
            "ciudad", "ciudad",
            "estado", "estado",
            "representante", "representante");

    private final ProveedorRepository proveedorRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuthorizationService authorizationService;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Integer proveedoresModuleId;

    public ProveedoresService(
            ProveedorRepository proveedorRepository,
            UsuarioRepository usuarioRepository,
            AuthorizationService authorizationService,
            NamedParameterJdbcTemplate jdbcTemplate,
            @Value("${app.modules.proveedores-id:4}") Integer proveedoresModuleId) {
        this.proveedorRepository = proveedorRepository;
        this.usuarioRepository = usuarioRepository;
        this.authorizationService = authorizationService;
        this.jdbcTemplate = jdbcTemplate;
        this.proveedoresModuleId = proveedoresModuleId;
    }

    public ProveedoresPage findAll(
            String username,
            int requestedPage,
            int requestedPageSize,
            String search,
            String sortBy,
            String sortOrder) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar proveedores.");
        }

        int page = Math.max(requestedPage, 1);
        int pageSize = Math.min(Math.max(requestedPageSize, 1), 100);
        int startRow = ((page - 1) * pageSize) + 1;
        int endRow = page * pageSize;
        String sortField = SORT_FIELDS.getOrDefault(sortBy, "nombre");
        String direction = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("startRow", startRow)
                .addValue("endRow", endRow);
        String filters = buildSearchFilter(search, params);

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT_BIG(*) FROM dbo.Proveedores p WHERE 1 = 1 " + filters,
                params,
                Long.class);

        String sql = """
                WITH proveedoresBase AS (
                    SELECT
                        LTRIM(RTRIM(p.Codigo)) AS codigo,
                        p.Nombre AS nombre,
                        p.RFC AS rfc,
                        p.Calle AS calle,
                        p.NumExt AS numExt,
                        p.NumInt AS numInt,
                        p.Colonia AS colonia,
                        p.CodigoP AS codigoP,
                        p.Telefono1 AS telefono1,
                        p.Telefono2 AS telefono2,
                        p.Telefono3 AS telefono3,
                        p.Telefono4 AS telefono4,
                        p.Email AS email,
                        p.Municipio AS municipio,
                        p.Ciudad AS ciudad,
                        p.Estado AS estado,
                        p.Pais AS pais,
                        p.Representante AS representante
                    FROM dbo.Proveedores p
                    WHERE 1 = 1
                    %s
                ),
                proveedoresPaginados AS (
                    SELECT
                        ROW_NUMBER() OVER (ORDER BY %s %s, codigo ASC) AS rowNumber,
                        codigo, nombre, rfc, calle, numExt, numInt, colonia, codigoP,
                        telefono1, telefono2, telefono3, telefono4, email, municipio,
                        ciudad, estado, pais, representante
                    FROM proveedoresBase
                )
                SELECT
                    codigo, nombre, rfc, calle, numExt, numInt, colonia, codigoP,
                    telefono1, telefono2, telefono3, telefono4, email, municipio,
                    ciudad, estado, pais, representante
                FROM proveedoresPaginados
                WHERE rowNumber BETWEEN :startRow AND :endRow
                ORDER BY rowNumber
                """.formatted(filters, sortField, direction);

        var data = jdbcTemplate.query(sql, params, (resultSet, rowNum) -> mapRow(resultSet));
        long totalRecords = total == null ? 0 : total;
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        return new ProveedoresPage(data, page, pageSize, totalRecords, totalPages);
    }

    public ProveedorDto findOne(String username, String codigo) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar() && !module.acciones().editar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar proveedores.");
        }
        return findByCodigo(codigo);
    }

    public ProveedoresCatalogosDto getCatalogs(String username) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().consultar() && !module.acciones().editar() && !module.acciones().crear()) {
            throw new PartidasAccessDeniedException("No tiene permiso para consultar catálogos de proveedores.");
        }

        var estados = jdbcTemplate.query(
                """
                SELECT id, nombre
                FROM dbo.Estados
                ORDER BY UPPER(LTRIM(RTRIM(nombre)))
                """,
                (resultSet, rowNum) -> new CatalogOptionDto(
                        resultSet.getInt("id"),
                        resultSet.getString("nombre")));
        var municipios = jdbcTemplate.query(
                """
                SELECT id, id_estado AS idEstado, nombre
                FROM dbo.Municipios
                ORDER BY id_estado, UPPER(LTRIM(RTRIM(nombre)))
                """,
                (resultSet, rowNum) -> new MunicipioOptionDto(
                        resultSet.getInt("id"),
                        resultSet.getInt("idEstado"),
                        resultSet.getString("nombre")));
        return new ProveedoresCatalogosDto(estados, municipios);
    }

    public List<ProveedorDto> findExportRows(
            String username,
            String search,
            String sortBy,
            String sortOrder) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().exportar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para exportar proveedores.");
        }

        String sortField = SORT_FIELDS.getOrDefault(sortBy, "nombre");
        String direction = "desc".equalsIgnoreCase(sortOrder) ? "DESC" : "ASC";
        MapSqlParameterSource params = new MapSqlParameterSource();
        String filters = buildSearchFilter(search, params);
        String sql = """
                SELECT
                    LTRIM(RTRIM(p.Codigo)) AS codigo,
                    p.Nombre AS nombre,
                    p.RFC AS rfc,
                    p.Calle AS calle,
                    p.NumExt AS numExt,
                    p.NumInt AS numInt,
                    p.Colonia AS colonia,
                    p.CodigoP AS codigoP,
                    p.Telefono1 AS telefono1,
                    p.Telefono2 AS telefono2,
                    p.Telefono3 AS telefono3,
                    p.Telefono4 AS telefono4,
                    p.Email AS email,
                    p.Municipio AS municipio,
                    p.Ciudad AS ciudad,
                    p.Estado AS estado,
                    p.Pais AS pais,
                    p.Representante AS representante
                FROM dbo.Proveedores p
                WHERE 1 = 1
                %s
                ORDER BY %s %s, codigo ASC
                """.formatted(filters, sortField, direction);

        return jdbcTemplate.query(sql, params, (resultSet, rowNum) -> mapRow(resultSet));
    }

    @Transactional
    public ProveedorDto create(String username, SaveProveedorRequest request) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().crear()) {
            throw new PartidasAccessDeniedException("No tiene permiso para crear proveedores.");
        }

        String codigo = requireValue(request.codigo(), "El código es requerido.");
        if (proveedorRepository.existsById(codigo)) {
            throw new UsuariosException("Ya existe un proveedor con ese código.");
        }

        Proveedor proveedor = new Proveedor();
        proveedor.setCodigo(codigo);
        applyRequest(proveedor, request);
        proveedorRepository.saveAndFlush(proveedor);
        return findByCodigo(codigo);
    }

    @Transactional
    public ProveedorDto update(String username, String codigo, SaveProveedorRequest request) {
        ModuloAutorizado module = resolveModule(username);
        if (!module.acciones().editar()) {
            throw new PartidasAccessDeniedException("No tiene permiso para editar proveedores.");
        }

        Proveedor proveedor = proveedorRepository.findById(codigo)
                .orElseThrow(() -> new UsuariosException("El proveedor solicitado no existe."));
        applyRequest(proveedor, request);
        proveedorRepository.saveAndFlush(proveedor);
        return findByCodigo(codigo);
    }

    private void applyRequest(Proveedor proveedor, SaveProveedorRequest request) {
        proveedor.setNombre(requireValue(request.nombre(), "El nombre es requerido."));
        proveedor.setRfc(trimToNull(request.rfc()));
        proveedor.setCalle(trimToNull(request.calle()));
        proveedor.setNumExt(trimToNull(request.numExt()));
        proveedor.setNumInt(trimToNull(request.numInt()));
        proveedor.setColonia(trimToNull(request.colonia()));
        proveedor.setCodigoP(trimToNull(request.codigoP()));
        proveedor.setTelefono1(trimToNull(request.telefono1()));
        proveedor.setTelefono2(trimToNull(request.telefono2()));
        proveedor.setTelefono3(trimToNull(request.telefono3()));
        proveedor.setTelefono4(trimToNull(request.telefono4()));
        proveedor.setEmail(trimToNull(request.email()));
        proveedor.setMunicipio(trimToNull(request.municipio()));
        proveedor.setCiudad(trimToNull(request.ciudad()));
        proveedor.setEstado(trimToNull(request.estado()));
        proveedor.setPais(trimToNull(request.pais()));
        proveedor.setRepresentante(trimToNull(request.representante()));
    }

    private ProveedorDto findByCodigo(String codigo) {
        String sql = """
                SELECT
                    LTRIM(RTRIM(p.Codigo)) AS codigo,
                    p.Nombre AS nombre,
                    p.RFC AS rfc,
                    p.Calle AS calle,
                    p.NumExt AS numExt,
                    p.NumInt AS numInt,
                    p.Colonia AS colonia,
                    p.CodigoP AS codigoP,
                    p.Telefono1 AS telefono1,
                    p.Telefono2 AS telefono2,
                    p.Telefono3 AS telefono3,
                    p.Telefono4 AS telefono4,
                    p.Email AS email,
                    p.Municipio AS municipio,
                    p.Ciudad AS ciudad,
                    p.Estado AS estado,
                    p.Pais AS pais,
                    p.Representante AS representante
                FROM dbo.Proveedores p
                WHERE p.Codigo = :codigo
                """;
        return jdbcTemplate.queryForObject(
                sql,
                new MapSqlParameterSource("codigo", codigo),
                (resultSet, rowNum) -> mapRow(resultSet));
    }

    private ModuloAutorizado resolveModule(String username) {
        Usuario usuario = usuarioRepository.findFirstByUsuarioIgnoreCase(username)
                .orElseThrow(() -> new PartidasAccessDeniedException("El usuario de la sesión no existe."));
        return authorizationService.getModulos(usuario.getId()).stream()
                .filter(module -> proveedoresModuleId.equals(module.id()))
                .findFirst()
                .filter(module -> module.acciones().ejecutar())
                .orElseThrow(() -> new PartidasAccessDeniedException("No tiene acceso al módulo de proveedores."));
    }

    private String buildSearchFilter(String search, MapSqlParameterSource params) {
        if (search == null || search.isBlank()) {
            return "";
        }
        params.addValue("search", "%" + search.trim() + "%");
        return """
                 AND (
                    p.Codigo LIKE :search OR
                    p.Nombre LIKE :search OR
                    p.RFC LIKE :search OR
                    p.Email LIKE :search OR
                    p.Telefono1 LIKE :search OR
                    p.Ciudad LIKE :search OR
                    p.Estado LIKE :search OR
                    p.Representante LIKE :search
                 )
                """;
    }

    private ProveedorDto mapRow(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new ProveedorDto(
                resultSet.getString("codigo"),
                resultSet.getString("nombre"),
                resultSet.getString("rfc"),
                resultSet.getString("calle"),
                resultSet.getString("numExt"),
                resultSet.getString("numInt"),
                resultSet.getString("colonia"),
                resultSet.getString("codigoP"),
                resultSet.getString("telefono1"),
                resultSet.getString("telefono2"),
                resultSet.getString("telefono3"),
                resultSet.getString("telefono4"),
                resultSet.getString("email"),
                resultSet.getString("municipio"),
                resultSet.getString("ciudad"),
                resultSet.getString("estado"),
                resultSet.getString("pais"),
                resultSet.getString("representante"));
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
