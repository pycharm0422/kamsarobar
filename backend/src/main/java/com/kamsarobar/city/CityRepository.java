package com.kamsarobar.city;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findAllByOrderByNameAsc();

    List<City> findAllByActiveTrueOrderByNameAsc();

    Optional<City> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
