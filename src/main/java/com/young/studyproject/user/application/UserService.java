package com.young.studyproject.user.application;

import com.young.studyproject.common.exception.EntityNotFoundException;
import com.young.studyproject.user.application.dto.UserCreateCommand;
import com.young.studyproject.user.application.dto.UserResult;
import com.young.studyproject.user.application.dto.UserUpdateCommand;
import com.young.studyproject.user.domain.User;
import com.young.studyproject.user.domain.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResult create(UserCreateCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new IllegalArgumentException("이미 존재하는 이메일입니다: " + command.email());
        }

        User user = User.builder()
                .name(command.name())
                .email(command.email())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return UserResult.from(userRepository.save(user));
    }

    public UserResult getById(Long id) {
        return UserResult.from(findUser(id));
    }

    public List<UserResult> getAll() {
        return userRepository.findAll().stream()
                .map(UserResult::from)
                .toList();
    }

    public List<UserResult> search(String name) {
        return userRepository.search(name).stream()
                .map(UserResult::from)
                .toList();
    }

    @Transactional
    public UserResult update(Long id, UserUpdateCommand command) {
        User user = findUser(id);
        User updated = user.update(command.name(), command.email());
        return UserResult.from(userRepository.save(updated));
    }

    @Transactional
    public void delete(Long id) {
        findUser(id);
        userRepository.deleteById(id);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다. id=" + id));
    }
}
