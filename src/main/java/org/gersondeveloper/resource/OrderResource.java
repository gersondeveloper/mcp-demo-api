package org.gersondeveloper.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gersondeveloper.domain.dto.request.OrderItemRequest;
import org.gersondeveloper.domain.dto.request.OrderRequest;
import org.gersondeveloper.domain.dto.response.OrderItemResponse;
import org.gersondeveloper.domain.dto.response.OrderResponse;
import org.gersondeveloper.domain.model.Order;
import org.gersondeveloper.domain.model.OrderItem;
import org.gersondeveloper.domain.model.Product;
import org.gersondeveloper.domain.model.User;

import java.math.BigDecimal;
import java.util.List;

@Path("/api/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @GET
    public List<OrderResponse> listActive() {
        return Order.<Order>list("isActive", true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        Order order = Order.findById(id);
        if (order == null || !order.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toResponse(order)).build();
    }

    @POST
    @Transactional
    public Response create(OrderRequest request) {
        User user = User.findById(request.userId());
        if (user == null || !user.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        Order order = new Order();
        order.user = user;

        BigDecimal total = BigDecimal.ZERO;
        for (OrderItemRequest itemReq : request.items()) {
            Product product = Product.findById(itemReq.productId());
            if (product == null || !product.isActive) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            }
            OrderItem item = new OrderItem();
            item.product = product;
            item.quantity = itemReq.quantity();
            item.unitPrice = product.price;
            item.order = order;
            order.items.add(item);
            total = total.add(product.price.multiply(BigDecimal.valueOf(itemReq.quantity())));
        }
        order.totalAmount = total;
        order.persist();
        return Response.status(Response.Status.CREATED).entity(toResponse(order)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, OrderRequest request) {
        Order order = Order.findById(id);
        if (order == null || !order.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        User user = User.findById(request.userId());
        if (user == null || !user.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        order.user = user;
        return Response.ok(toResponse(order)).build();
    }

    OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.items.stream()
                .map(item -> new OrderItemResponse(
                        item.id, item.product.id, item.product.name,
                        item.quantity, item.unitPrice))
                .toList();
        return new OrderResponse(order.id, order.status, order.totalAmount, order.user.id, items);
    }
}
