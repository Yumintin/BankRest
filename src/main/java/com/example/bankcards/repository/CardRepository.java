package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardRepository extends JpaRepository<Card, UUID> {
    @Query("select c from Card c " +
            "where (:q is null or lower(c.owner.fullName) like lower(concat('%', :q, '%')) " +
            "or lower(c.last4) like lower(concat('%', :q, '%')))")
    Page<Card> search(@Param("q") String q, Pageable pageable);

    @Query("select c from Card c " +
            "where c.owner.id = :ownerId and (:q is null or :q = '' " +
            "or lower(c.owner.fullName) like lower(concat('%', :q, '%')) " +
            "or c.last4 like concat('%', :q, '%'))")
    Page<Card> searchForUser(@Param("ownerId") Long ownerId, @Param("q") String q, Pageable pageable);

    Page<Card> findAllByOwnerId(Long ownerId, Pageable pageable);

    Optional<Card> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Card c where c.id = :id")
    Optional<Card> findByIdForUpdate(@Param("id") UUID id);
}