package com.ride_share.service.impl;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ride_share.config.AppConstants;
import com.ride_share.entities.Category;
import com.ride_share.entities.Rider;
import com.ride_share.entities.Role;
import com.ride_share.entities.User;
import com.ride_share.entities.Vehicle;
import com.ride_share.exceptions.ApiException;
//import com.ride_share.entities.V;
import com.ride_share.exceptions.ResourceNotFoundException;
import com.ride_share.playoads.CategoryDto;
import com.ride_share.playoads.RiderDto;
import com.ride_share.playoads.VehicleDto;
import com.ride_share.repositories.CategoryRepo;
//import com.ride_share.playoads.VDto;
import com.ride_share.repositories.RiderRepo;
import com.ride_share.repositories.RoleRepo;
import com.ride_share.repositories.UserRepo;
import com.ride_share.repositories.VehicleRepo;
import com.ride_share.service.RiderService;

@Service
public class RiderServiceImpl implements RiderService{

	@Autowired
	private ModelMapper modelMapper;
	@Autowired
	private RiderRepo riderRepo;
	
	@Autowired
	private CategoryRepo categoryRepo;
	@Autowired
	private UserRepo userRepo;
	
	@Autowired
	private RoleRepo roleRepo;
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private VehicleRepo vehicleRepo;
	// Create a new Rider
    @Override
    public RiderDto createRider(RiderDto riderDto, Integer userId,Integer categoryId) {
        User user = this.userRepo.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "User ID", userId));

        Category category = this.categoryRepo.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "category id ", categoryId));

        Rider.RiderStatus existingStatus = this.riderRepo.findRiderStatusByUserId(userId);
        if (existingStatus == Rider.RiderStatus.PENDING) {
            throw new ApiException("Cannot create rider. User already has a rider application in PENDING status.");
        }

        Rider rider = this.modelMapper.map(riderDto, Rider.class);
        rider.setDriver_License(riderDto.getDriver_License());
        rider.setDate_Of_Birth(riderDto.getDate_Of_Birth());
        rider.setAddedDate(LocalDateTime.now());
        rider.setSelfieWithIdCard("");
        rider.setUser(user);
        rider.setCategory(category);

        // Set status to PENDING only if no prior application or rejected
        rider.setStatus(Rider.RiderStatus.PENDING);

        Rider savedRider = this.riderRepo.save(rider);
        return this.modelMapper.map(savedRider, RiderDto.class);
    }

    // Update an existing Rider
    @Override
    public RiderDto updateRider(RiderDto riderDto, Integer riderId) {
        Rider rider = this.riderRepo.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", "Rider ID", riderId));

        // Prevent updates if the application is already approved
        if (rider.getStatus() == Rider.RiderStatus.APPROVED) {
            throw new ApiException("The application is already APPROVED.");
        }
            rider.setDate_Of_Birth(riderDto.getDate_Of_Birth());
        
        // Update other fields
        rider.setDriver_License(riderDto.getDriver_License());
        rider.setSelfieWithIdCard(riderDto.getSelfieWithIdCard());
        rider.setUpdatedDate(LocalDateTime.now());

        // After update, set status back to PENDING for admin review
        rider.setStatus(Rider.RiderStatus.PENDING);
       // rider.setCategory(riderDto.getCategory());

        Rider updatedRider = this.riderRepo.save(rider);
        return this.modelMapper.map(updatedRider, RiderDto.class);
    }

    // Approve Rider Application
    @Override
    public RiderDto approveRider(Integer riderId) {
        Rider rider = this.riderRepo.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", "Rider ID", riderId));

        User user = this.userRepo.findById(rider.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "User Id", rider.getUser().getId()));
        // Only allow approval if status is PENDING
        if (rider.getStatus() != Rider.RiderStatus.PENDING) {
            throw new ApiException("Cannot approve rider. The application is not in PENDING status.");
        }

        rider.setStatus(Rider.RiderStatus.APPROVED);
        rider.setUpdatedDate(LocalDateTime.now());

        Role role = this.roleRepo.findById(AppConstants.RIDER_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "ID", AppConstants.RIDER_USER));

        rider.getUser().getRoles().clear();
        rider.getUser().getRoles().add(role);

        rider.setBalance(6000.0); //

        String welcomeMessage = String.format(
            "Welcome, %s! We're delighted to have you at Tuffan Ride-Share. "
            + "A balance of 6000 has been added to your account. Enjoy your journey with us. Thank you!\r\n",
            user.getName()
        );

        emailService.sendOtpMobile(user.getMobileNo(), welcomeMessage);

        Rider approvedRider = this.riderRepo.save(rider);
        return this.modelMapper.map(approvedRider, RiderDto.class);
    }

