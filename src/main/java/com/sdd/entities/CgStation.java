package com.sdd.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "CgStation")
public class CgStation {

    @Id
    @Column(name = "STATION_ID", nullable = false)
    private String stationId;

    @Column(name = "STATION_NAME", nullable = false)
    private String stationName;

    @Column(name = "RHQ_ID", nullable = false)
    private String rhqId;

    @Column(name = "IS_FLAG")
    private String isFlag;

    @Column(name = "CREATED_ON")
    private Timestamp createdOn;

    @Column(name = "UPDATED_ON", nullable = false)
    private Timestamp updatedOn;


}
