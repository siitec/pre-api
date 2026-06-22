package mx.tsj.connect.general.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import mx.tsj.connect.general.entities.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findFirstByUsuarioIgnoreCase(String usuario);
}
