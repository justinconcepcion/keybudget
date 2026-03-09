package com.keybudget.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Data access for {@link Category} entities. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Returns all categories owned by the given user plus all system-default categories
     * (where userId is null).
     *
     * @param userId the authenticated user's id
     * @return combined list of user-specific and system categories
     */
    List<Category> findByUserIdOrUserIdIsNull(Long userId);

    /** Checks whether any system-default category already exists (used to guard seeding). */
    boolean existsByUserIdIsNull();
}
