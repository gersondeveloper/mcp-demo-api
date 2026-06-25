package org.gersondeveloper.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product extends BaseEntity {

    @Column(nullable = false)
    public String name;

    public String description;

    @Column(nullable = false)
    public BigDecimal price;

    @Column(nullable = false)
    public int stockQuantity;
}
