package com.arquitectura.persistence.repository;

import com.arquitectura.domain.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Integer> {

    // Creamos un método para buscar todos los mensajes de un canal específico.
    // Spring generará la consulta SQL basándose en el nombre del método.
    List<Message> findByChannelChannelId(int channelId);
    @Query("SELECT m FROM Message m JOIN FETCH m.author WHERE m.channel.channelId = :channelId ORDER BY m.timestamp ASC")
    List<Message> findByChannelIdWithAuthors(@Param("channelId") int channelId);
}