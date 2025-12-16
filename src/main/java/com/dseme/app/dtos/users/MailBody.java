package com.dseme.app.dtos.users;

import lombok.Builder;

@Builder
public record MailBody(String to, String subject, String text) {
}
