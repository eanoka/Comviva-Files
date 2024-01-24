package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import org.springframework.data.repository.CrudRepository;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.ClientConfig;

public interface ClientConfigRepository extends CrudRepository<ClientConfig, Long> {
	ClientConfig findByClientIdAndKey(Long clientId, String key);
}