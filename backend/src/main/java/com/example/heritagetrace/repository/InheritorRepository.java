package com.example.heritagetrace.repository;

import com.example.heritagetrace.dto.response.InheritorDTO;
import com.example.heritagetrace.entity.Inheritor;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InheritorRepository extends JpaRepository<Inheritor, Long> {
    Page<Inheritor> findByNameContaining(String name, Pageable pageable);

    Page<Inheritor> findByTitle(String title, Pageable pageable);

    @Query("SELECT new com.example.heritagetrace.dto.response.InheritorDTO(" +
           "i.id, i.name, i.title, i.specialty, i.contact, " +
           "i.certificateName, i.certificateContentType, i.certificateSize, i.certificateUploadTime, " +
           "i.createTime, i.updateTime, i.status) FROM Inheritor i WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "i.name LIKE %:keyword% OR i.title LIKE %:keyword% OR " +
           "i.specialty LIKE %:keyword%)")
    Page<InheritorDTO> searchDtoByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT new com.example.heritagetrace.dto.response.InheritorDTO(" +
           "i.id, i.name, i.title, i.specialty, i.contact, " +
           "i.certificateName, i.certificateContentType, i.certificateSize, i.certificateUploadTime, " +
           "i.createTime, i.updateTime, i.status) FROM Inheritor i WHERE i.id IN :ids")
    List<InheritorDTO> findDtoByIdIn(@Param("ids") List<Long> ids);

    /**
     * 看板在册人数：停档（SUSPENDED）不进这个数；
     * 历史行状态可能为 NULL，一律按在册计入。
     */
    @Query("SELECT COUNT(i) FROM Inheritor i WHERE i.status IS NULL OR i.status <> 'SUSPENDED'")
    long countRegistered();

    /**
     * 停档/启用专用：SELECT ... FOR UPDATE 锁住传承人行。
     * 两人同一时刻一个停档、一个启用时，后到的事务阻塞到先到的提交，
     * 再基于最新落成状态继续，最终只有最后提交的那一份生效。
     * 挂关联前也用同一把锁核对状态，停档与挂上不会同时成立。
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inheritor i WHERE i.id = :id")
    Optional<Inheritor> findByIdForUpdate(@Param("id") Long id);
}
