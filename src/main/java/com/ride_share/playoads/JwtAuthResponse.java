package com.ride_share.playoads;

import lombok.Data;

@Data
public class JwtAuthResponse {

private String token;
	
	private UserDto user;
}
