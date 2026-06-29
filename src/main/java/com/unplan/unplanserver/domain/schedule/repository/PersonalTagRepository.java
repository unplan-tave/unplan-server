package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.PersonalTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalTagRepository extends JpaRepository<PersonalTag, Long> {

    Optional<PersonalTag> findByMemberIdAndName(Long memberId, String name);

    List<PersonalTag> findByMemberIdOrderByName(Long memberId);
}
