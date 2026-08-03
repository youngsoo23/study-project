package com.young.studyproject.example.functional;

import com.young.studyproject.example.functional.dto.CombineResponse;
import com.young.studyproject.example.functional.dto.NormalizeNameResponse;
import com.young.studyproject.example.functional.dto.PostSummaryResponse;
import com.young.studyproject.example.functional.dto.UserSummaryResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 각 엔드포인트는 java.util.function의 함수형 인터페이스 하나씩을 직접 사용해본다.
 */
@RestController
@RequestMapping("/api/examples/functional-interfaces")
@RequiredArgsConstructor
public class FunctionalInterfaceExampleController {

    private final FunctionalInterfaceExampleService exampleService;

    @GetMapping("/user-names") // Function<User, String>
    public List<String> userNames() {
        return exampleService.extractAllUserNames();
    }

    @GetMapping("/users/search") // Predicate<User> 조합 (and)
    public List<UserSummaryResponse> searchUsers(
            @RequestParam(defaultValue = "") String nameKeyword,
            @RequestParam(defaultValue = "") String emailDomain
    ) {
        return exampleService.findUsersByNameAndEmailDomain(nameKeyword, emailDomain).stream()
                .map(UserSummaryResponse::from)
                .toList();
    }

    @GetMapping("/users/summaries") // Consumer<User>
    public List<String> userSummaries() {
        return exampleService.collectUserSummaries();
    }

    @GetMapping("/users/first-or-guest") // Supplier<User>
    public UserSummaryResponse firstUserOrGuest() {
        return UserSummaryResponse.from(exampleService.getFirstUserOrGuest());
    }

    @GetMapping("/combine") // BiFunction<User, Post, String>
    public CombineResponse combine(@RequestParam Long userId, @RequestParam Long postId) {
        return new CombineResponse(exampleService.combineUserAndPost(userId, postId));
    }

    @GetMapping("/normalize-name") // UnaryOperator<String>
    public NormalizeNameResponse normalizeName(@RequestParam String name) {
        return new NormalizeNameResponse(name, exampleService.normalizeUserName(name));
    }

    @GetMapping("/posts/latest") // BinaryOperator<Post>
    public PostSummaryResponse latestPost(@RequestParam Long postId1, @RequestParam Long postId2) {
        return PostSummaryResponse.from(exampleService.pickMoreRecentlyUpdatedPost(postId1, postId2));
    }
}
