package com.ikn.ums.googlemeet.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Docs_Destination")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DocsDestinationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document")
    private String document;

    @Column(name = "exportUri")
    private String exportUri;
}
