package ru.yandex.practicum.constants;

public final class ApiConstants {
    // Базовые пути для сервисов
    public static final String BASE_PATH_SHOPPING_CART = "/api/v1/shopping-cart";
    public static final String BASE_PATH_SHOPPING_STORE = "/api/v1/shopping-store";
    public static final String BASE_PATH_WAREHOUSE = "/api/v1/warehouse";

    // Пути для Shopping Cart
    public static final String CART_REMOVE = BASE_PATH_SHOPPING_CART + "/remove";
    public static final String CART_CHANGE_QUANTITY = BASE_PATH_SHOPPING_CART + "/change-quantity";

    // Пути для Warehouse
    public static final String WAREHOUSE_CHECK = BASE_PATH_WAREHOUSE + "/check";
    public static final String WAREHOUSE_ADD = BASE_PATH_WAREHOUSE + "/add";
    public static final String WAREHOUSE_ADDRESS = BASE_PATH_WAREHOUSE + "/address";
    public static final String WAREHOUSE_ASSEMBLY = BASE_PATH_WAREHOUSE + "/assembly";           // ← НОВЫЙ
    public static final String WAREHOUSE_SHIPPED = BASE_PATH_WAREHOUSE + "/shipped";             // ← НОВЫЙ
    public static final String WAREHOUSE_RETURN = BASE_PATH_WAREHOUSE + "/return";               // ← НОВЫЙ

    // Пути для Shopping Store
    public static final String STORE_REMOVE_PRODUCT = BASE_PATH_SHOPPING_STORE + "/removeProductFromStore";
    public static final String STORE_QUANTITY_STATE = BASE_PATH_SHOPPING_STORE + "/quantityState";
    public static final String STORE_PRODUCT_BY_ID = BASE_PATH_SHOPPING_STORE + "/{productId}";

    // Пути для Order
    public static final String ORDER_UPDATE_STATE = "/api/v1/order/{orderId}/state";

    // Параметры запросов
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_CATEGORY = "category";
    public static final String PARAM_PRODUCT_ID = "productId";

    private ApiConstants() {
        // Приватный конструктор для предотвращения создания экземпляров
    }
}
