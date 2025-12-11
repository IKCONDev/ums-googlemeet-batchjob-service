package com.ikn.ums.googlemeet.service;

import java.util.List;

import com.ikn.ums.googlemeet.entity.GoogleScheduledMeeting;
import com.ikn.ums.googlemeet.entity.GoogleCompletedMeeting;

public interface GoogleMeetingPersistenceService {
    
    /**
     * Persists the latest set of Google scheduled meetings into the application's
     * raw data storage.
     * <p>
     * This method performs a full refresh of the scheduled meetings table by:
     * <ul>
     *     <li>Deleting all existing scheduled meeting records</li>
     *     <li>Resetting the primary key sequence</li>
     *     <li>Inserting the newly fetched meeting records</li>
     * </ul>
     * The operation executes within a transactional boundary to ensure atomicity 
     * and maintain data consistency.
     *
     * @param scheduledMeetings the list of scheduled meeting entities to persist
     * @return the list of scheduled meeting records saved to the database
     */
    List<GoogleScheduledMeeting> deleteResetAndPersist(List<GoogleScheduledMeeting> scheduledMeetings);

    
    /**
     * Persists the latest set of Google completed meetings into the application's
     * raw data storage.
     * <p>
     * This method inserts newly completed meeting records into storage.
     * The operation runs inside a transactional boundary to ensure
     * atomicity and data consistency.
     *
     * @param completedMeetings the list of completed Google meeting entities to persist
     * @return the list of completed meeting records saved to the database
     */
    List<GoogleCompletedMeeting> persistCompletedMeetings(List<GoogleCompletedMeeting> completedMeetings);
}
