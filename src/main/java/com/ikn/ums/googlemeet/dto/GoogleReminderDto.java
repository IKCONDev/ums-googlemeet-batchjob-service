package com.ikn.ums.googlemeet.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GoogleReminderDto {
    private Boolean useDefault;
    private List<Reminder> overrides;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Reminder {
        private String method; 
        private Integer minutes;
    }
}
