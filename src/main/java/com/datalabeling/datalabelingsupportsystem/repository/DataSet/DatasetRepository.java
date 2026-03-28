package com.datalabeling.datalabelingsupportsystem.repository.DataSet;

import com.datalabeling.datalabelingsupportsystem.pojo.Dataset;
import com.datalabeling.datalabelingsupportsystem.repository.DataSet.projection.ProjectDatasetCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DatasetRepository extends JpaRepository<Dataset, Long> {
    List<Dataset> findByProject_ProjectId(Long projectId);

    @Query("""
            select d.project.projectId as projectId, count(d) as total
            from Dataset d
            where d.project.projectId in :projectIds
            group by d.project.projectId
            """)
    List<ProjectDatasetCountProjection> countByProjectIds(@Param("projectIds") List<Long> projectIds);
}
