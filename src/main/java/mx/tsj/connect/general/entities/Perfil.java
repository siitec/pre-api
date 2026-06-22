package mx.tsj.connect.general.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Perfiles", schema = "dbo")
public class Perfil {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "id_Rol")
    private Integer idRol;

    @Column(name = "id_Usuario")
    private Integer idUsuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "alcance", length = 100)
    private AlcancePerfil alcance;

    public Integer getId() { return id; }
    public Integer getIdRol() { return idRol; }
    public Integer getIdUsuario() { return idUsuario; }
    public AlcancePerfil getAlcance() { return alcance; }
}
