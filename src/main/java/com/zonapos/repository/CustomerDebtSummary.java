package com.zonapos.repository;

public interface CustomerDebtSummary {
    Long getCustomerId();
    Double getTotalDebt();
    Long getUnpaidCount();
}
