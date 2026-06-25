package org.gersondeveloper.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gersondeveloper.domain.dto.request.OrderItemRequest;
import org.gersondeveloper.domain.dto.request.StandaloneOrderItemRequest;
import org.gersondeveloper.domain.dto.response.OrderItemResponse;
import org.gersondeveloper.domain.model.Order;
import org.gersondeveloper.domain.model.OrderItem;
import org.gersondeveloper.domain.model.Product;

import java.util.List;

@Path("/api/orderItems")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderItemResource {

    @GET
    @Transactional
    public List<OrderItemResponse> listActive() {
        return OrderItem.<OrderItem>list("isActive", true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    @Transactional
    public Response findById(@PathParam("id") Long id) {
        OrderItem item = OrderItem.findById(id);
        if (item == null || !item.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toResponse(item)).build();
    }

    @POST
    @Transactional
    public Response create(StandaloneOrderItemRequest request) {
        Order order = Order.findById(request.orderId());
        if (order == null || !order.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Product product = Product.findById(request.productId());
        if (product == null || !product.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        OrderItem item = new OrderItem();
        item.order = order;
        item.product = product;
        item.quantity = request.quantity();
        item.unitPrice = product.price;
        item.persist();
        return Response.status(Response.Status.CREATED).entity(toResponse(item)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, OrderItemRequest request) {
        OrderItem item = OrderItem.findById(id);
        if (item == null || !item.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        Product product = Product.findById(request.productId());
        if (product == null || !product.isActive) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        item.product = product;
        item.quantity = request.quantity();
        item.unitPrice = product.price;
        return Response.ok(toResponse(item)).build();
    }

    private OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.id, item.product.id, item.product.name,
                item.quantity, item.unitPrice);
    }
}
