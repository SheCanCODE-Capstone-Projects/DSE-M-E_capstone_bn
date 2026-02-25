package com.dseme.app.repositories;

import com.dseme.app.models.MeCohortBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MeCohortBatchRepository extends JpaRepository<MeCohortBatch, UUID> {
    List<MeCohortBatch> findByPartner_PartnerId(String partnerId);
}

