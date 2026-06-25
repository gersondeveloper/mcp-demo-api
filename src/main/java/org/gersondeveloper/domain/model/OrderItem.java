package org.gersondeveloper.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {

    @Column(nullable = false)
    public int quantity;

    @Column(nullable = false)
    public BigDecimal unitPrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    public Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    public Product product;
}
