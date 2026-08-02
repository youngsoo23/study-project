package com.young.studyproject.post.infrastructure;

import com.young.studyproject.post.domain.Post;
import com.young.studyproject.post.domain.PostRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostRepositoryImpl implements PostRepository {

    private final PostJpaRepository postJpaRepository;

    @Override
    public Post save(Post post) {
        return toDomain(postJpaRepository.save(toEntity(post)));
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postJpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Post> findAll() {
        return postJpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public List<Post> search(String keyword, Long userId) {
        return postJpaRepository.search(keyword, userId).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        postJpaRepository.deleteById(id);
    }

    private PostJpaEntity toEntity(Post post) {
        return PostJpaEntity.builder()
                .id(post.getId())
                .title(post.getTitle())
                .content(post.getContent())
                .userId(post.getUserId())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private Post toDomain(PostJpaEntity entity) {
        return Post.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .userId(entity.getUserId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
