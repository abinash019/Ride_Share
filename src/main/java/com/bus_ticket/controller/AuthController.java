package com.bus_ticket.controller;

import java.security.Principal;

import javax.validation.Valid;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bus_ticket.entities.OtpRequest;
import com.bus_ticket.entities.User;
import com.bus_ticket.exceptions.ApiException;
import com.bus_ticket.playoads.ForgetPasswordDto;
import com.bus_ticket.playoads.JwtAuthRequest;
import com.bus_ticket.playoads.JwtAuthResponse;
import com.bus_ticket.playoads.UserDto;
import com.bus_ticket.repositories.UserRepo;
import com.bus_ticket.security.JwtTokenHelper;
import com.bus_ticket.service.ForgetPasswordService;
import com.bus_ticket.service.OtpRequestService;
import com.bus_ticket.service.UserService;
import com.bus_ticket.service.impl.RateLimitingService;

import com.bus_ticket.entities.ForgetPassword;





@RestController
@RequestMapping("/api/v1/auth/")
public class AuthController {
	@Autowired
	private JwtTokenHelper jwtTokenHelper;

	@Autowired
	private UserDetailsService userDetailsService;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private UserService userService;
	
	 @Autowired
	   private OtpRequestService otpRequestService;
	  
	 @Autowired
	 private RateLimitingService rateLimitingService;
	 
	 @Autowired
	    private ForgetPasswordService forgetPasswordService;

	@PostMapping("/login")
	public ResponseEntity<JwtAuthResponse> createToken(@RequestBody JwtAuthRequest request) throws Exception {
		this.authenticate(request.getUsername(), request.getPassword());
		UserDetails userDetails = this.userDetailsService.loadUserByUsername(request.getUsername());
		String token = this.jwtTokenHelper.generateToken(userDetails);

		JwtAuthResponse response = new JwtAuthResponse();
		response.setToken(token);
		response.setUser(this.mapper.map((User) userDetails, UserDto.class));
		return new ResponseEntity<JwtAuthResponse>(response, HttpStatus.OK);
	}

	private void authenticate(String username, String password) throws Exception {

		UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username,
				password);

		try {

			this.authenticationManager.authenticate(authenticationToken);

		} catch (BadCredentialsException e) {
			System.out.println("Invalid Detials !!");
			throw new ApiException("Invalid username or password !!");
		}

	}

	// register new user api

	@PostMapping("/register")
	public ResponseEntity<UserDto> registerUser(@Valid @RequestBody UserDto userDto) {
		UserDto registeredUser = this.userService.registerNewUser(userDto);
		return new ResponseEntity<UserDto>(registeredUser, HttpStatus.CREATED);
	}

	// get loggedin user data
	@Autowired
	private UserRepo userRepo;
	@Autowired
	private ModelMapper mapper;

	@GetMapping("/current-user/")
	public ResponseEntity<UserDto> getUser(Principal principal) {
		User user = this.userRepo.findByEmail(principal.getName()).get();
		return new ResponseEntity<UserDto>(this.mapper.map(user, UserDto.class), HttpStatus.OK);
	}
	
	//otp for registration
    @PostMapping("/get-phone-number")
    public ResponseEntity<OtpRequest> createOtp(@RequestBody OtpRequest otpReq) {
    	
    	OtpRequest ph = otpRequestService.createOtp(otpReq);
    	
    			
        return ResponseEntity.ok(ph);
    }
    @PostMapping("/forgetpw")
    public ResponseEntity<ForgetPassword> createForgetPassword(@RequestBody ForgetPasswordDto forgetPasswordDto) {
        ForgetPassword forgetPassword = forgetPasswordService.createForget(forgetPasswordDto);
        rateLimitingService.checkRateLimit("test-api-key");
        return ResponseEntity.ok(forgetPassword);
    }

    @PostMapping("/update-password")
    public ResponseEntity<String> updatePassword(@RequestBody ForgetPassword request) {
        try {
        	forgetPasswordService.updatePassword(request.getPhnum(), request.getOtp(), request.getNewPassword());
            return ResponseEntity.ok("Password updated successfully");
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }

}}
