package com.young.studyproject.user.presentation;

import com.young.studyproject.user.application.UserService;
import com.young.studyproject.user.application.dto.UserCreateCommand;
import com.young.studyproject.user.application.dto.UserResult;
import com.young.studyproject.user.application.dto.UserUpdateCommand;
import com.young.studyproject.user.presentation.dto.UserCreateRequest;
import com.young.studyproject.user.presentation.dto.UserResponse;
import com.young.studyproject.user.presentation.dto.UserUpdateRequest;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResult result = userService.create(new UserCreateCommand(request.name(), request.email()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(result));
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return UserResponse.from(userService.getById(id));
    }

    @GetMapping
    public List<UserResponse> getAll(@RequestParam(required = false) String name) {
        List<UserResult> results = (name == null || name.isBlank())
                ? userService.getAll()
                : userService.search(name);
        return results.stream().map(UserResponse::from).toList();
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        UserResult result = userService.update(id, new UserUpdateCommand(request.name(), request.email()));
        return UserResponse.from(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
