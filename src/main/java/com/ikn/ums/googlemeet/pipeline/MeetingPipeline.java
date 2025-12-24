package com.ikn.ums.googlemeet.pipeline;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.ikn.ums.googlemeet.processor.MeetingProcessor;


public class MeetingPipeline<T> {

    private List<T> meetings;
    private final MeetingProcessor<T> processor;

    private MeetingPipeline(List<T> meetings, MeetingProcessor<T> processor) {
        this.meetings = meetings;
        this.processor = processor;
    }

    /**
     * Creates and initializes a new MeetingPipeline instance.
     *
     * This is the entry point for processing a list of meetings.
     * The pipeline will:
     *   - Hold the initial meeting list
     *   - Use the provided MeetingProcessor to apply all processing steps
     *
     * @param meetings  the list of meeting DTOs to process
     * @param processor the processor responsible for handling all pipeline operations
     * @return a new MeetingPipeline instance ready for chaining pipeline steps
     */
    public static <T> MeetingPipeline<T> start(List<T> meetings, MeetingProcessor<T> processor) {
        return new MeetingPipeline<>(meetings, processor);
    }
    
    /**
     * Filters out meetings that have already been processed and inserted into the database.
     * Implementations should check by meeting ID.
     * @return the current MeetingPipeline with updated meeting list
     */
    public MeetingPipeline<T> filterAlreadyProcessed(){
    	this.meetings = processor.filterAlreadyProcessed(meetings);
    	return this;
    }


    /**
     * attach meetingType to a google meeting using the given processor
     * @return the current MeetingPipeline with updated meeting list
     */
    public MeetingPipeline<T> classifyType() {
        this.meetings = meetings.stream()
                .map(processor::classifyType)
                .collect(Collectors.toList());  
        return this;
    }

    /**
     * attach invitees to a google meeting  using the given processor
     * @return the current MeetingPipeline with updated meeting list
     */
    public MeetingPipeline<T> attachInvitees() {
        this.meetings = meetings.stream()
                .map(processor::attachInvitees)
                .collect(Collectors.toList());
        return this;
    }
    
    
    
    public MeetingPipeline<T> attachConferenceData() {
        this.meetings = meetings.stream()
                .map(m -> processor.attachConferenceData(m))
                .collect(Collectors.toList());
        return this;
    }

    
    
    /**
     * Attaches participants  information to each meeting in the pipeline.
     *
     * <p>The actual data enrichment is performed by the configured {@link MeetingProcessor},
     * which may fetch participants from Goog API or derive them from previously fetched data.</p>
     *
     * <p>The method updates the internal meeting list with enriched meeting objects and
     * returns the same pipeline instance to allow method chaining.</p>
     *
     * @return the current {@code MeetingPipeline} instance with participants attached
     */
    public MeetingPipeline<T> attachParticipants() {
        this.meetings = meetings.stream()
                .map(processor::attachParticipants)
                .collect(Collectors.toList());
        return this;
    }
    
    
    public MeetingPipeline<T> attachTranscripts() {
        this.meetings = meetings.stream()
                .map(processor::attachTranscripts)
                .collect(Collectors.toList());
        return this;
    }

    
    /**
     * Enriches each meeting DTO with additional metadata retrieved from external sources
     * (typically the Zoom Meeting Details API).
     *
     * <p>This step allows the processor to populate fields that are not included in the
     * basic meeting summary payload—such as timezone, join URL, creation timestamp,
     * agenda, passwords, and other meeting-level configuration details.
     *
     * <p>The processor's {@code enrichMeetingData()} method is invoked for each meeting
     * in the pipeline.
     *
     * <p>Returns the updated pipeline instance to allow fluent chaining.</p>
     *
     * @return this MeetingPipeline instance after enriching meeting data
     */
    public MeetingPipeline<T> enrichData() {
        this.meetings = meetings.stream()
                .map(processor::enrichMeetingData)
                .collect(Collectors.toList());
        return this;
    }

    
    /**
     * Preprocesses the meetings before applying the rest of the pipeline.
     *
     * This step allows each processor to perform its own custom logic,
     * such as:
     *   - Expanding recurring meetings into individual occurrences
     *   - Normalizing or enriching meeting data
     *   - Performing any initial transformations required before
     *     classification or invitee attachment
     *
     * The processor returns a transformed list, which becomes the new
     * state of the pipeline.
     *
     * @return the current MeetingPipeline with updated meeting list
     */
    public MeetingPipeline<T> preProcess() {
        this.meetings = processor.preProcess(this.meetings);
        return this;
    }

    
    /**
     * Applies date-range filtering to the current list of meetings.
     *
     * This step allows the processor to remove any meetings that fall
     * outside the provided start and end date-time boundaries.
     *
     * Notes:
     *  - Scheduled meeting processors typically override this to apply
     *    date filtering (e.g., next 30 days).
     *  - Completed meeting processors rely on the default implementation,
     *    which simply returns the list unchanged.
     *
     * @param startDateTime   the inclusive lower bound for meeting start times
     * @param endDateTime     the inclusive upper bound for meeting start times
     * @return the updated MeetingPipeline with filtered meetings
     */
    public MeetingPipeline<T> filterDateRange(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.meetings = processor.filterDateRange(this.meetings, startDateTime, endDateTime);
        return this;
    }


    /**
     * Completes the pipeline and returns the final processed meeting list.
     */
    public List<T> done() {
        return meetings;
    }

}
