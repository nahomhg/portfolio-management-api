package io.github.nahomgh.portfolio.repository;

import io.github.nahomgh.portfolio.entity.Holding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HoldingRepository extends JpaRepository<Holding, Long> {

    Optional<Holding> findByAssetAndUser_Id(String assetName, Long userId);
    List<Holding> findAllByUser_id(Long user_id);



}
