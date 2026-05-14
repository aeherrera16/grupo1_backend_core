package com.banquito.core.enums;

import lombok.Getter;

@Getter
public enum CoreUserRoleEnum {

    OPERARIO("OPERARIO");

    private final String value;

    CoreUserRoleEnum(String value) {
        this.value = value;
    }
}
