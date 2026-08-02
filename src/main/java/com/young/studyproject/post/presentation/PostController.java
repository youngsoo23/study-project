package com.young.studyproject.post.presentation;

import com.young.studyproject.post.application.PostService;
import com.young.studyproject.post.application.dto.PostCreateCommand;
import com.young.studyproject.post.application.dto.PostResult;
import com.young.studyproject.post.application.dto.PostUpdateCommand;
import com.young.studyproject.post.presentation.dto.PostCreateRequest;
import com.young.studyproject.post.presentation.dto.PostResponse;
import com.young.studyproject.post.presentation.dto.PostUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostResponse> create(@Valid @RequestBody PostCreateRequest request) {
        PostResult result = postService.create(
                new PostCreateCommand(request.title(), request.content(), request.userId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PostResponse.from(result));
    }

    @GetMapping("/{id}")
    public PostResponse getById(@PathVariable Long id) {
        return PostResponse.from(postService.getById(id));
    }

    @GetMapping
    public List<PostResponse> getAll(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId
    ) {
        List<PostResult> results = (keyword == null || keyword.isBlank()) && userId == null
                ? postService.getAll()
                : postService.search(keyword, userId);
        return results.stream().map(PostResponse::from).toList();
    }

    @PutMapping("/{id}")
    public PostResponse update(@PathVariable Long id, @Valid @RequestBody PostUpdateRequest request) {
        PostResult result = postService.update(id, new PostUpdateCommand(request.title(), request.content()));
        return PostResponse.from(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
