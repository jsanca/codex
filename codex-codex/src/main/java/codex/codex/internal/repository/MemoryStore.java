package codex.codex.internal.repository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.Map;

/**
 * Small reusable in-memory storage component for repository implementations.
 *
 * @param <K> key type
 * @param <E> entity type
 */
final class MemoryStore<K, E> {

    private final Map<K, E> store = new ConcurrentHashMap<>();
    private final Function<E, K> keyResolver;

    MemoryStore(final Function<E, K> keyResolver) {
        this.keyResolver = Objects.requireNonNull(keyResolver, "keyResolver must not be null");
    }

    /**
     * Saves the given entity into the store, replacing any existing entity with the same key.
     *
     * @param entity the entity to save; must not be null
     * @return the saved entity
     */
    E save(final E entity) {
        Objects.requireNonNull(entity, "entity must not be null");

        final K key = Objects.requireNonNull(keyResolver.apply(entity), "entity key must not be null");
        store.put(key, entity);
        return entity;
    }

    /**
     * Finds an entity by its exact key.
     *
     * @param key the key to look up; must not be null
     * @return an Optional containing the found entity, or empty if not found
     */
    Optional<E> findByKey(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(store.get(key));
    }

    /**
     * Checks if an entity with the given key exists in the store.
     *
     * @param key the key to check; must not be null
     * @return true if the entity exists, false otherwise
     */
    boolean existsByKey(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        return store.containsKey(key);
    }

    /**
     * Removes an entity by its key.
     *
     * @param key the key of the entity to remove; must not be null
     * @return true if an entity was removed, false if no such entity existed
     */
    boolean deleteByKey(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        return store.remove(key) != null;
    }

    /**
     * Retrieves all entities in the store as an unmodifiable list.
     * <p>
     * <strong>Warning:</strong> The returned list makes NO ordering guarantees.
     * Because the underlying store is a {@link java.util.concurrent.ConcurrentHashMap},
     * iteration order is completely non-deterministic.
     *
     * @return an unmodifiable list of all stored entities
     */
    List<E> findAll() {
        return List.copyOf(store.values());
    }

    /**
     * Finds all entities matching the given predicate.
     *
     * @param predicate the condition to match; must not be null
     * @return an unmodifiable list of matching entities
     */
    List<E> findWhere(final java.util.function.Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return store.values().stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Finds the first entity matching the given predicate.
     * <p>
     * Note that since the underlying store is unordered, if multiple entities
     * match the predicate, which one is returned is non-deterministic.
     *
     * @param predicate the condition to match; must not be null
     * @return an Optional containing the first matched entity, or empty if none match
     */
    Optional<E> findFirstWhere(final java.util.function.Predicate<E> predicate) {
        Objects.requireNonNull(predicate, "predicate must not be null");
        return store.values().stream()
                .filter(predicate)
                .findFirst();
    }
}