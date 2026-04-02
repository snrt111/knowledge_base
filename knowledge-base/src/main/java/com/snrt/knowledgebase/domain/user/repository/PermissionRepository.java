package com.snrt.knowledgebase.domain.user.repository;

import com.snrt.knowledgebase.domain.user.entity.Permission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {

    Optional<Permission> findByCode(String code);

    boolean existsByCode(String code);

    List<Permission> findByTypeAndIsActiveTrueAndIsDeletedFalseOrderBySortAsc(String type);

    List<Permission> findByParentIdAndIsActiveTrueAndIsDeletedFalseOrderBySortAsc(String parentId);

    List<Permission> findByIsActiveTrueAndIsDeletedFalseOrderBySortAsc();

    @Query("SELECT p FROM Permission p WHERE p.isDeleted = false " +
           "AND (:name IS NULL OR p.name LIKE %:name%) " +
           "AND (:code IS NULL OR p.code LIKE %:code%) " +
           "AND (:type IS NULL OR p.type = :type)")
    Page<Permission> findByConditions(@Param("name") String name, 
                                        @Param("code") String code, 
                                        @Param("type") String type, 
                                        Pageable pageable);

    @Query("SELECT p FROM Permission p " +
           "INNER JOIN RolePermission rp ON p.id = rp.permissionId " +
           "WHERE rp.roleId = :roleId AND p.isActive = true AND p.isDeleted = false")
    List<Permission> findByRoleId(@Param("roleId") String roleId);

    @Query("SELECT DISTINCT p FROM Permission p " +
           "INNER JOIN RolePermission rp ON p.id = rp.permissionId " +
           "INNER JOIN UserRole ur ON rp.roleId = ur.roleId " +
           "WHERE ur.userId = :userId AND p.isActive = true AND p.isDeleted = false")
    List<Permission> findByUserId(@Param("userId") String userId);
}
