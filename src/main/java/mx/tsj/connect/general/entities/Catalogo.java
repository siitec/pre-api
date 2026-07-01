package mx.tsj.connect.general.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Catalogo", schema = "dbo")
public class Catalogo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "PARTIDA", length = 4)
    private String partida;

    @Column(name = "DESCRIPCION", length = 250)
    private String descripcion;

    @Column(name = "UM", length = 10)
    private String um;

    @Column(name = "MUESTRA", length = 2)
    private String muestra;

    public Integer getId() { return id; }
    public String getPartida() { return partida; }
    public String getDescripcion() { return descripcion; }
    public String getUm() { return um; }
    public String getMuestra() { return muestra; }

    public void setPartida(String partida) { this.partida = partida; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public void setUm(String um) { this.um = um; }
    public void setMuestra(String muestra) { this.muestra = muestra; }
}
