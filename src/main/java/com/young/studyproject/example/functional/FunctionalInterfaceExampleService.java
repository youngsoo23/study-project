package com.young.studyproject.example.functional;

import com.young.studyproject.common.exception.EntityNotFoundException;
import com.young.studyproject.post.domain.Post;
import com.young.studyproject.post.domain.PostRepository;
import com.young.studyproject.user.domain.User;
import com.young.studyproject.user.domain.UserRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * study-doc/interview_debrief.md 2번 항목(스트림 안에서 쓰는 함수형 인터페이스) 학습용 예제.
 * 실제 User/Post 데이터를 각 함수형 인터페이스로 다뤄본다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FunctionalInterfaceExampleService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    // Function<T, R>: T를 받아 R을 반환. stream().map()의 인자 타입.
    public List<String> extractAllUserNames() {
        Function<User, String> toName = User::getName;

        return userRepository.findAll().stream()
                .map(toName)
                .toList();
    }

    // Predicate<T>: T를 받아 boolean 반환. stream().filter()의 인자 타입.
    // and()/or()/negate()로 여러 조건을 조합할 수 있다.
    public List<User> findUsersByNameAndEmailDomain(String nameKeyword, String emailDomain) {
        Predicate<User> nameMatches = user -> user.getName().contains(nameKeyword);
        Predicate<User> emailMatches = user -> user.getEmail().endsWith(emailDomain);

        return userRepository.findAll().stream()
                .filter(nameMatches.and(emailMatches))
                .toList();
    }

    // Consumer<T>: T를 받아 값 없이 부수 효과만 수행. stream().forEach()의 인자 타입.
    public List<String> collectUserSummaries() {
        List<String> summaries = new ArrayList<>();
        Consumer<User> appendSummary = user -> summaries.add(user.getId() + ":" + user.getName());

        userRepository.findAll().forEach(appendSummary);
        return summaries;
    }

    // Supplier<T>: 인자 없이 값을 생성. Optional.orElseGet() 같은 지연 기본값 생성에 쓰인다.
    public User getFirstUserOrGuest() {
        Supplier<User> guestSupplier = () -> User.builder()
                .name("guest")
                .email("guest@example.com")
                .build();

        return userRepository.findAll().stream()
                .findFirst()
                .orElseGet(guestSupplier);
    }

    // BiFunction<T, U, R>: 두 인자를 받아 R을 반환.
    public String combineUserAndPost(Long userId, Long postId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + userId));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. id=" + postId));

        BiFunction<User, Post, String> combine =
                (u, p) -> u.getName() + "님이 작성한 글: " + p.getTitle();

        return combine.apply(user, post);
    }

    // UnaryOperator<T>: Function<T, T>의 특수화. 입력과 출력 타입이 같다.
    public String normalizeUserName(String rawName) {
        UnaryOperator<String> trimAndCapitalize = name -> {
            String trimmed = name.trim();
            return trimmed.isEmpty()
                    ? trimmed
                    : Character.toUpperCase(trimmed.charAt(0)) + trimmed.substring(1);
        };

        return trimAndCapitalize.apply(rawName);
    }

    // BinaryOperator<T>: BiFunction<T, T, T>의 특수화. stream().reduce()의 인자 타입이며,
    // Comparator와 결합한 maxBy/minBy로 둘 중 하나를 고르는 데 자주 쓰인다.
    public Post pickMoreRecentlyUpdatedPost(Long postId1, Long postId2) {
        Post post1 = postRepository.findById(postId1)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. id=" + postId1));
        Post post2 = postRepository.findById(postId2)
                .orElseThrow(() -> new EntityNotFoundException("게시글을 찾을 수 없습니다. id=" + postId2));

        BinaryOperator<Post> pickLatest = BinaryOperator.maxBy(Comparator.comparing(Post::getUpdatedAt));

        return pickLatest.apply(post1, post2);
    }
}
