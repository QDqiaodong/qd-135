package com.example.heritagetrace.repository;

import com.example.heritagetrace.dto.response.InheritorDTO;
import com.example.heritagetrace.entity.Inheritor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InheritorRepository extends JpaRepository<Inheritor, Long> {
    Page<Inheritor> findByNameContaining(String name, Pageable pageable);

    Page<Inheritor> findByTitle(String title, Pageable pageable);

    @Query("SELECT new com.example.heritagetrace.dto.response.InheritorDTO(" +
           "i.id, i.name, i.title, i.specialty, i.contact, " +
           "i.certificateName, i.certificateContentType, i.certificateSize, i.certificateUploadTime, " +
           "i.createTime, i.updateTime) FROM Inheritor i WHERE " +
           "(:keyword IS NULL OR :keyword = '' OR " +
           "i.name LIKE %:keyword% OR i.title LIKE %:keyword% OR " +
           "i.specialty LIKE %:keyword%)")
    Page<InheritorDTO> searchDtoByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT new com.example.heritagetrace.dto.response.InheritorDTO(" +
           "i.id, i.name, i.title, i.specialty, i.contact, " +
           "i.certificateName, i.certificateContentType, i.certificateSize, i.certificateUploadTime, " +
           "i.createTime, i.updateTime) FROM Inheritor i WHERE i.id IN :ids")
    List<InheritorDTO> findDtoByIdIn(@Param("ids") List<Long> ids);
}
