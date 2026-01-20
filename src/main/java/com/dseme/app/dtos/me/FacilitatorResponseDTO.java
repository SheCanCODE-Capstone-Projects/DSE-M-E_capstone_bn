package com.dseme.app.dtos.me;

import lombok.*;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilitatorResponseDTO {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String employeeId;
    private String department;
    private String specialization;
    private String status;
    private List<AssignedCourseDTO> assignedCourses;
}