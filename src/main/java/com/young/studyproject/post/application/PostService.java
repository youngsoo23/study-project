package com.young.studyproject.post.application;

import com.young.studyproject.common.exception.EntityNotFoundException;
import com.young.studyproject.post.application.dto.PostCreateCommand;
import com.young.studyproject.post.application.dto.PostResult;
import com.young.studyproject.post.application.dto.PostUpdateCommand;
import com.young.studyproject.post.domain.Post;
import com.young.studyproject.post.domain.PostRepository;
import com.young.studyproject.user.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostResult create(PostCreateCommand command) {
        userRepository.findById(command.userId())
                .orElseThrow(() -> new EntityNotFoundException("작성자를 찾을 수 없습니다. userId=" + command.userId()));

        Post post = Post.builder()
                .title(command.title())
                .content(command.content())
                .userId(command.userId())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return PostResult.from(postRepository.save(post));
    }

    public PostResult getById(Long id) {
        return PostResult.from(findPost(id));
    }

    public List<PostResult> getAll() {
        return postRepository.findAll().stream()
                .map(PostResult::from)
                .toList();
    }

    public List<PostResult> search(String keyword, Long userId) {
        return postRepository.search(keyword, userId).stream()
                .map(PostResult::from)
                .toList();
    }

    @Transactional
    public PostResult update(Long id, PostUpdateCommand command) {
        Post post = findPost(id);
        Post updated = post.update(command.title(), command.content());
        return PostResult.from(postRepository.save(updated));
    }

    @Transactional
    public void delete(Long id) {
        findPost(id);
        postRepository.deleteById(id);
    }

    private Post findPost(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. id=" + id));
    }
}
