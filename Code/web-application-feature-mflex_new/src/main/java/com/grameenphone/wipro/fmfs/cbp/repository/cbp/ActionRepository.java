package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import org.springframework.data.repository.CrudRepository;

import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Action;

import java.util.List;

public interface ActionRepository extends CrudRepository<Action, Long> {
    List<Action> findByClientId(long clientId);
    Action findByName(String action);
}