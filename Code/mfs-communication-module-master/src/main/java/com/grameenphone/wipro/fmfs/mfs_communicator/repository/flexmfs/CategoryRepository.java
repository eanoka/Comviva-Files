package com.grameenphone.wipro.fmfs.mfs_communicator.repository.flexmfs;

import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.Category;
import com.grameenphone.wipro.fmfs.mfs_communicator.model.orm.flexmfs.Company;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.stereotype.Repository;

import javax.persistence.QueryHint;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    @QueryHints(@QueryHint(name = "org.hibernate.cacheable", value = "true"))
    Category findCategoryByCategoryCode(String code);
}