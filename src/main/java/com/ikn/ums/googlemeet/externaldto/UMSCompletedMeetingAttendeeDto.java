package com.ikn.ums.googlemeet.externaldto;

import lombok.Data;

/**
 * Represents a scheduled meeting attendee with the following details:
 *
 * <ul>
 *   <li><b>email</b>  - Email address of the attendee</li>
 *   <li><b>name</b>   - Full name of the attendee</li>
 *   <li><b>role</b>   - Attendee role (Attendee / Organizer)</li>
 *   <li><b>status</b> - Invitation response status (accepted / none / notResponded)</li>
 *   <li><b>type</b>   - Whether attendee is required or optional</li>
 * </ul>
 */

@Data
public class UMSCompletedMeetingAttendeeDto {

    private Long id;
    private String email;
    private String name;
    private String type;
    private String status;
    private String role;
}
