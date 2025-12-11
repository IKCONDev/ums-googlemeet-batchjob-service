package com.ikn.ums.googlemeet.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;
import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.repo.GoogleCompletedMeetingRepository;
import com.ikn.ums.googlemeet.repo.GoogleScheduledMeetingRepository;
import com.ikn.ums.googlemeet.service.GoogleMeetingPersistenceService;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class GoogleMeetingPersistenceServiceImpl implements GoogleMeetingPersistenceService {

    @Autowired
    private GoogleScheduledMeetingRepository scheduledMeetingRepository;

    @Autowired
    private GoogleCompletedMeetingRepository completedMeetingRepository;

    /**
     * Deletes existing scheduled meetings, resets sequences, and persists new scheduled meetings.
     */
    @Override
    @Transactional(value = TxType.REQUIRED)
    public List<GoogleScheduledMeeting> deleteResetAndPersist(List<GoogleScheduledMeeting> scheduledMeetings) {
        String methodName = "deleteResetAndPersist()";

        // Delete all existing scheduled meetings
        scheduledMeetingRepository.deleteAll();
        log.info("{} - Deleted existing records of scheduled meetings", methodName);

        // Reset sequences if your table has auto-increment/sequence requirements
        scheduledMeetingRepository.resetSequence();
        log.info("{} - Sequence reset to 1 for scheduled_meeting_rawdata_tab", methodName);

//        scheduledMeetingRepository.resetAttendeesSequence();
//        log.info("{} - Sequence reset to 1 for scheduled_meeting_attendee_rawdata_tab", methodName);

        // Save all new scheduled meetings
        List<GoogleScheduledMeeting> persistedMeetings = scheduledMeetingRepository.saveAll(scheduledMeetings);
        log.info("{} - Saved all upcoming Google meetings into raw data DB", methodName);

        return persistedMeetings;
    }

    /**
     * Persists completed Google meetings into the DB.
     */
    @Override
    @Transactional(value = TxType.REQUIRED)
    public List<GoogleCompletedMeeting> persistCompletedMeetings(List<GoogleCompletedMeeting> meetings) {
        String methodName = "persistCompletedMeetings()";

        List<GoogleCompletedMeeting> persistedMeetings = completedMeetingRepository.saveAll(meetings);
        log.info("{} - Saved {} completed Google meetings into raw data DB",
                 methodName, meetings.size());

        return persistedMeetings;
    }
}
