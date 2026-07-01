package mx.tsj.connect.general.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Proveedores", schema = "dbo")
public class Proveedor {
    @Id
    @Column(name = "Codigo", length = 50)
    private String codigo;

    @Column(name = "Nombre", length = 200)
    private String nombre;

    @Column(name = "RFC", length = 13)
    private String rfc;

    @Column(name = "Calle", length = 100)
    private String calle;

    @Column(name = "NumExt", length = 10)
    private String numExt;

    @Column(name = "NumInt", length = 10)
    private String numInt;

    @Column(name = "Colonia", length = 50)
    private String colonia;

    @Column(name = "CodigoP", length = 10)
    private String codigoP;

    @Column(name = "Telefono1", length = 15)
    private String telefono1;

    @Column(name = "Telefono2", length = 15)
    private String telefono2;

    @Column(name = "Telefono3", length = 15)
    private String telefono3;

    @Column(name = "Telefono4", length = 15)
    private String telefono4;

    @Column(name = "Email", length = 100)
    private String email;

    @Column(name = "Municipio", length = 50)
    private String municipio;

    @Column(name = "Ciudad", length = 50)
    private String ciudad;

    @Column(name = "Estado", length = 50)
    private String estado;

    @Column(name = "Pais", length = 55)
    private String pais;

    @Column(name = "Representante", length = 100)
    private String representante;

    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getRfc() { return rfc; }
    public String getCalle() { return calle; }
    public String getNumExt() { return numExt; }
    public String getNumInt() { return numInt; }
    public String getColonia() { return colonia; }
    public String getCodigoP() { return codigoP; }
    public String getTelefono1() { return telefono1; }
    public String getTelefono2() { return telefono2; }
    public String getTelefono3() { return telefono3; }
    public String getTelefono4() { return telefono4; }
    public String getEmail() { return email; }
    public String getMunicipio() { return municipio; }
    public String getCiudad() { return ciudad; }
    public String getEstado() { return estado; }
    public String getPais() { return pais; }
    public String getRepresentante() { return representante; }

    public void setCodigo(String codigo) { this.codigo = codigo; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setRfc(String rfc) { this.rfc = rfc; }
    public void setCalle(String calle) { this.calle = calle; }
    public void setNumExt(String numExt) { this.numExt = numExt; }
    public void setNumInt(String numInt) { this.numInt = numInt; }
    public void setColonia(String colonia) { this.colonia = colonia; }
    public void setCodigoP(String codigoP) { this.codigoP = codigoP; }
    public void setTelefono1(String telefono1) { this.telefono1 = telefono1; }
    public void setTelefono2(String telefono2) { this.telefono2 = telefono2; }
    public void setTelefono3(String telefono3) { this.telefono3 = telefono3; }
    public void setTelefono4(String telefono4) { this.telefono4 = telefono4; }
    public void setEmail(String email) { this.email = email; }
    public void setMunicipio(String municipio) { this.municipio = municipio; }
    public void setCiudad(String ciudad) { this.ciudad = ciudad; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setPais(String pais) { this.pais = pais; }
    public void setRepresentante(String representante) { this.representante = representante; }
}
