package com.keybudget.user;

import com.keybudget.user.dto.UserProfileResponse;

public interface UserService {
    User upsertFromGoogle(String googleSub, String email, String name, String pictureUrl);
    User findById(Long userId);
    UserProfileResponse getProfile(Long userId);
    UserProfileResponse updateCurrency(Long userId, String currency);
}
