package com.ride_share.entities;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;

import lombok.Data;

import lombok.NoArgsConstructor;


import javax.persistence.*;




@Entity
@Data
@NoArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String vehicleType;
    private String vehicleBrand;
    private String vehicleNumber;
    private String productionYear;

    
	private String vechicleImg; 
	private String billBook1;
	private String billBook2; 
	
    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
