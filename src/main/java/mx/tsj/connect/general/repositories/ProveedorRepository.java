package mx.tsj.connect.general.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.tsj.connect.general.entities.Proveedor;

public interface ProveedorRepository extends JpaRepository<Proveedor, String> {
}
