//package com.ikn.ums.googlemeet.dto;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//
//@Data
//public class GoogleEntryPointDto {
//
//    @JsonProperty("entryPointType")
//    private String entryPointType; // e.g., "video", "phone"
//
//    @JsonProperty("uri")
//    private String uri;
//
//    @JsonProperty("label")
//    private String label;
//
//    @JsonProperty("pin")
//    private String pin;
//}

package com.ikn.ums.googlemeet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleEntryPointDto {
    private String entryPointType; // video, phone, more
    private String uri;
    private String label;
    private String pin;          // optional
    private String regionCode;   // optional for phone
}
