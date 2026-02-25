package com.dseme.app.controllers.facilitator;

import com.dseme.app.dtos.facilitator.*;
import com.dseme.app.services.facilitator.ParticipantManagementService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/facilitator/participants")
@RequiredArgsConstructor
public class ParticipantController extends FacilitatorBaseController {

    private final ParticipantManagementService participantService;

    @GetMapping
    public ResponseEntity<List<ParticipantDTO>> getParticipants(
            HttpServletRequest request,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrollmentDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        FacilitatorContext context = getFacilitatorContext(request);
        if (cohortId != null) {
            return ResponseEntity.ok(participantService.getParticipantsByCohortId(context, cohortId));
        }
        return ResponseEntity.ok(participantService.getParticipantsByCohort(context));
    }

    @GetMapping("/list")
    public ResponseEntity<ParticipantListResponseDTO> getParticipantsList(
            HttpServletRequest request,
            @RequestParam(required = false) UUID cohortId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "enrollmentDate") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        FacilitatorContext context = getFacilitatorContext(request);
        List<ParticipantListItemDTO> participants;
        if (cohortId != null) {
            participants = participantService.getParticipantsListByCohortId(context, cohortId);
        } else {
            participants = participantService.getParticipantsListByCohort(context);
        }
        
        ParticipantListResponseDTO response = ParticipantListResponseDTO.builder()
                .participants(participants)
                .totalElements(participants.size())
                .totalPages(1)
                .currentPage(0)
                .pageSize(participants.size())
                .hasNext(false)
                .hasPrevious(false)
                .build();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/statistics")
    public ResponseEntity<ParticipantStatisticsDTO> getStatistics(HttpServletRequest request) {
        FacilitatorContext context = getFacilitatorContext(request);
        return ResponseEntity.ok(participantService.getStatistics(context));
    }

    @PostMapping
    public ResponseEntity<ParticipantDTO> createParticipant(@RequestBody CreateParticipantRequest request, HttpServletRequest httpRequest) {
        FacilitatorContext context = getFacilitatorContext(httpRequest);
        return ResponseEntity.ok(participantService.createParticipant(context, request));
    }

    @GetMapping("/{participantId}")
    public ResponseEntity<ParticipantDTO> getParticipant(@PathVariable UUID participantId, HttpServletRequest request) {
        FacilitatorContext context = getFacilitatorContext(request);
        return ResponseEntity.ok(participantService.getParticipant(context, participantId));
    }

    @PutMapping("/{participantId}")
    public ResponseEntity<ParticipantDTO> updateParticipant(
            @PathVariable UUID participantId,
            @RequestBody UpdateParticipantRequest request,
            HttpServletRequest httpRequest) {
        FacilitatorContext context = getFacilitatorContext(httpRequest);
        return ResponseEntity.ok(participantService.updateParticipant(context, participantId, request));
    }

    @DeleteMapping("/{participantId}")
    public ResponseEntity<Void> deleteParticipant(@PathVariable UUID participantId, HttpServletRequest request) {
        FacilitatorContext context = getFacilitatorContext(request);
        participantService.deleteParticipant(context, participantId);
        return ResponseEntity.noContent().build();
    }
}
