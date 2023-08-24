package com.sdd.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.transaction.Transactional;
import java.sql.Timestamp;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "HrData")
public class HrData {
    @Id
    @Column(name = "pid", nullable = false)
    private String pid;

    @Column(name = "pno")
    private String pno;

    @Column(name = "cadre")
    private String cadre;

    @Column(name = "fullName")
    private String fullName;

    @Column(name = "rank")
    private String rank;

    @Column(name = "dob")
    private String dob;

    @Column(name = "joiningDate")
    private String joiningDate;

    @Column(name = "offEmail")
    private String offEmail;

    @Column(name = "mobileNo")
    private String mobileNo;

    @Column(name = "unit")
    private String unit;

    @Column(name = "unitDate")
    private String unitDate;

    @Column(name = "station")
    private String station;

    @Column(name = "unitId")
    private String unitId;

    @Column(name = "userName")
    private String userName;

    @Column(name = "roleId")
    private String roleId;

    @Column(name = "isActive")
    private String isActive;

    @Column(name = "toDate")
    private String toDate;

    @Column(name = "fromDate")
    private String fromDate;

    @Column(name = "password")
    private String password;

    @Column(name = "CREATED_ON")
    private Timestamp createdOn;

    @Column(name = "UPDATED_ON", nullable = false)
    private Timestamp updatedOn;



}
