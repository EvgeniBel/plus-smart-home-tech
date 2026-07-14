package ru.yandex.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.model.Payment;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PaymentMapper {

    @Mapping(target = "paymentId", source = "paymentId")
    @Mapping(target = "totalPayment", source = "totalPayment")
    @Mapping(target = "deliveryTotal", source = "deliveryTotal")
    @Mapping(target = "feeTotal", source = "feeTotal")
    PaymentDto toDto(Payment payment);
}