//    Role role = this.roleRepo.findById(AppConstants.NORMAL_USER).get();
//    user.getRoles().add(role);		   
    @Override
    public RiderDto addBalanceOfRider(RiderDto riderDto, Integer riderId) {
        // Find the rider by ID or throw an exception if not found
        Rider rider = this.riderRepo.findById(riderId)
            .orElseThrow(() -> new ResourceNotFoundException("Rider", "Rider ID", riderId));
        
        // Get the current balance and requested balance
        double currentBalance = rider.getBalance();
        double reqBalance = riderDto.getBalance();
        
        // Add the requested balance to the current balance
        double newBalance = currentBalance + reqBalance;
        
        // Update the rider's balance
        rider.setBalance(newBalance);
        riderRepo.save(rider);
        
        // Update the RiderDto with the new balance
        riderDto.setBalance(newBalance);
        
        return riderDto;
    }


    // Reject Rider Application
    @Override
    public RiderDto rejectRider(RiderDto riderDto,Integer riderId) {
        Rider rider = this.riderRepo.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", "Rider ID", riderId));
        User user = this.userRepo.findById(rider.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "User Id", rider.getUser().getId()));
        // Only allow rejection if status is PENDING
        if (rider.getStatus() != Rider.RiderStatus.PENDING) {
            throw new ApiException("Cannot reject rider. The application is not in PENDING status.");
        }
        Role role = this.roleRepo.findById(AppConstants.NORMAL_USER)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "ID", AppConstants.RIDER_USER));

        rider.getUser().getRoles().clear();
        rider.getUser().getRoles().add(role);
        rider.setStatus(Rider.RiderStatus.REJECTED);
        if (riderDto.getStatusMessage() == null) {
            throw new ApiException("Reason for rejection is required.");
        }

        rider.setStatusMessage(riderDto.getStatusMessage());
        rider.setUpdatedDate(LocalDateTime.now());

        // Proper Message Format
        String msg = String.format(
            "Hello Mr %s, your request has been rejected for this reason: \"%s\"",
            user.getName(),
            riderDto.getStatusMessage()
        );

        emailService.sendOtpMobile(user.getMobileNo(), msg);

        Rider rejectedRider = this.riderRepo.save(rider);
        return this.modelMapper.map(rejectedRider, RiderDto.class);
    }



	@Override
	public void deleteRider(Integer riderId) {
		 Rider rider = this.riderRepo.findById(riderId)
	                .orElseThrow(() -> new ResourceNotFoundException("Rider ", "rider id", riderId));

	        this.riderRepo.delete(rider);
		
	}

	@Override
	public RiderDto getRiderById(Integer riderId) {
		Rider rider = this.riderRepo.findById(riderId)
                .orElseThrow(() -> new ResourceNotFoundException("Rider", "rider id", riderId));
        return this.modelMapper.map(rider, RiderDto.class);
	}

	@Override
	public List<RiderDto> getRidersByUser(Integer userId) {
		 User user = this.userRepo.findById(userId)
	                .orElseThrow(() -> new ResourceNotFoundException("User ", "userId ", userId));
	        List<Rider> posts = this.riderRepo.findByUser(user);

	        List<RiderDto> riderDtos = posts.stream().map((rider) -> this.modelMapper.map(rider, RiderDto.class))
	                .collect(Collectors.toList());

	        return riderDtos;
	}

	
	
	@Override
	public List<RiderDto> getAllRiders() {

		List<Rider> riders = this.riderRepo.findAll();
		List<RiderDto> riderDtos = riders.stream().map(rider -> this.riderToDto(rider)).collect(Collectors.toList());

		return riderDtos;
	}
	
	

//
//	@Override
//	public List<RiderDto> getPendingRiders() {
//	    List<Rider> pendingRiders = riderRepo.findByStatus(Rider.RiderStatus.PENDING);
//	    return pendingRiders.stream()
//	            .map(this::riderToDto)
//	            .collect(Collectors.toList());
//	}
	
	@Override
	public List<RiderDto> getPendingRiders() {
	    List<Rider> pendingRiders = riderRepo.findByStatus(Rider.RiderStatus.PENDING);
	    return pendingRiders.stream()
	            .map(rider -> {
	                RiderDto riderDto = riderToDto(rider); // Rider लाई RiderDto मा बदल
	                
	                vehicleRepo.findByUserId(rider.getUser().getId()).ifPresent(vehicle -> {
	                	VehicleDto vehicleDto = vehicleToDto(vehicle);
	                    riderDto.setVehicle(vehicleDto);
	                });
	                
	               
	                return riderDto;
	            })
	            .collect(Collectors.toList());
	}

	private VehicleDto vehicleToDto(Vehicle vehicle) {
	    VehicleDto dto = new VehicleDto();
	    dto.setId(vehicle.getId());
	  dto.setBillBook1(vehicle.getBillBook1());
	  dto.setBillBook2(vehicle.getBillBook2());
	  dto.setProductionYear(vehicle.getProductionYear());
	  dto.setVechicleImg(vehicle.getVechicleImg());
	  dto.setVehicleNumber(vehicle.getVehicleNumber());
	  dto.setVehicleType(vehicle.getVehicleType());
	  dto.setVehicleBrand(vehicle.getVehicleBrand());
	// **Entity to DTO change here**
	    if (vehicle.getCategory() != null) {
	        dto.setCategory(categoryToDto(vehicle.getCategory()));
	    }
	    return dto;
	}

	private CategoryDto categoryToDto(Category category) {
	    CategoryDto dto = new CategoryDto();
	    dto.setCategoryId(category.getCategoryId());
	    dto.setCategoryTitle(category.getCategoryTitle());
	    return dto;
	}


	public Rider dtoToRider(RiderDto riderDto) {
		Rider rider = this.modelMapper.map(riderDto, Rider.class);

		return rider;
	}

	public RiderDto riderToDto(Rider rider) {
		RiderDto riderDto = this.modelMapper.map(rider, RiderDto.class);
		return riderDto;
	}

}
