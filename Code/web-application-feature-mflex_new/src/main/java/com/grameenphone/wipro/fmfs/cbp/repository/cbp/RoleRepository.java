package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Role;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends CrudRepository<Role, Long> {}