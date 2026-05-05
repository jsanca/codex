module codex.fundamentum {
    requires com.github.benmanes.caffeine;
    requires org.slf4j;
    exports codex.fundamentum.api;
    exports codex.fundamentum.api.cache;
    exports codex.fundamentum.api.concurrent;
    exports codex.fundamentum.api.event;
    exports codex.fundamentum.api.exception;
    exports codex.fundamentum.api.lifecycle;
    exports codex.fundamentum.api.model;
    exports codex.fundamentum.api.runtime;
    exports codex.fundamentum.api.tx;
}
