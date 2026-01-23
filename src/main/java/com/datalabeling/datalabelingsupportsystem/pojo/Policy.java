package com.datalabeling.datalabelingsupportsystem.pojo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long policyId;

    @Column(name = "error_name", nullable = false, unique = true)
    private String errorName;

    @Column(columnDefinition = "TEXT")
    private String description;
}
