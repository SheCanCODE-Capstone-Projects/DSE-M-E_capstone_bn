package com.dseme.app.repositories;

import com.dseme.app.dtos.meofficer.FacilitatorSearchCriteria;
import com.dseme.app.enums.AvailabilityStatus;
import com.dseme.app.enums.CohortStatus;
import com.dseme.app.enums.Role;
import com.dseme.app.models.User;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specifications for facilitator filtering.
 * Used for advanced search and filter queries.
 */
public class FacilitatorSpecification {

    /**
     * Creates a specification for partner-level filtering with search criteria.
     */
    public static Specification<User> withPartnerAndCriteria(
            String partnerId,
            FacilitatorSearchCriteria criteria
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Role filter (must be FACILITATOR)
            predicates.add(cb.equal(root.get("role"), Role.FACILITATOR));

            // Partner filter (mandatory)
            predicates.add(cb.equal(root.get("partner").get("partnerId"), partnerId));

            // Active status filter (only active facilitators by default)
            predicates.add(cb.equal(root.get("isActive"), true));

            // Full-text search (name, email)
            if (criteria.getSearch() != null && !criteria.getSearch().trim().isEmpty()) {
                String searchPattern = "%" + criteria.getSearch().toLowerCase() + "%";
                Predicate nameSearch = cb.or(
                        cb.like(cb.lower(root.get("firstName")), searchPattern),
                        cb.like(cb.lower(root.get("lastName")), searchPattern),
                        cb.like(cb.lower(root.get("email")), searchPattern)
                );
                predicates.add(nameSearch);
            }

            // Center filter (location/region)
            if (criteria.getCenterId() != null) {
                predicates.add(cb.equal(root.get("center").get("id"), criteria.getCenterId()));
            }

            // Note: AvailabilityStatus, subjectArea, and performance filters
            // will be applied in the service layer after calculating metrics
            // as they require joins with other tables

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
