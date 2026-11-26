package es.unex.gc04.erasmusnest.registro;

import static org.assertj.core.api.Assertions.assertThat;

import es.unex.gc04.erasmusnest.usuario.UsuarioRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

/** Formulario de registro (TK-004). Solo cuentas sintéticas (NFR-009). */
@SpringBootTest
class RegistroControllerTest {

    @Autowired
    private RegistroController controller;

    @Autowired
    private UsuarioRepository usuarios;

    @BeforeEach
    void limpiar() {
        usuarios.deleteAll();
    }

    @Test
    void elFormularioSeMuestraVacio() {
        ExtendedModelMap model = new ExtendedModelMap();

        assertThat(controller.mostrarFormulario(model)).isEqualTo(RegistroController.VISTA_FORMULARIO);
        assertThat(model.get("form")).isInstanceOf(RegistroForm.class);
    }

    @Test
    void registroValidoRedirigeConElMensajeGenerico() {
        RedirectAttributesModelMap redirect = new RedirectAttributesModelMap();

        String vista = controller.registrar(
                new RegistroForm("ana.garcia@alumnos.unex.es", "Erasmus2026", "Erasmus2026"),
                new ExtendedModelMap(), redirect);

        assertThat(vista).isEqualTo("redirect:/registro/enviado");
        assertThat(Map.<String, Object>copyOf(redirect.getFlashAttributes()))
                .containsEntry("mensaje", RegistroService.MENSAJE_GENERICO);
    }

    @Test
    @SuppressWarnings("unchecked")
    void conErroresVuelveAlFormularioConElErrorEnSuCampoYSinContrasenas() {
        RegistroForm form = new RegistroForm("ana.garcia@alumnos.unex.es", "Erasmus2026", "Erasmus2025");
        ExtendedModelMap model = new ExtendedModelMap();

        String vista = controller.registrar(form, model, new RedirectAttributesModelMap());

        assertThat(vista).isEqualTo(RegistroController.VISTA_FORMULARIO);
        assertThat((Map<String, String>) model.get("errores"))
                .containsEntry("confirmacion", RegistroService.ERROR_NO_COINCIDEN);
        assertThat(form.getEmail()).isEqualTo("ana.garcia@alumnos.unex.es");
        assertThat(form.getPassword()).isNull();
        assertThat(form.getConfirmacion()).isNull();
        assertThat(usuarios.count()).isZero();
    }
}
