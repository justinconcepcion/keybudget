package com.keybudget.user;

import com.keybudget.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public User upsertFromGoogle(String googleSub, String email, String name, String pictureUrl) {
        return userRepository.findByGoogleSub(googleSub)
                .map(existing -> {
                    existing.setEmail(email);
                    existing.setName(name);
                    existing.setPictureUrl(pictureUrl);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    User user = new User();
                    user.setGoogleSub(googleSub);
                    user.setEmail(email);
                    user.setName(name);
                    user.setPictureUrl(pictureUrl);
                    return userRepository.save(user);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getName(), user.getPictureUrl());
    }
}
