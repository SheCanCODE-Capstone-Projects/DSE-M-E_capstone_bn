package com.dseme.app.repositories;

import com.dseme.app.dtos.meofficer.ParticipantSearchCriteria;
import com.dseme.app.models.Enrollment;
import com.dseme.app.models.Participant;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for participant filtering.
 * Used for advanced search and filter queries.
 */
public class ParticipantSpecification {

    /**
     * Creates a specification for partner-level filtering with search criteria.
     */
    public static Specification<Participant> withPartnerAndCriteria(
            String partnerId,
            ParticipantSearchCriteria criteria
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Partner filter (mandatory)
            predicates.add(cb.equal(root.get("partner").get("partnerId"), partnerId));

            // Full-text search (name, email, phone)
            if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                String searchPattern = "%" + criteria.getSearch().toLowerCase() + "%";
                Predicate nameSearch = cb.or(
                        cb.like(cb.lower(root.get("firstName")), searchPattern),
                        cb.like(cb.lower(root.get("lastName")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern),
                        cb.like(root.get("phone"), "%" + criteria.getSearch() + "%")
                );
                predicates.add(nameSearch);
            }

            // Cohort filter (through enrollments)
            if (criteria.getCohortId() != null) {
                Join<Participant, Enrollment> enrollmentJoin = root.join("enrollments");
                predicates.add(cb.equal(enrollmentJoin.get("cohort").get("id"), criteria.getCohortId()));
                query.distinct(true);
            }

            // Facilitator filter (through enrollments -> module -> module assignments)
            if (criteria.getFacilitatorId() != null) {
                // Use EXISTS subquery for better performance
                Subquery<Long> assignmentSubquery = query.subquery(Long.class);
                Root<com.dseme.app.models.ModuleAssignment> assignmentRoot = 
                        assignmentSubquery.from(com.dseme.app.models.ModuleAssignment.class);
                Join<com.dseme.app.models.ModuleAssignment, com.dseme.app.models.TrainingModule> moduleJoin = 
                        assignmentRoot.join("module");
                Join<com.dseme.app.models.ModuleAssignment, com.dseme.app.models.Cohort> cohortJoin = 
                        assignmentRoot.join("cohort");
                
                // Join enrollments to check if participant has enrollment with this module
                Subquery<Long> enrollmentSubquery = query.subquery(Long.class);
                Root<Enrollment> enrollmentRoot = enrollmentSubquery.from(Enrollment.class);
                enrollmentSubquery.select(cb.literal(1L))
                        .where(cb.and(
                                cb.equal(enrollmentRoot.get("participant").get("id"), root.get("id")),
                                cb.equal(enrollmentRoot.get("module").get("id"), moduleJoin.get("id")),
                                cb.equal(enrollmentRoot.get("cohort").get("id"), cohortJoin.get("id"))
                        ));
                
                assignmentSubquery.select(cb.literal(1L))
                        .where(cb.and(
                                cb.equal(assignmentRoot.get("facilitator").get("id"), criteria.getFacilitatorId()),
                                cb.exists(enrollmentSubquery)
                        ));
                
                predicates.add(cb.exists(assignmentSubquery));
                query.distinct(true);
            }

            // Enrollment status filter
            if (criteria.getStatus() != null) {
                Join<Participant, Enrollment> enrollmentJoin = root.join("enrollments");
                predicates.add(cb.equal(enrollmentJoin.get("status"), criteria.getStatus()));
                query.distinct(true);
            }

            // Enrollment date range filter
            if (criteria.getEnrollmentDateStart() != null || criteria.getEnrollmentDateEnd() != null) {
                Join<Participant, Enrollment> enrollmentJoin = root.join("enrollments");
                if (criteria.getEnrollmentDateStart() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(
                            enrollmentJoin.get("enrollmentDate"), criteria.getEnrollmentDateStart()));
                }
                if (criteria.getEnrollmentDateEnd() != null) {
                    predicates.add(cb.lessThanOrEqualTo(
                            enrollmentJoin.get("enrollmentDate"), criteria.getEnrollmentDateEnd()));
                }
                query.distinct(true);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
