package es.unex.gc04.erasmusnest.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Modelo de usuario (TK-002). Solo cuentas sintéticas (NFR-009). */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository repositorio;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    void unaCuentaNuevaNaceNoVerificada() {
        Usuario guardado = repositorio.save(new Usuario("ana.garcia@alumnos.unex.es", encoder.encode("Erasmus2026")));

        assertThat(guardado.getEstado()).isEqualTo(EstadoCuenta.NO_VERIFICADA);
        assertThat(guardado.estaVerificada()).isFalse();
    }

    @Test
    void laContrasenaSeGuardaConHashYConSal() {
        String hash1 = encoder.encode("Erasmus2026");
        String hash2 = encoder.encode("Erasmus2026");

        assertThat(hash1)
                .isNotEqualTo("Erasmus2026")
                .isNotEqualTo(hash2); // la sal hace que cada hash sea distinto
        assertThat(encoder.matches("Erasmus2026", hash1)).isTrue();
    }

    @Test
    void elCorreoEsUnico() {
        repositorio.saveAndFlush(new Usuario("luis.perez@alumnos.unex.es", encoder.encode("Clave1234")));

        Usuario duplicado = new Usuario("luis.perez@alumnos.unex.es", encoder.encode("Otra12345"));

        assertThatThrownBy(() -> repositorio.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
