package com.ikn.ums.googlemeet.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "google_signed_in_user")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SignedInUserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_email")
    private String user;

    @Column(name = "display_name")
    private String displayName;
}
