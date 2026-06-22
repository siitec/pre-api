package mx.tsj.connect.general.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Permisos", schema = "dbo")
public class Permiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_Modulo")
    private Integer idModulo;

    @Column(name = "id_Rol")
    private Integer idRol;

    @Column(name = "consultar", length = 100, columnDefinition = "char(100)")
    private String consultar;

    @Column(name = "crear", length = 100, columnDefinition = "char(100)")
    private String crear;

    @Column(name = "editar", length = 100, columnDefinition = "char(100)")
    private String editar;

    @Column(name = "eliminar", length = 100, columnDefinition = "char(100)")
    private String eliminar;

    @Column(name = "ejecutar", length = 100, columnDefinition = "char(100)")
    private String ejecutar;

    @Column(name = "autorizar", length = 100, columnDefinition = "char(100)")
    private String autorizar;

    @Column(name = "exportar", length = 100, columnDefinition = "char(100)")
    private String exportar;

    public Integer getId() { return id; }
    public Integer getIdModulo() { return idModulo; }
    public Integer getIdRol() { return idRol; }
    public String getConsultar() { return consultar; }
    public String getCrear() { return crear; }
    public String getEditar() { return editar; }
    public String getEliminar() { return eliminar; }
    public String getEjecutar() { return ejecutar; }
    public String getAutorizar() { return autorizar; }
    public String getExportar() { return exportar; }
}
