package mx.tsj.connect.general.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "PartidasRol", schema = "dbo")
public class PartidaRol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_Rol")
    private Integer idRol;

    @Column(name = "id_Partida")
    private Integer idPartida;

    public Integer getId() { return id; }
    public Integer getIdRol() { return idRol; }
    public Integer getIdPartida() { return idPartida; }
}
