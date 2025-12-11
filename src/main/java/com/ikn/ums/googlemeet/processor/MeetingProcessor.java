package com.ikn.ums.googlemeet.processor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GoogleMeetProcessor
 *
 * A generic processor contract that applies transformation steps on any
 * Google Meet DTO (scheduled, completed, or recurring).
 *
 * <p>This interface is used inside the GoogleMeetPipeline to ensure each
 * meeting goes through a well-defined processing flow:</p>
 *
 * <ul>
 *     <li><b>classifyType</b> – determines and sets the meeting type
 *         (single instance, recurring, scheduled, completed, etc.)</li>
 *     <li><b>attachInvitees</b> – attaches invitee/attendees list to the meeting</li>
 * </ul>
 *
 * @param <T> the type of meeting DTO (e.g., GoogleScheduledMeetingDto, GoogleCompletedMeetingDto)
 */
public interface MeetingProcessor<T> {

    /**
     * Classifies the meeting type based on its properties.
     *
     * @param meeting the meeting DTO
     * @return the enriched meeting DTO
     */
    T classifyType(T meeting);

    /**
     * Attaches meeting invites or additional metadata.
     *
     * @param meeting the meeting DTO
     * @return the enriched meeting DTO
     */
    T attachInvitees(T meeting);

    /**
     * Pre-process the meetings before further pipeline steps.
     * Example: Expand recurring meetings, enrich base data, etc.
     *
     * @param meetings the list of meeting DTOs
     * @return the processed list
     */
    List<T> preProcess(List<T> meetings);

    /**
     * Optional date range filtering (for scheduled meetings only).
     * Completed meetings will use the default method which does nothing.
     *
     * @param meetings the list of meeting DTOs
     * @param startDateTime filter start boundary (inclusive)
     * @param endDateTime filter end boundary (inclusive)
     * @return filtered list of meetings
     */
    default List<T> filterDateRange(List<T> meetings, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        return meetings; // completed meetings bypass this
    }

    /**
     * Filters out meetings that have already been processed and inserted into the database.
     * Implementations should check by UUID, meeting ID, or any unique identifier.
     *
     * @param meetings the list of meeting DTOs
     * @return the list containing only NEW meetings that are not present in the raw data DB
     */
    default List<T> filterAlreadyProcessed(List<T> meetings) {
        return meetings;
    }
}
