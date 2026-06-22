package mx.tsj.connect.general.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "Partidas", schema = "dbo")
public class Partida {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "clave", length = 4)
    private String clave;

    @Column(name = "nombre", length = 200)
    private String nombre;

    public Integer getId() { return id; }
    public String getClave() { return clave; }
    public String getNombre() { return nombre; }
}
