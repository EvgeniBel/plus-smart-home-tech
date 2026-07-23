package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.model.Delivery;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DeliveryMapper {

    @Mapping(target = "deliveryId", source = "deliveryId")
    @Mapping(target = "fromAddress", expression = "java(mapFromAddress(delivery))")
    @Mapping(target = "toAddress", expression = "java(mapToAddress(delivery))")
    @Mapping(target = "deliveryState", source = "deliveryState")
    DeliveryDto toDto(Delivery delivery);

    @Mapping(target = "fromCountry", source = "fromAddress.country")
    @Mapping(target = "fromCity", source = "fromAddress.city")
    @Mapping(target = "fromStreet", source = "fromAddress.street")
    @Mapping(target = "fromHouse", source = "fromAddress.house")
    @Mapping(target = "fromFlat", source = "fromAddress.flat")
    @Mapping(target = "toCountry", source = "toAddress.country")
    @Mapping(target = "toCity", source = "toAddress.city")
    @Mapping(target = "toStreet", source = "toAddress.street")
    @Mapping(target = "toHouse", source = "toAddress.house")
    @Mapping(target = "toFlat", source = "toAddress.flat")
    @Mapping(target = "deliveryState", source = "deliveryState")
    @Mapping(target = "deliveryId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Delivery toEntity(DeliveryDto dto);

    default AddressDto mapFromAddress(Delivery delivery) {
        if (delivery == null) return null;
        return AddressDto.builder()
                .country(delivery.getFromCountry())
                .city(delivery.getFromCity())
                .street(delivery.getFromStreet())
                .house(delivery.getFromHouse())
                .flat(delivery.getFromFlat())
                .build();
    }

    default AddressDto mapToAddress(Delivery delivery) {
        if (delivery == null) return null;
        return AddressDto.builder()
                .country(delivery.getToCountry())
                .city(delivery.getToCity())
                .street(delivery.getToStreet())
                .house(delivery.getToHouse())
                .flat(delivery.getToFlat())
                .build();
    }
}