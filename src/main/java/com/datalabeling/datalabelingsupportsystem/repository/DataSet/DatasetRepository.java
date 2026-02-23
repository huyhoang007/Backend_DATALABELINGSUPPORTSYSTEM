package com.datalabeling.datalabelingsupportsystem.repository.DataSet;

import com.datalabeling.datalabelingsupportsystem.pojo.Dataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    List<Dataset> findByProject_ProjectId(Long projectId);
}
