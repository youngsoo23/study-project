package com.young.studyproject.payment.presentation;

import com.young.studyproject.payment.application.PaymentService;
import com.young.studyproject.payment.application.dto.PaymentCreateCommand;
import com.young.studyproject.payment.application.dto.PaymentResult;
import com.young.studyproject.payment.presentation.dto.PaymentCreateRequest;
import com.young.studyproject.payment.presentation.dto.PaymentResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentCreateRequest request) {
        PaymentResult result = paymentService.create(new PaymentCreateCommand(request.orderId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(result));
    }

    @GetMapping("/{id}")
    public PaymentResponse getById(@PathVariable Long id) {
        return PaymentResponse.from(paymentService.getById(id));
    }

    @PostMapping("/{id}/document-failure")
    public PaymentResponse markDocumentFailure(@PathVariable Long id) {
        return PaymentResponse.from(paymentService.markDocumentFailure(id));
    }

    @PostMapping("/{id}/complete")
    public PaymentResponse complete(@PathVariable Long id) {
        return PaymentResponse.from(paymentService.complete(id));
    }
}
