package com.ikn.ums.googlemeet.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;

import jakarta.transaction.Transactional;

@Repository
public interface GoogleCompletedMeetingRepository extends JpaRepository<GoogleCompletedMeeting, Long> {

    @Modifying
    @Transactional
    @Query(value = "ALTER SEQUENCE googlemeet_event_gen RESTART WITH 1", nativeQuery = true)
    void resetSequence();
    
    
 // NEW: check if a Google event already exists
    boolean existsBygoogleEventId(String googleEventId);
    
    Optional<GoogleCompletedMeeting> findByGoogleEventId(String googleEventId);


}
