package com.datalabeling.datalabelingsupportsystem.pojo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "labels")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Label {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long labelId;
    
    @Column(nullable = false)
    private String labelName;
    
    @Column(nullable = false)
    private String colorCode;
    
    @Column(nullable = false)
    private String labelType;
    
    private String description;
    
    @Column(length = 1)
    private String shortcutKey;
    
    @Column(nullable = false)
    private Boolean isActive = true;
}
