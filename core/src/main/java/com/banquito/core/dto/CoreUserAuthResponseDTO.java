package com.banquito.core.dto;

import com.banquito.core.enums.CommonStatusEnum;
import com.banquito.core.enums.CoreUserRoleEnum;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CoreUserAuthResponseDTO {

    private Integer coreUserId;
    private String username;
    private String fullName;
    private CoreUserRoleEnum role;
    private CommonStatusEnum status;
    private LocalDateTime lastLogin;

    public CoreUserAuthResponseDTO() {
    }

    public CoreUserAuthResponseDTO(Integer coreUserId, String username, String fullName,
                                   CoreUserRoleEnum role, CommonStatusEnum status, LocalDateTime lastLogin) {
        this.coreUserId = coreUserId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.status = status;
        this.lastLogin = lastLogin;
    }
}
