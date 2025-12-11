package com.ikn.ums.googlemeet.exception;

public class ErrorCodeMessages {

	 public static final String ERR_GOOGLEMEET_USERS_NOT_FOUND_CODE = "EVENTS-NOT-FOUND-1001";
	 public static final String ERR_GOOGLEMEET_USERS_NOT_FOUND_MSG = "Users / Employees not found for batch processing";
	 
	 public static final String ERR_GOOGLEMEET_UNKNOWN_ERROR_CODE = "ERR_GOOGLEMEET_UNKNOWN_ERROR_CODE-1002";
	 public static final String ERR_GOOGLEMEET_UNKNOWN_ERROR_MSG = "An Unknown Error occured while batch processing";
	 
	 public static final String ERR_GOOGLEMEET_SERVICE_NOT_FOUND_CODE = "EVENTS-CORE-SERVICE-1003";
	 public static final String ERR_GOOGLEMEET_SERVICE_NOT_FOUND_MSG = "Requested Employee Service not present.";
	 
	 public static final String ERR_GOOGLEMEET_SERVICE_EXCEPTION_CODE = "ERR_GOOGLEMEET_SERVICE_EXCEPTION_CODE-1004";
	 public static final String ERR_GOOGLEMEET_SERVICE_EXCEPTION_MSG = "Exception Occured in the Employee Service Layer."; 	

	 public static final String ERR_GOOGLEMEET_EMAIL_ID_EMPTY_CODE = "ERR_GOOGLEMEET_EMAIL_ID_EMPTY_CODE-1005";
	 public static final String ERR_GOOGLEMEET_EMAIL_ID_EMPTY_MSG = "User id is empty"; 
	 
	 public static final String ERR_GOOGLEMEET_EVENT_GET_UNSUCCESS_CODE = "ERR_GOOGLEMEET_EVENT_GET_UNSUCCESS_CODE-1006";
	 public static final String ERR_GOOGLEMEET_EVENT_GET_UNSUCCESS_MSG = "Error Occured While Retrieving Event Details !";
	 
	 public static final String ERR_GOOGLEMEET_EVENT_GET_ATT_COUNT_UNSUCCESS_CODE = "ERR_GOOGLEMEET_EVENT_GET_ATT_COUNT_UNSUCCESS_CODE-1007";
	 public static final String ERR_GOOGLEMEET_EVENT_GET_ATT_COUNT_UNSUCCESS_MSG = "Error Occured While Retrieving total attended events count !";
	 
	 public static final String ERR_GOOGLEMEET_BATCHPROCESS_UNSUCCESS_CODE = "ERR_GOOGLEMEET_BATCHPROCESS_UNSUCCESS_CODE-1008";
	 public static final String ERR_GOOGLEMEET_BATCHPROCESS_UNSUCCESS_MSG = "Error Occured While batch processing !";
	 
	 public static final String ERR_GOOGLEMEET_GET_ORGEVENTS_COUNT_UNSUCCESS_CODE = "ERR_GOOGLEMEET_GET_ORGEVENTS_COUNT_UNSUCCESS_CODE-1009";
	 public static final String ERR_GOOGLEMEET_GET_ORGEVENTS_COUNT_UNSUCCESS_MSG = "Error Occured While Retrieving total organized events count !";
	
	 public static final String ERR_GOOGLEMEET_INVALID_EVENTID_CODE = "ERR_GOOGLEMEET_INVALID_EVENTID_CODE-1010";
	 public static final String ERR_GOOGLEMEET_INVALID_EVENTID_MSG = "Invalid Event id !";
	 
	 public static final String ERR_GOOGLEMEET_EVENTS_GET_ALL_UNSUCCESS_CODE = "ERR_GOOGLEMEET_EVENTS_GET_ALL_UNSUCCESS_CODE-1010";
	 public static final String ERR_GOOGLEMEET_EVENTS_GET_ALL_UNSUCCESS_MSG = "Error occured while retrieving all events for user!";
	 
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_SUCCESS_CODE = "ERR_GOOGLEMEET_BATCH_PROCESS_SUCCESS_CODE-1011";
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_SUCCESS_MSG = "Raw Data Batch processing is Successfull !";
	 
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_GET_UNSUCCESS_CODE = "ERR_GOOGLEMEET_BATCH_PROCESS_GET_UNSUCCESS_CODE-1012";
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_GET_UNSUCCESS_MSG = "Error occured while fethcing batch process list";
	 
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_EMPTY_CODE = "ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_EMPTY_CODE-1013";
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_EMPTY_MSG = "Cron time or expression is empty.";
	 
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_UPADTE_UNSUCCESS_CODE = "ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_UPADTE_UNSUCCESS_CODE-1014";
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_UPADTE_UNSUCCESS_MSG = "Error while updating crontime for batch process.";
	 
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_GET_UNSUCCESS_CODE = "ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_GET_UNSUCCESS_CODE-1015";
	 public static final String ERR_GOOGLEMEET_BATCH_PROCESS_CRONTIME_GET_UNSUCCESS_MSG = "Error while fatching crontime of batch process.";

	 public static final String ERR_GOOGLEMEET_EVENTID_EMPTY_CODE = "ERR_GOOGLEMEET_EVENTID_EMPTY_CODE-1016";
	 public static final String ERR_GOOGLEMEET_EVENTID_EMPTY_MSG = "Empty event id(s) !";
	 
	 public static final String ERR_GOOGLEMEET_CONFERENCE_IDS_GET_UNSUCCESS_CODE = "GM_CRID_001";
	 public static final String ERR_GOOGLEMEET_CONFERENCE_IDS_GET_UNSUCCESS_MSG = "Failed to fetch Google Meet Conference Record IDs.";
	 
	 public static final String ERR_GOOGLEMEET_CONFERENCE_RECORD_GET_UNSUCCESS_CODE = "GM_CREC_001";
	 public static final String ERR_GOOGLEMEET_CONFERENCE_RECORD_GET_UNSUCCESS_MSG = "Failed to fetch Google Meet Conference Record.";

	 public static final String ERR_GOOGLEMEET_PARTICIPANTS_GET_UNSUCCESS_CODE = "GM_PART_001";
	 public static final String ERR_GOOGLEMEET_PARTICIPANTS_GET_UNSUCCESS_MSG = "Failed to fetch Google Meet Participants.";

}
