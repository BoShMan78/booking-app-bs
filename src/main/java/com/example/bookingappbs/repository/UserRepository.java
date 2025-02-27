package com.example.bookingappbs.repository;

import com.example.bookingappbs.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);

    Optional<User> findUserByEmail(String email);

    //    @Modifying
    //    @Query(value = "INSERT INTO users (email, first_name, last_name, password, role) "
    //            + "VALUES (:email, :firstName, :lastName, :password, "
    //            + "CAST(:role AS user_role)) RETURNING *",
    //            nativeQuery = true)
    //    User saveUserWithRole(@Param("email") String email,
    //                          @Param("firstName") String firstName,
    //                          @Param("lastName") String lastName,
    //                          @Param("password") String password,
    //                          @Param("role") String role);

    //    @Transactional
    //    @Modifying
    //    @Query(value = "INSERT INTO users (
    //    email, first_name, last_name, password, role, is_deleted) "
    //            + "VALUES (:email, :firstName, :lastName, :password, "
    //            + "CAST(:role AS user_role), :isDeleted) RETURNING *", nativeQuery = true)
    //    User saveUser(@Param("email") String email,
    //                  @Param("firstName") String firstName,
    //                  @Param("lastName") String lastName,
    //                  @Param("password") String password,
    //                  @Param("role") String role,
    //                  @Param("isDeleted") boolean isDeleted);
}
