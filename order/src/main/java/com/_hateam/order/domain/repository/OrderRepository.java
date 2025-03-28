package com._hateam.order.domain.repository;

import com._hateam.order.domain.model.Order;
import com._hateam.order.domain.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {
    Order save(Order order);

    Optional<Order> findById(UUID orderId);

    List<Order> findAll(int page, int size, String sort);

    List<Order> search(
            String searchTerm,
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID companyId,
            UUID hubId,
            UUID productId,
            int page,
            int size,
            String sort
    );

    // 검색 결과 개수 조회 메서드
    long countSearchResults(
            String searchTerm,
            OrderStatus status,
            LocalDateTime startDate,
            LocalDateTime endDate,
            UUID companyId,
            UUID hubId,
            UUID productId
    );

    void delete(Order order);

    boolean existsById(UUID orderId);
}