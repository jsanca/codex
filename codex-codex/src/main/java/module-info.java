module codex.codex {
    requires transitive codex.fundamentum;
    requires org.slf4j;
    exports codex.codex.api;
    exports codex.codex.api.model.identity;
    exports codex.codex.api.model.value;
    exports codex.codex.api.model.entity;
}
