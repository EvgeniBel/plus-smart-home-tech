package ru.yandex.practicum.model;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.dto.AddressDto;

import java.util.Random;

@Component
public class WarehouseAddress {
    private static final String[] ADDRESSES =
            new String[]{"ADDRESS_1", "ADDRESS_2"};

    private static final String CURRENT_ADDRESS =
            ADDRESSES[new Random().nextInt(ADDRESSES.length)];

    public AddressDto getAddress() {
        return AddressDto.builder()
                .country(CURRENT_ADDRESS)
                .city(CURRENT_ADDRESS)
                .street(CURRENT_ADDRESS)
                .house(CURRENT_ADDRESS)
                .flat(CURRENT_ADDRESS)
                .build();
    }
}