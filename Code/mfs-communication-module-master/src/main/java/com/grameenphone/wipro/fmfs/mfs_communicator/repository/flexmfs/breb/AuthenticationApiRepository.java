package com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs.breb;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.RebAuthenticationApiDetail;

public interface AuthenticationApiRepository extends JpaRepository<RebAuthenticationApiDetail, Integer> {
	
	
	List<RebAuthenticationApiDetail> findAll();  

}
