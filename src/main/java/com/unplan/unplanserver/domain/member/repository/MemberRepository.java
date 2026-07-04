package com.unplan.unplanserver.domain.member.repository;

import com.unplan.unplanserver.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByOauthId(String oauthId);

    Optional<Member> findByEmail(String email);

    @Modifying
    @Query(value = "DELETE FROM member WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    void deleteWithdrawnMembers(@Param("threshold")LocalDateTime threshold);

    @Query(value = "SELECT * FROM member WHERE deleted_at IS NOT NULL AND deleted_at < :threshold", nativeQuery = true)
    List<Member> findByDeletedAtBefore(@Param("threshold")LocalDateTime threshold);
}
