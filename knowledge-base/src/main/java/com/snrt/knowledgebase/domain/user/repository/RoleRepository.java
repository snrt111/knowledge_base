package com.snrt.knowledgebase.domain.user.repository;

import com.snrt.knowledgebase.domain.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByCode(String code);

    Optional<Role> findByName(String name);

    boolean existsByCode(String code);

    boolean existsByName(String name);

    List<Role> findByIsActiveTrueAndIsDeletedFalseOrderBySortAsc();

    @Query("SELECT r FROM Role r WHERE r.isDeleted = false " +
           "AND (:name IS NULL OR r.name LIKE %:name%) " +
           "AND (:code IS NULL OR r.code LIKE %:code%)")
    Page<Role> findByConditions(@Param("name") String name, 
                                  @Param("code") String code, 
                                  Pageable pageable);
}
