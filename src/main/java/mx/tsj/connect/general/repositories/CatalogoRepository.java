package mx.tsj.connect.general.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.tsj.connect.general.entities.Catalogo;

public interface CatalogoRepository extends JpaRepository<Catalogo, Integer> {
}
