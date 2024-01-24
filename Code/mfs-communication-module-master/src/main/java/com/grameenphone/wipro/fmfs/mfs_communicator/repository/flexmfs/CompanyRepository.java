package com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.Company;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Integer> {
    @EntityGraph(attributePaths = {"category"})
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Company findCompanyByCompanyCode(String code);
}