package com.arquitectura.persistence.repository;

import com.arquitectura.domain.TranscripcionAudio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TranscripcionAudioRepository extends JpaRepository<TranscripcionAudio, UUID> {
    @Query("SELECT DISTINCT ta FROM TranscripcionAudio ta JOIN FETCH ta.mensaje m JOIN FETCH m.author JOIN FETCH m.channel")
    List<TranscripcionAudio> findAllWithDetails();
}