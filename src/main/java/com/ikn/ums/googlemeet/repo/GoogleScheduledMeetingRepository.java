package com.ikn.ums.googlemeet.repo;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;

@Repository
public interface GoogleScheduledMeetingRepository extends JpaRepository<GoogleScheduledMeeting, Long> {

    @Modifying
    @Query(value = "ALTER SEQUENCE google_scheduled_meeting_seq RESTART WITH 1", nativeQuery = true)
    void resetSequence();
    
    
    
    // NEW: check if a Google event already exists
       //boolean existsBygoogleEventId(String googleEventId);
       
      // Optional<GoogleScheduledMeeting> findByGoogleEventId(String googleEventId);
       
       @Modifying
       @Query(value = "ALTER SEQUENCE google_scheduled_meeting_attendee_seq RESTART WITH 1", nativeQuery = true)
       void resetAttendeesSequence();
       
       @Query("""
       	    SELECT g.eventid
       	    FROM GoogleScheduledMeeting g
       	    WHERE g.eventid IN :ids
       	""")
       	Set<String> findExistingEventIds(@Param("ids") Set<String> ids);



}
