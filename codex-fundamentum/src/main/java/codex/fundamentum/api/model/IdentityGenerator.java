package codex.fundamentum.api.model;


/**
 * The IdentityGenerator interface is responsible for generating identities of type I,
 * associated with an entity of type S.
 */
public interface IdentityGenerator<S, I> {

    I nextIdentity(S source);
}
