package rw.ac.rca.hoabs.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.ac.rca.hoabs.common.exception.ApiException;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentGatewayStub gatewayStub;

    public PaymentService(PaymentRepository paymentRepository, PaymentGatewayStub gatewayStub) {
        this.paymentRepository = paymentRepository;
        this.gatewayStub = gatewayStub;
    }

    @Transactional
    public Payment processBookingFee(UUID appointmentId, long amountRwf, String provider, String stubModeOverride) {
        PaymentGatewayStub.PaymentResult result = gatewayStub.charge(provider, amountRwf, stubModeOverride);

        Payment payment = new Payment();
        payment.setAppointmentId(appointmentId);
        payment.setAmountRwf(amountRwf);
        payment.setProvider(provider != null ? provider : "MTN_MOMO");
        payment.setStatus(result.status());
        payment.setGatewayRef(result.gatewayRef());

        paymentRepository.save(payment);

        if (result.status() == PaymentStatus.DECLINED) {
            throw ApiException.badRequest("Payment declined: " + result.message());
        }
        if (result.status() == PaymentStatus.TIMED_OUT) {
            throw ApiException.conflict("Payment timed out: " + result.message() + " No appointment was created and no charge was captured.");
        }

        return payment;
    }
}
