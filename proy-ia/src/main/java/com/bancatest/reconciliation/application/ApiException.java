package com.bancatest.reconciliation.application;

public class ApiException extends RuntimeException {

    private final int status;
    private final String type;
    private final String title;

    public ApiException(int status, String type, String title, String detail) {
        super(detail);
        this.status = status;
        this.type = type;
        this.title = title;
    }

    public int status() {
        return status;
    }

    public String type() {
        return type;
    }

    public String title() {
        return title;
    }

    public static ApiException notFound(String detail) {
        return new ApiException(404, "https://example.com/problems/reconciliation-not-found",
                "Conciliación no encontrada", detail);
    }

    public static ApiException paymentNotFound() {
        return new ApiException(404, "https://example.com/problems/payment-not-found",
                "Pago no encontrado", "No existe un pago previo para la referencia de operación indicada.");
    }
}
