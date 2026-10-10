package rw.ac.rca.hoabs.payment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import rw.ac.rca.hoabs.config.HoabsProperties;

import java.util.UUID;

@Component
public class PaymentGatewayStub {

    private static final Logger log = LoggerFactory.getLogger(PaymentGatewayStub.class);

    private final HoabsProperties properties;

    public PaymentGatewayStub(HoabsProperties properties) {
        this.properties = properties;
    }

    public record PaymentResult(PaymentStatus status, String gatewayRef, String message) {}

    public PaymentResult charge(String provider, long amountRwf, String overrideMode) {
        String mode = (overrideMode != null && !overrideMode.isBlank())
                ? overrideMode.toUpperCase()
                : (properties.payment() != null && properties.payment().stubMode() != null
                ? properties.payment().stubMode().toUpperCase()
                : "SUCCESS");

        log.info("Processing booking fee payment: provider={}, amount={} RWF, stubMode={}", provider, amountRwf, mode);

        return switch (mode) {
            case "DECLINE" -> new PaymentResult(PaymentStatus.DECLINED, null, "Payment was declined by provider.");
            case "TIMEOUT" -> new PaymentResult(PaymentStatus.TIMED_OUT, null, "Payment gateway request timed out.");
            case "SUCCESS" -> {
                String ref = "MOMO-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                yield new PaymentResult(PaymentStatus.SUCCEEDED, ref, "Payment successful.");
            }
            default -> new PaymentResult(PaymentStatus.SUCCEEDED, "MOMO-DEV", "Payment successful.");
        };
    }
}
