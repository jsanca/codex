package codex.custos.api.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessDeniedExceptionTest {

    @Test
    @DisplayName("Should construct with a message")
    void constructWithMessage() {
        AccessDeniedException ex = new AccessDeniedException("operation not allowed");
        assertThat(ex.getMessage()).isEqualTo("operation not allowed");
        assertThat(ex).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("Should construct with a message and cause")
    void constructWithMessageAndCause() {
        Throwable cause = new IllegalStateException("upstream failure");
        AccessDeniedException ex = new AccessDeniedException("operation not allowed", cause);
        assertThat(ex.getMessage()).isEqualTo("operation not allowed");
        assertThat(ex.getCause()).isSameAs(cause);
    }
}
