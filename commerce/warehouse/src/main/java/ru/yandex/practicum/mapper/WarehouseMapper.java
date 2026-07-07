package ru.yandex.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.DimensionDto;
import ru.yandex.practicum.dto.NewProductInWarehouseRequest;
import ru.yandex.practicum.model.WarehouseProduct;

@Component
public class WarehouseMapper {

    public WarehouseProduct toEntity(NewProductInWarehouseRequest request) {
        if (request == null) return null;

        return WarehouseProduct.builder()
                .productId(request.getProductId())
                .name(request.getName())
                .description(request.getDescription())
                .weight(request.getWeight())
                .fragile(request.getFragile() != null && request.getFragile())
                .quantity(0)
                .build();
    }

    public void updateDimensions(WarehouseProduct product, DimensionDto dimension) {
        if (product != null && dimension != null) {
            product.setWidth(dimension.getWidth());
            product.setHeight(dimension.getHeight());
            product.setDepth(dimension.getDepth());
        }
    }
}