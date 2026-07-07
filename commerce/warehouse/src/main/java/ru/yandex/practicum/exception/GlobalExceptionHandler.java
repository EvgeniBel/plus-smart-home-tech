package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SpecifiedProductAlreadyInWarehouseException.class)
    public ResponseEntity<Map<String, String>> handleSpecifiedProductAlreadyInWarehouse(
            SpecifiedProductAlreadyInWarehouseException ex) {
        log.error("Product already in warehouse: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Product already exists");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouse.class)
    public ResponseEntity<Map<String, Object>> handleProductInShoppingCartLowQuantity(
            ProductInShoppingCartLowQuantityInWarehouse ex) {
        log.error("Insufficient quantity in warehouse: {}", ex.getMessage());
        Map<String, Object> error = new HashMap<>();
        error.put("error", "Insufficient quantity");
        error.put("message", ex.getMessage());
        error.put("unavailableProducts", ex.getUnavailableProducts());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(NoSpecifiedProductInWarehouseException.class)
    public ResponseEntity<Map<String, String>> handleNoSpecifiedProductInWarehouse(
            NoSpecifiedProductInWarehouseException ex) {
        log.error("Product not found in warehouse: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Product not found");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Illegal argument: {}", ex.getMessage());
        Map<String, String> error = new HashMap<>();
        error.put("error", "Bad request");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        Map<String, String> error = new HashMap<>();
        error.put("error", "Internal server error");
        error.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}