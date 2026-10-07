package com.kamsarobar.profile;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.springframework.data.domain.PageRequest;

import com.kamsarobar.common.util.TextNormalizer;

/**
 * Turns user-typed labels into shared tag entities, creating the ones that do not exist yet.
 * One generic implementation serves companies, skills and any future tag type.
 */
public class TagResolver<T extends NamedTag> {

    private static final int SUGGESTION_LIMIT = 10;

    private final TagRepository<T> repository;
    private final Function<String, T> factory;

    public TagResolver(TagRepository<T> repository, Function<String, T> factory) {
        this.repository = repository;
        this.factory = factory;
    }

    public Set<T> resolve(Collection<String> rawNames) {
        if (rawNames == null || rawNames.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Map<String, String> byKey = new LinkedHashMap<>();
        rawNames.stream().filter(Objects::nonNull).forEach(raw -> {
            String key = TextNormalizer.key(raw);
            if (key != null) {
                byKey.putIfAbsent(key, TextNormalizer.clean(raw));
            }
        });
        Map<String, T> existing = new LinkedHashMap<>();
        repository.findByNormalizedNameIn(byKey.keySet()).forEach(t -> existing.put(t.getNormalizedName(), t));

        Set<T> result = new LinkedHashSet<>();
        byKey.forEach((key, display) -> result.add(
                existing.computeIfAbsent(key, k -> repository.save(factory.apply(display)))));
        return result;
    }

    public List<String> suggest(String query) {
        String key = TextNormalizer.key(query);
        if (key == null) {
            return List.of();
        }
        return repository.findByNormalizedNameContainingOrderByNameAsc(key, PageRequest.of(0, SUGGESTION_LIMIT))
                .stream().map(NamedTag::getName).toList();
    }
}
