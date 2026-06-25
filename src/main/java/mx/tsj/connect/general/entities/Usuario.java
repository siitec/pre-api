package mx.tsj.connect.general.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Usuarios", schema = "dbo")
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id")
    private Integer id;

    @Column(name = "Status", length = 10)
    private String status;

    @Column(name = "Usuario", length = 50)
    private String usuario;

    @Column(name = "Nombre", length = 200)
    private String nombre;

    @Column(name = "Password", length = 50)
    private String password;

    @Column(name = "UA", length = 2)
    private String ua;

    @Column(name = "Permisos", length = 72)
    private String permisos;

    @Column(name = "SIA", length = 50)
    private String sia;

    @Column(name = "RH", length = 50)
    private String rh;

    @Column(name = "TESORERIA", length = 50)
    private String tesoreria;

    @Column(name = "PAA", length = 50)
    private String paa;

    @Column(name = "Sistemas", length = 50)
    private String sistemas;

    @Column(name = "Viaticos", length = 50)
    private String viaticos;

    @Column(name = "Fondo", length = 50)
    private String fondo;

    @Column(name = "Area", length = 100)
    private String area;

    @Column(name = "Puesto", length = 100)
    private String puesto;

    @Column(name = "Responsable", length = 200)
    private String responsable;

    @Column(name = "Campus", length = 50)
    private String campus;

    public Integer getId() { return id; }
    public String getStatus() { return status; }
    public String getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public String getPassword() { return password; }
    public String getUa() { return ua; }
    public String getPermisos() { return permisos; }
    public String getSia() { return sia; }
    public String getRh() { return rh; }
    public String getTesoreria() { return tesoreria; }
    public String getPaa() { return paa; }
    public String getSistemas() { return sistemas; }
    public String getViaticos() { return viaticos; }
    public String getFondo() { return fondo; }
    public String getArea() { return area; }
    public String getPuesto() { return puesto; }
    public String getResponsable() { return responsable; }
    public String getCampus() { return campus; }

    public void setPassword(String password) { this.password = password; }
    public void setStatus(String status) { this.status = status; }
    public void setUsuario(String usuario) { this.usuario = usuario; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setUa(String ua) { this.ua = ua; }
    public void setPermisos(String permisos) { this.permisos = permisos; }
    public void setSia(String sia) { this.sia = sia; }
    public void setRh(String rh) { this.rh = rh; }
    public void setTesoreria(String tesoreria) { this.tesoreria = tesoreria; }
    public void setPaa(String paa) { this.paa = paa; }
    public void setSistemas(String sistemas) { this.sistemas = sistemas; }
    public void setViaticos(String viaticos) { this.viaticos = viaticos; }
    public void setFondo(String fondo) { this.fondo = fondo; }
    public void setArea(String area) { this.area = area; }
    public void setPuesto(String puesto) { this.puesto = puesto; }
    public void setResponsable(String responsable) { this.responsable = responsable; }
    public void setCampus(String campus) { this.campus = campus; }
}
