package org.gersondeveloper.domain.model;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;

@MappedSuperclass
public abstract class BaseEntity extends PanacheEntity {

    @Column(nullable = false, updatable = false)
    public LocalDateTime createDate;

    @Column(nullable = false)
    public LocalDateTime modificationDate;

    @Column(nullable = false)
    public boolean isActive = true;

    @PrePersist
    void onCreate() {
        createDate = LocalDateTime.now();
        modificationDate = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        modificationDate = LocalDateTime.now();
    }
}
