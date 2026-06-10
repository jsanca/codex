package codex.custos.api.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityEvaluationContextTest {

    @Test
    @DisplayName("empty() returns a non-null SecurityEvaluationContext")
    void emptyIsNonNull() {
        assertThat(SecurityEvaluationContext.empty()).isNotNull();
    }

    @Test
    @DisplayName("empty() returns the same singleton instance on repeated calls")
    void emptyIsSingleton() {
        SecurityEvaluationContext first = SecurityEvaluationContext.empty();
        SecurityEvaluationContext second = SecurityEvaluationContext.empty();
        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("empty() implements SecurityEvaluationContext")
    void emptyImplementsInterface() {
        assertThat(SecurityEvaluationContext.empty()).isInstanceOf(SecurityEvaluationContext.class);
    }
}
