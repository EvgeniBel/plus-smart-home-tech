package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.ShoppingStoreClient;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.enums.PaymentStatus;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.mapper.PaymentMapper;
import ru.yandex.practicum.model.Payment;
import ru.yandex.practicum.repository.PaymentRepository;

import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private static final double VAT_RATE = 0.10;
    private static final double DELIVERY_PRICE = 50.0;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final ShoppingStoreClient shoppingStoreClient;

    public double calculateProductCost(OrderDto order) {
        log.info("Расчёт стоимости товаров для заказа: {}", order.getOrderId());
        validateOrder(order);

        double totalProductCost = 0.0;

        for (Map.Entry<UUID, Long> entry : order.getProducts().entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            try {
                ProductDto product = shoppingStoreClient.getProduct(productId);
                double productPrice = product.getPrice() != null ? product.getPrice() : 0.0;
                totalProductCost += productPrice * quantity;
                log.debug("Товар {}: цена={}, количество={}, сумма={}",
                        productId, productPrice, quantity, productPrice * quantity);
            } catch (Exception e) {
                log.error("Ошибка при получении товара {}: {}", productId, e.getMessage());
                throw new NotEnoughInfoInOrderToCalculateException
                        (String.format("Не удалось получить информацию о товаре: %s", productId));
            }
        }

        log.info("Общая стоимость товаров: {}", totalProductCost);
        return totalProductCost;
    }

    public double calculateTotalCost(OrderDto order) {
        log.info("Расчёт полной стоимости заказа: {}", order.getOrderId());
        double productCost = calculateProductCost(order);
        double vat = productCost * VAT_RATE;
        double total = productCost + vat + DELIVERY_PRICE;
        log.info("Полная стоимость: товары={}, НДС={}, доставка={}, итого={}",
                productCost, vat, DELIVERY_PRICE, total);
        return total;
    }

    @Transactional
    public PaymentDto createPayment(OrderDto order) {
        log.info("Создание оплаты для заказа: {}", order.getOrderId());
        validateOrder(order);

        paymentRepository.findByOrderId(order.getOrderId())
                .ifPresent(p -> {
                    throw new IllegalArgumentException(String.format("Оплата для заказа уже существует: %s", order.getOrderId()));
                });

        double productCost = calculateProductCost(order);
        double vat = productCost * VAT_RATE;
        double totalPayment = productCost + vat + DELIVERY_PRICE;

        Payment payment = Payment.builder()
                .orderId(order.getOrderId())
                .productTotal(productCost)
                .deliveryTotal(DELIVERY_PRICE)
                .feeTotal(vat)
                .totalPayment(totalPayment)
                .status(PaymentStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Оплата создана: {}, статус: {}", savedPayment.getPaymentId(), savedPayment.getStatus());

        return paymentMapper.toDto(savedPayment);
    }

    @Transactional
    public void processSuccessfulPayment(UUID paymentId) {
        log.info("Обработка успешной оплаты: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundException(String.format("Платёж не найден: %s", paymentId)));

        payment.setStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        log.info("Статус оплаты обновлён на SUCCESS: {}", paymentId);
    }

    @Transactional
    public void processFailedPayment(UUID paymentId) {
        log.info("Обработка отказа в оплате: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new NoOrderFoundException(String.format("Платёж не найден: %s", paymentId)));

        payment.setStatus(PaymentStatus.FAILED);
        paymentRepository.save(payment);

        log.info("Статус оплаты обновлён на FAILED: {}", paymentId);
    }

    private void validateOrder(OrderDto order) {
        if (order == null) {
            throw new IllegalArgumentException("Заказ не может быть null");
        }
        if (order.getOrderId() == null) {
            throw new IllegalArgumentException("ID заказа не может быть null");
        }
        if (order.getProducts() == null || order.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Заказ не содержит товаров для расчёта"
            );
        }
    }
}