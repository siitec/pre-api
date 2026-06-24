package mx.tsj.connect.general.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Usuarios", schema = "dbo")
public class Usuario {
    @Id
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

    @Column(name = "Area", length = 100)
    private String area;

    @Column(name = "Puesto", length = 100)
    private String puesto;

    @Column(name = "Responsable", length = 200)
    private String responsable;

    public Integer getId() { return id; }
    public String getStatus() { return status; }
    public String getUsuario() { return usuario; }
    public String getNombre() { return nombre; }
    public String getPassword() { return password; }
    public String getUa() { return ua; }
    public String getPermisos() { return permisos; }
    public String getArea() { return area; }
    public String getPuesto() { return puesto; }
    public String getResponsable() { return responsable; }

    public void setPassword(String password) { this.password = password; }
    public void setPuesto(String puesto) { this.puesto = puesto; }
}
