package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.model.Product;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ProductMapper {
    ProductDto toDto(Product product);

    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "productState", ignore = true)
    Product toEntityForCreate(ProductDto productDto);

}