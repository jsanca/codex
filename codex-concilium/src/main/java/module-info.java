/**
 * Codex Concilium — the local runtime composition module.
 *
 * <p>Composes module runtimes ({@code CodexRuntime}, {@code IndexRuntime},
 * {@code ChroniconRuntime}, and future runtimes) into a coherent local application runtime.</p>
 */
module codex.concilium {
    requires codex.fundamentum;
    requires codex.codex;
    requires codex.index;
    requires codex.chronicon;
    requires org.slf4j;
}
