package com.datalabeling.datalabelingsupportsystem.repository.DataSet;

import com.datalabeling.datalabelingsupportsystem.pojo.DataItem;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.projection.DatasetItemCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataItemRepository extends JpaRepository<DataItem, Long> {
    List<DataItem> findByDataset_DatasetIdAndIsActiveTrue(Long datasetId);

    long countByDataset_DatasetIdAndIsActiveTrue(Long datasetId);

    List<DataItem> findByDataset_DatasetId(Long datasetDatasetId);

    @Query("""
            select di.dataset.datasetId as datasetId, count(di) as total
            from DataItem di
            where di.isActive = true and di.dataset.datasetId in :datasetIds
            group by di.dataset.datasetId
            """)
    List<DatasetItemCountProjection> countActiveItemsByDatasetIds(@Param("datasetIds") List<Long> datasetIds);

}
