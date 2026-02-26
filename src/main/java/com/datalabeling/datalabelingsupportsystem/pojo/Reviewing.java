package com.datalabeling.datalabelingsupportsystem.pojo;

import com.datalabeling.datalabelingsupportsystem.enums.Reviewing.ReviewingStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Reviewing")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Reviewing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reviewing_id")
    private Long reviewingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "annotator_id", nullable = false)
    private User annotator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "label_id", nullable = false)
    private Label label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    private DataItem dataItem;

    // JSON string: [{"x":10,"y":20}, ...]
    @Column(name = "geometry", columnDefinition = "TEXT")
    private String geometry;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private ReviewingStatus status;

    @Column(name = "is_improved")
    private Boolean isImproved;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;
}
