module codex.custos {
    requires transitive codex.fundamentum;
    requires transitive codex.codex;
    exports codex.custos.api.exception;
    exports codex.custos.api.model;
    exports codex.custos.api.service;
}
