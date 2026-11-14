
package com.example.clothingshop.repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.clothingshop.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIsActiveTrueAndRole_RoleName(String email, String roleName);

    @Modifying
    @Query("""
        update User u set u.passwordHash = :newHash
        where u.userId = :userId and u.isActive = true and u.passwordHash = :oldHash
        """)
    int replacePassword(@Param("userId") Integer userId, @Param("oldHash") String oldHash,
                        @Param("newHash") String newHash);

    @Query("""
    select u from User u join u.role r
    where (:keyword is null or lower(u.fullName) like lower(concat('%', :keyword, '%'))
           or lower(u.email) like lower(concat('%', :keyword, '%')))
      and (:roleName is null or r.roleName = :roleName)
    """)
    Page<User> searchAccounts(
            @Param("keyword") String keyword,
            @Param("roleName") String roleName,
            Pageable pageable);

    long countByRole_RoleName(String roleName);

    long countByIsActiveTrue();
}
