package com.canchas.web.dto;

import com.canchas.web.model.User;

public record ClientSearchResultDTO(Long id, String fullName, String username, String dni,
                                    String email, String phoneNumber, String department,
                                    String province, boolean active) {
    public static ClientSearchResultDTO from(User user) {
        return new ClientSearchResultDTO(user.getId(), user.getFullName(), user.getUsername(), user.getDni(),
                user.getEmail(), user.getPhoneNumber(),
                user.getDepartment() == null ? "" : user.getDepartment().getDisplayName(),
                user.getProvince(), user.isActive());
    }
}
