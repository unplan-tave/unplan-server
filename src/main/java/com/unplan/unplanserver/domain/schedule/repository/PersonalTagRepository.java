package com.unplan.unplanserver.domain.schedule.repository;

import com.unplan.unplanserver.domain.schedule.entity.PersonalTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PersonalTagRepository extends JpaRepository<PersonalTag, Long> {

    // 대소문자만 다른 같은 태그가 중복 생성되지 않도록 IgnoreCase 로 조회
    Optional<PersonalTag> findByMemberIdAndNameIgnoreCase(Long memberId, String name);

    List<PersonalTag> findByMemberIdOrderByName(Long memberId);
}
