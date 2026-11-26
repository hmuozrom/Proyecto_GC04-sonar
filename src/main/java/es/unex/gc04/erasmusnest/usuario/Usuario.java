package es.unex.gc04.erasmusnest.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Usuario de ErasmusNest (TD-01, TK-002).
 * La contraseña solo se guarda como hash con sal (NFR-001), nunca en texto plano.
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Correo normalizado (sin espacios y en minúsculas). Único: evita cuentas duplicadas. */
    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoCuenta estado = EstadoCuenta.NO_VERIFICADA;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro = LocalDateTime.now(ZoneId.systemDefault());

    protected Usuario() {
        // Requerido por JPA
    }

    public Usuario(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public EstadoCuenta getEstado() { return estado; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }

    public boolean estaVerificada() {
        return estado == EstadoCuenta.VERIFICADA;
    }

    /** Lo usará US-002 al abrir un enlace de verificación válido. */
    public void verificar() {
        this.estado = EstadoCuenta.VERIFICADA;
    }
}
