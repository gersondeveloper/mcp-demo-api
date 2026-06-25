# CRUD Entities & Endpoints — Design Spec
**Date:** 2026-06-25

## Overview

REST API CRUD completo para os domínios User, Product, Order e OrderItem, usando Quarkus 3 com Panache Active Record, Java records como DTOs, e PostgreSQL.

## Stack

- Quarkus 3.36.3, Java 21
- Panache Active Record (`PanacheEntity`)
- JAX-RS via `quarkus-rest` + `quarkus-rest-jackson`
- PostgreSQL (Dev Services em dev/test)

## Package Structure

```
org.gersondeveloper/
├── domain/
│   ├── model/
│   │   ├── BaseEntity.java       ← campos comuns (PanacheEntity)
│   │   ├── User.java
│   │   ├── Product.java
│   │   ├── Order.java
│   │   └── OrderItem.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── UserRequest.java
│   │   │   ├── ProductRequest.java
│   │   │   ├── OrderRequest.java
│   │   │   └── OrderItemRequest.java
│   │   └── response/
│   │       ├── UserResponse.java
│   │       ├── ProductResponse.java
│   │       ├── OrderResponse.java
│   │       └── OrderItemResponse.java
│   └── enums/
│       └── OrderStatus.java
└── resource/
    ├── UserResource.java
    ├── ProductResource.java
    ├── OrderResource.java
    └── OrderItemResource.java
```

## Entities

### BaseEntity (extends PanacheEntity)
| Field | Type | Notes |
|-------|------|-------|
| createDate | LocalDateTime | setado em @PrePersist |
| modificationDate | LocalDateTime | setado em @PrePersist e @PreUpdate |
| isActive | boolean | default true |

### User
| Field | Type | Constraints |
|-------|------|-------------|
| username | String | unique, not null |
| address | String | not null |

### Product
| Field | Type | Constraints |
|-------|------|-------------|
| name | String | not null |
| description | String | nullable |
| price | BigDecimal | not null |
| stockQuantity | int | not null |

### Order
| Field | Type | Constraints |
|-------|------|-------------|
| status | OrderStatus | not null, default PENDING |
| totalAmount | BigDecimal | calculado no POST |
| user | User | ManyToOne, not null |
| items | List<OrderItem> | OneToMany, cascade ALL |

### OrderItem
| Field | Type | Constraints |
|-------|------|-------------|
| quantity | int | not null |
| unitPrice | BigDecimal | not null (copiado do Product no POST) |
| order | Order | ManyToOne |
| product | Product | ManyToOne |

## DTOs (Java Records)

### Requests
```java
record UserRequest(String username, String address)
record ProductRequest(String name, String description, BigDecimal price, int stockQuantity)
record OrderRequest(Long userId, List<OrderItemRequest> items)
record OrderItemRequest(Long productId, int quantity)
```

### Responses
```java
record UserResponse(Long id, String username, String address, boolean isActive, LocalDateTime createDate)
record ProductResponse(Long id, String name, String description, BigDecimal price, int stockQuantity, boolean isActive)
record OrderResponse(Long id, OrderStatus status, BigDecimal totalAmount, Long userId, List<OrderItemResponse> items)
record OrderItemResponse(Long id, Long productId, String productName, int quantity, BigDecimal unitPrice)
```

## Endpoints

| Method | Path | Description | Response |
|--------|------|-------------|----------|
| GET | /api/users | Lista usuários ativos (isActive=true) | 200 List<UserResponse> |
| GET | /api/users/{id} | Busca por ID | 200 UserResponse / 404 |
| POST | /api/users | Cria usuário | 201 UserResponse |
| PUT | /api/users/{id} | Atualiza usuário | 200 UserResponse / 404 |
| GET | /api/products | Lista produtos ativos | 200 List<ProductResponse> |
| GET | /api/products/{id} | Busca por ID | 200 ProductResponse / 404 |
| POST | /api/products | Cria produto | 201 ProductResponse |
| PUT | /api/products/{id} | Atualiza produto | 200 ProductResponse / 404 |
| GET | /api/orders | Lista pedidos ativos | 200 List<OrderResponse> |
| GET | /api/orders/{id} | Busca por ID | 200 OrderResponse / 404 |
| POST | /api/orders | Cria pedido (calcula totalAmount) | 201 OrderResponse |
| PUT | /api/orders/{id} | Atualiza status do pedido | 200 OrderResponse / 404 |
| GET | /api/orderItems | Lista itens ativos | 200 List<OrderItemResponse> |
| GET | /api/orderItems/{id} | Busca por ID | 200 OrderItemResponse / 404 |
| POST | /api/orderItems | Cria item de pedido | 201 OrderItemResponse |
| PUT | /api/orderItems/{id} | Atualiza item | 200 OrderItemResponse / 404 |

## Business Rules

- `GET /api/*` lista apenas registros com `isActive = true`
- `POST /api/orders`: `totalAmount` = soma de (`quantity × product.price`) por item; `unitPrice` do `OrderItem` é copiado do `Product` no momento da criação
- Entidade não é deletada fisicamente — `isActive = false` marca como inativo (soft delete reservado para implementação futura se necessário)

## pom.xml Changes

Adicionar `quarkus-rest-jackson` para serialização JSON.
