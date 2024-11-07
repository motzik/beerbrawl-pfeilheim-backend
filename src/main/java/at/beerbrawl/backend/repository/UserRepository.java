/* SPDX-License-Identifier: AGPL-3.0-or-later */

package at.beerbrawl.backend.repository;

import at.beerbrawl.backend.entity.ApplicationUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<ApplicationUser, Long> {
    ApplicationUser findByUsername(String username);

    boolean existsByUsername(String username);

    void deleteByUsername(String username);
}
