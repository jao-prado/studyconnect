package com.itb.inf3em.studyconnect.model.dto;

public record AccountSecurityDTO(boolean hasPassword, boolean hasGoogle, boolean mfaEnabled) {
}
