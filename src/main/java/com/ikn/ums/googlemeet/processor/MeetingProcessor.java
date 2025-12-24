package com.ikn.ums.googlemeet.processor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GoogleMeetProcessor
 *
 * A generic processor interface that applies transformation steps on any
 * Google Meet DTO (scheduled, completed, or recurring).
 *
 * <p>This interface is used inside the GoogleMeetPipeline to ensure each
 * meeting goes through a well-defined processing flow:</p>
 *
 * <ul>
 *     <li><b>classifyType</b> – determines and sets the meeting type
 *         (single instance, recurring, scheduled, completed, etc.)</li>
 *     <li><b>attachInvitees</b> – attaches invitee/attendees list to the meeting</li>
 *     <li><b>attachParticipants</b> – attaches participants from conference records</li>
 *     <li><b>attachTranscripts</b> – attaches transcripts for completed meetings</li>
 *     <li><b>enrichMeetingData</b> – adds additional metadata like join URL, organizer info</li>
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
    
    
    
    
    T attachConferenceData(T meeting) ;
    

    /**
     * Attaches participant details to the given Google Meet meeting.
     *
     * <p>This method enriches a meeting DTO with its full participant list,
     * fetched from the Google Meet Conference Records API
     * (e.g., /v2/conferenceRecords/{conferenceRecordId}/participants).</p>
     *
     * <p>By default, this method returns the meeting unchanged. Concrete
     * implementations for completed meetings should override this method
     * to fetch and attach participants.</p>
     *
     * @param meeting the meeting DTO to enrich
     * @return the meeting DTO, either enriched or unchanged
     */
    default T attachParticipants(T meeting) {
        return meeting;
    }

    /**
     * Attaches transcript data to the Google Meet meeting.
     *
     * <p>This is a no-op (default) implementation because not all meetings
     * have transcripts. Only completed meetings with transcription enabled
     * will have transcript data available.</p>
     *
     * <p>Processors for completed meetings should override this method to
     * fetch and attach transcript data using:
     * /v2/conferenceRecords/{conferenceRecordId}/transcripts and
     * Google Docs API to retrieve the actual text.</p>
     *
     * @param meeting the meeting DTO
     * @return the same meeting DTO with attached transcript data (unchanged in default implementation)
     */
    default T attachTranscripts(T meeting) {
        return meeting;
    }

    /**
     * Hook method for enriching a Google Meet meeting with additional metadata
     * fetched from external sources (e.g., Calendar API or Conference Records API).
     *
     * <p>This default implementation performs no action. Concrete processor
     * implementations may override this method to populate fields such
     * as join URL, timezone, agenda, organizer info, or other metadata.</p>
     *
     * @param meeting the meeting DTO to enrich
     * @return the same meeting instance, optionally enriched
     */
    default T enrichMeetingData(T meeting) {
        return meeting;
    }

    /**
     * Pre-processes the meetings before further pipeline steps.
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
