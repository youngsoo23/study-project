package com.young.studyproject.post.infrastructure;

import static com.young.studyproject.post.infrastructure.QPostJpaEntity.postJpaEntity;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PostQuerydslRepositoryImpl implements PostQuerydslRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<PostJpaEntity> search(String keyword, Long userId) {
        return queryFactory
                .selectFrom(postJpaEntity)
                .where(keywordContains(keyword), userIdEq(userId))
                .fetch();
    }

    private BooleanExpression keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return postJpaEntity.title.containsIgnoreCase(keyword)
                .or(postJpaEntity.content.containsIgnoreCase(keyword));
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId == null ? null : postJpaEntity.userId.eq(userId);
    }
}
