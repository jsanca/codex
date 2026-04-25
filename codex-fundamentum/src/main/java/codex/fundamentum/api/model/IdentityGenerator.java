package codex.fundamentum.api.model;

public interface IdentityGenerator<S, I> {

    I nextIdentity(S source);
}
