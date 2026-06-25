package org.gersondeveloper.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.gersondeveloper.domain.dto.request.ProductRequest;
import org.gersondeveloper.domain.dto.response.ProductResponse;
import org.gersondeveloper.domain.model.Product;

import java.util.List;

@Path("/api/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductResource {

    @GET
    public List<ProductResponse> listActive() {
        return Product.<Product>list("isActive", true)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") Long id) {
        Product product = Product.findById(id);
        if (product == null || !product.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(toResponse(product)).build();
    }

    @POST
    @Transactional
    public Response create(ProductRequest request) {
        Product product = new Product();
        product.name = request.name();
        product.description = request.description();
        product.price = request.price();
        product.stockQuantity = request.stockQuantity();
        product.persist();
        return Response.status(Response.Status.CREATED).entity(toResponse(product)).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, ProductRequest request) {
        Product product = Product.findById(id);
        if (product == null || !product.isActive) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        product.name = request.name();
        product.description = request.description();
        product.price = request.price();
        product.stockQuantity = request.stockQuantity();
        return Response.ok(toResponse(product)).build();
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id, product.name, product.description,
                product.price, product.stockQuantity, product.isActive);
    }
}
