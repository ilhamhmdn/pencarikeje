package com.kejelah.pencarikeje.status;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StatusRepository extends JpaRepository<Status, Long> {

    List<Status> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<Status> findByCode(String code);

    Optional<Status> findByIdAndActiveTrue(Long id);
}
