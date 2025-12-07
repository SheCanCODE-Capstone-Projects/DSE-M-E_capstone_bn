package com.dseme.app.dtos.users;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RejectRequestDTO {
    @NotBlank(message = "Comment is required")
    private String comment;
}
