package com.ikn.ums.googlemeet.repo;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;

import jakarta.transaction.Transactional;

import org.springframework.data.repository.query.Param;

@Repository
public interface GoogleCompletedMeetingRepository extends JpaRepository<GoogleCompletedMeeting, Long> {

    @Modifying
    @Transactional
    @Query(value = "ALTER SEQUENCE googlemeet_event_gen RESTART WITH 1", nativeQuery = true)
    void resetSequence();

    @Query("""
    	    SELECT g.eventid
    	    FROM GoogleCompletedMeeting g
    	    WHERE g.eventid IN :ids
    	""")
    	Set<String> findExistingEventIds(@Param("ids") Set<String> ids);

    
    
 // NEW: check if a Google event already exists
//    boolean existsBygoogleEventId(String googleEventId);
//    
//    Optional<GoogleCompletedMeeting> findByGoogleEventId(String googleEventId);


}
