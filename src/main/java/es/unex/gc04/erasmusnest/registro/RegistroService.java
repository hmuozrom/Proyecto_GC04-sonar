package es.unex.gc04.erasmusnest.registro;

import es.unex.gc04.erasmusnest.usuario.Usuario;
import es.unex.gc04.erasmusnest.usuario.UsuarioRepository;
import es.unex.gc04.erasmusnest.verificacion.VerificacionService;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Validación en servidor del registro (TK-003, NFR-007): formato del correo, dominio
 * institucional configurable (RN-01), normalización, contraseña y duplicados.
 */
@Service
public class RegistroService {

    public static final String MENSAJE_GENERICO = "Si el correo es válido, recibirás un enlace";
    public static final String ERROR_CORREO_MAL_FORMADO = "Introduce un correo válido";
    public static final String ERROR_DOMINIO = "Solo se admiten correos institucionales de la UEx";
    public static final String ERROR_NO_COINCIDEN = "Las contraseñas no coinciden";

    /**
     * Un solo @, sin espacios, con algo antes y con un punto en el dominio.
     * Cuantificadores posesivos y etiquetas del dominio sin puntos: sin backtracking (java:S8786).
     */
    private static final Pattern FORMATO_CORREO =
            Pattern.compile("^[^@\\s]++@[^@\\s.]++(?:\\.[^@\\s.]++)++$");

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final String dominio;
    private final VerificacionService verificacion;

    public RegistroService(UsuarioRepository usuarios,
                           PasswordEncoder encoder,
                           @Value("${app.registro.dominio-institucional}") String dominio,
                           VerificacionService verificacion) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.dominio = dominio.trim().toLowerCase(Locale.ROOT);
        this.verificacion = verificacion;
    }

    public ResultadoRegistro registrar(RegistroForm form) {
        Map<String, String> errores = new LinkedHashMap<>();

        String email = normalizar(form.getEmail());
        if (!FORMATO_CORREO.matcher(email).matches()) {
            errores.put("email", ERROR_CORREO_MAL_FORMADO);
        } else if (!email.endsWith("@" + dominio)) {
            // Termina exactamente en el dominio: rechaza @alumnos.unex.es.fake.com
            errores.put("email", ERROR_DOMINIO);
        }

        String password = form.getPassword() == null ? "" : form.getPassword();
        String faltaEnPassword = queFaltaEnLaContrasena(password);
        if (faltaEnPassword != null) {
            errores.put("password", faltaEnPassword);
        }

        if (!password.equals(form.getConfirmacion())) {
            errores.put("confirmacion", ERROR_NO_COINCIDEN);
        }

        if (!errores.isEmpty()) {
            return ResultadoRegistro.rechazado(errores);
        }

        if (usuarios.existsByEmail(email)) {
            return ResultadoRegistro.aceptado(); // no revela que la cuenta existe (AC-03)
        }

        try {
            Usuario nuevo = usuarios.saveAndFlush(new Usuario(email, encoder.encode(password)));
            // US-001-AC-04: un registro válido dispara la verificación (US-002, TK-006)
            verificacion.emitirEnlace(nuevo);
        } catch (DataIntegrityViolationException dobleEnvio) {
            // Doble clic: la restricción única impide la segunda cuenta.
        }
        return ResultadoRegistro.aceptado();
    }

    /** Quita los espacios de los extremos y pasa a minúsculas (casos límite de US-001). */
    static String normalizar(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** Devuelve qué le falta a la contraseña (US-001-AC-05), o null si es válida. */
    static String queFaltaEnLaContrasena(String password) {
        java.util.List<String> falta = new java.util.ArrayList<>();
        if (password.length() < 8) {
            falta.add("al menos 8 caracteres");
        }
        if (password.chars().noneMatch(Character::isLetter)) {
            falta.add("una letra");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            falta.add("un número");
        }
        if (falta.isEmpty()) {
            return null;
        }
        String lista = falta.size() == 1
                ? falta.get(0)
                : String.join(", ", falta.subList(0, falta.size() - 1)) + " y " + falta.get(falta.size() - 1);
        return "La contraseña necesita " + lista;
    }
}
