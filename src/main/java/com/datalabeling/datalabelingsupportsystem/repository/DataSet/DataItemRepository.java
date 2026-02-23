package com.datalabeling.datalabelingsupportsystem.repository.DataSet;

import com.datalabeling.datalabelingsupportsystem.pojo.DataItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataItemRepository extends JpaRepository<DataItem, Long> {
    List<DataItem> findByDataset_DatasetIdAndIsActiveTrue(Long datasetId);

    long countByDataset_DatasetIdAndIsActiveTrue(Long datasetId);
}
