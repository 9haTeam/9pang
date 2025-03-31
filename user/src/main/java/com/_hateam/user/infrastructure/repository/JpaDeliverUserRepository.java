package com._hateam.user.infrastructure.repository;


import com._hateam.user.domain.enums.DeliverType;
import com._hateam.user.domain.enums.Status;
import com._hateam.user.domain.model.DeliverUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaDeliverUserRepository  extends JpaRepository<DeliverUser, UUID> {

    Optional<DeliverUser> findByDeliverId(UUID deliverId);

    Optional<DeliverUser> findByUser_UserId(Long userId);

    List<DeliverUser> findByHubIdAndDeletedAtIsNull(UUID hubId);

    List<DeliverUser> findByDeletedAtIsNull();

    Page<DeliverUser> findByNameContainingAndDeletedAtIsNull(String name, Pageable pageable);

    Page<DeliverUser> findByNameContainingAndHubIdAndDeletedAtIsNull(String name, UUID hubId, Pageable pageable);

    List<DeliverUser> findByDeliverTypeAndDeletedAtIsNull(DeliverType deliverType);


    // 1) HUB인 애들만 rotationOrder asc 로 전부 조회
    List<DeliverUser> findByStatusAndDeliverTypeOrderByRotationOrderAsc(Status status, DeliverType deliverType);

    // 2) COMPANY면서 특정 hubId인 애들만 rotationOrder asc 로 전부 조회
    List<DeliverUser> findByStatusAndDeliverTypeAndHubIdOrderByRotationOrderAsc(
            Status status, DeliverType deliverType, UUID hubId);

    // (deliverType, hubId)별 최대 rotationOrder 조회
    @Query("""
           SELECT MAX(d.rotationOrder) 
           FROM DeliverUser d
           WHERE d.deliverType = :deliverType
             AND (
                (:hubId IS NULL AND d.hubId IS NULL)
                OR (:hubId IS NOT NULL AND d.hubId = :hubId)
             )
           """)
    Integer findMaxRotationOrder(@Param("deliverType") DeliverType deliverType,
                                 @Param("hubId") UUID hubId);


}