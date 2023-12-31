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
@Table(name = "AllocationType")
public class AllocationType {
    @Id
    @Column(name = "ALLOC_TYPE_ID", nullable = false)
    private String allocTypeId;

    @Column(name = "ALLOC_TYPE")
    private String allocType;

    @Column(name = "ALLOC_DESC")
    private String allocDesc;


    @Column(name = "IS_FLAG")
    private String isFlag;

    @Column(name = "REMARKS")
    private String remarks;

    @Column(name = "FIN_YEAR")
    private String finYear;

    @Column(name = "MAJOR_MINER_HEAD")
    private String majorMinerHead;

    @Column(name = "SUB_HEAD_TYPE")
    private String subHeadType;


    @Column(name = "CREATED_ON")
    private Timestamp createdOn;

    @Column(name = "UPDATED_ON", nullable = false)
    private Timestamp updatedOn;



}
