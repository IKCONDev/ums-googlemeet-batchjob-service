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
     * Implementations should check by UUID, meeting ID, or any unique identifier.
     * @return the current MeetingPipeline with updated meeting list
     */
    public MeetingPipeline<T> filterAlreadyProcessed(){
    	this.meetings = processor.filterAlreadyProcessed(meetings);
    	return this;
    }


    /**
     * attach meetingType to a zoom meeting using the given processor
     * @return the current MeetingPipeline with updated meeting list
     */
    public MeetingPipeline<T> classifyType() {
        this.meetings = meetings.stream()
                .map(processor::classifyType)
                .collect(Collectors.toList());  
        return this;
    }

    /**
     * attach invitees to a zoom meeting  using the given processor
     * @return the current MeetingPipeline with updated meeting list
     */
    public MeetingPipeline<T> attachInvitees() {
        this.meetings = meetings.stream()
                .map(processor::attachInvitees)
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
