package com.ikn.ums.googlemeet.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SignedInUserDto {

    private String user;          // email or user id from Google
    private String displayName;   // display name
}
