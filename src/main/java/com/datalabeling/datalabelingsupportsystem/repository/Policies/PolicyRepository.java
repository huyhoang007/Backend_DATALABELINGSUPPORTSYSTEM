package com.datalabeling.datalabelingsupportsystem.repository.Policies;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyRepository extends JpaRepository<com.datalabeling.datalabelingsupportsystem.pojo.Policy, Long> {

    Optional<com.datalabeling.datalabelingsupportsystem.pojo.Policy> findByErrorName(String errorName);
}
