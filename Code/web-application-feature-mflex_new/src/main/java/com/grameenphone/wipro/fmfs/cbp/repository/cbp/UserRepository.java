package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends IUserRepository, JpaRepository<User, Long> {}