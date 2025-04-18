package com.ride_share.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ride_share.entities.RideRequest;
import com.ride_share.entities.RiderApprovalRequest;
import com.ride_share.entities.User;

public interface RiderApprovalRequestRepo extends JpaRepository<RiderApprovalRequest, Integer> {

    Optional<RiderApprovalRequest> findByRideRequestRideRequestIdAndUserId(Integer rideRequestId, Integer userId);

    Optional<RiderApprovalRequest> findByUserAndRideRequest(User user, RideRequest rideRequest);
}


