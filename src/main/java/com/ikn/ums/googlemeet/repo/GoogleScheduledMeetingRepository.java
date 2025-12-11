package com.ikn.ums.googlemeet.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;

@Repository
public interface GoogleScheduledMeetingRepository extends JpaRepository<GoogleScheduledMeeting, Long> {

    @Modifying
    @Query(value = "ALTER SEQUENCE google_scheduled_meeting_seq RESTART WITH 1", nativeQuery = true)
    void resetSequence();

}
