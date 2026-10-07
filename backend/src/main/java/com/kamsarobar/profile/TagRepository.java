package com.kamsarobar.profile;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface TagRepository<T extends NamedTag> extends JpaRepository<T, Long> {

    List<T> findByNormalizedNameIn(Collection<String> normalizedNames);

    List<T> findByNormalizedNameContainingOrderByNameAsc(String fragment, Pageable pageable);
}
