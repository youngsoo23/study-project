package com.young.studyproject.example.record.dto;

public record MoneyCalcResponse(Money original, Money added, Money result, boolean originalUnchanged) {
}
