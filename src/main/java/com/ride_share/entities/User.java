package com.ride_share.entities;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;


import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;


import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class User implements UserDetails{

	     @Id
	    @GeneratedValue(strategy = GenerationType.AUTO)
	    private int id;

	    @Column(name = "name", nullable = false, length = 100)
	    private String name;

	    @Column(unique = true)
	    private String email;
       
	    @Column(nullable=false,length=100)
	    private String mobileNo;
	    
	    private String password;

	    private String imageName;
	    
	    private String balance;

	    @Enumerated(EnumType.STRING)
	    private UserMode modes;
	    
	    public enum UserMode {
	        RIDER,PESSENGER
	    }
        //--------------------------------
        
        @Column(name = "date_of_registration")
        private LocalDateTime dateOfRegistration;

        private LocalDateTime date_Of_Role_Changed;
        
        private LocalDateTime otpValidUntil;
        private String otp;
        
	    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
		@JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user", referencedColumnName = "id"), inverseJoinColumns = @JoinColumn(name = "role", referencedColumnName = "id"))
		private Set<Role> roles = new HashSet<>();
	    
	    //user lai [http://localhost:8085/api/v1/auth/branch] yo dine ui side ma
	    private String branch_Name;
	    
	    
	    
	    private String mode; // PASSENGER or DRIVER
        
	   private String date_of_Birth;
	   
//	    @ManyToOne
//	    @JoinColumn(name = "branch_id")
//	    private Branch branch; // Association with branch.
	    
	    
	    
		@Override
		public Collection<? extends GrantedAuthority> getAuthorities() {

			List<SimpleGrantedAuthority> authories = this.roles.stream()
					.map((role) -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
			return authories;
		}

		@Override
		public String getUsername() {
			// TODO Auto-generated method stub
			return this.mobileNo;
		}

		@Override
		public boolean isAccountNonExpired() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean isAccountNonLocked() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean isCredentialsNonExpired() {
			// TODO Auto-generated method stub
			return true;
		}

		@Override
		public boolean isEnabled() {
			// TODO Auto-generated method stub
			return true;
		}

		
	
}
