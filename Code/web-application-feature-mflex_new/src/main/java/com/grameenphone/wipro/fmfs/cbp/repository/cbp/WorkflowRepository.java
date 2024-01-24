package com.grameenphone.wipro.fmfs.cbp.repository.cbp;

import com.grameenphone.wipro.fmfs.cbp.enums.WorkflowHops;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.Action;
import com.grameenphone.wipro.fmfs.cbp.model.orm.cbp.WorkflowHop;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends CrudRepository<WorkflowHop, Long> {
    List<WorkflowHop> findByCodeAndClientIdOrderByOrder(WorkflowHops code, long clientId);
    
    @Query("from WorkflowHop where code = ?1 and (clientId is null or clientId = ?2)")
    List<WorkflowHop> getAllPaymentApprovalHops(WorkflowHops code, long clientId);
    
    @Query("select requiredAction from WorkflowHop where autoExecution = false and (clientId is null or clientId = ?1)")
    List<Action> getManualExecutableActions(long clientId);
}