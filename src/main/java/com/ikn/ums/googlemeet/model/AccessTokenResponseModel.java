package com.ikn.ums.googlemeet.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccessTokenResponseModel {
    
    @JsonProperty("access_token")
    private String accessToken; 
   
    @JsonProperty("expires_in")
    private Long expiresIn;
    
    @JsonProperty("refresh_token") 
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType; 
}