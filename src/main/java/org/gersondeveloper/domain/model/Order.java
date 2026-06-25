package org.gersondeveloper.domain.model;

import jakarta.persistence.*;
import org.gersondeveloper.domain.enums.OrderStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false)
    public BigDecimal totalAmount = BigDecimal.ZERO;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    public User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    public List<OrderItem> items = new ArrayList<>();
}
