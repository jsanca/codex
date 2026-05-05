module codex.index {
    requires transitive codex.codex;
    requires codex.fundamentum;
    requires org.slf4j;
    exports codex.index.api;
    exports codex.index.api.runtime;
}
