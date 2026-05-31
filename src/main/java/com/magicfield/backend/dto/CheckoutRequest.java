package com.magicfield.backend.dto;

import java.util.List;

public class CheckoutRequest {

    private String customerName;
    private String customerLastName;
    private String customerPhone;
    private String customerEmail;
    private String userId; // ID del usuario si está logueado, null si no

    private String deliveryType; // RETIRO_RAMOS, RETIRO_FRANCISCO, ENVIO_DOMICILIO, ENVIO_ANDREANI
    private String customerDni;
    private String shippingStreet;
    private String shippingStreetNumber;
    private String shippingCity;
    private String shippingProvince;
    private String shippingPostalCode;
    private String paymentMethod; // TRANSFERENCIA, EFECTIVO

    private List<CheckoutItemRequest> items;

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerLastName() { return customerLastName; }
    public void setCustomerLastName(String customerLastName) { this.customerLastName = customerLastName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDeliveryType() { return deliveryType; }
    public void setDeliveryType(String deliveryType) { this.deliveryType = deliveryType; }

    public String getCustomerDni() { return customerDni; }
    public void setCustomerDni(String customerDni) { this.customerDni = customerDni; }

    public String getShippingStreet() { return shippingStreet; }
    public void setShippingStreet(String shippingStreet) { this.shippingStreet = shippingStreet; }

    public String getShippingStreetNumber() { return shippingStreetNumber; }
    public void setShippingStreetNumber(String shippingStreetNumber) { this.shippingStreetNumber = shippingStreetNumber; }

    public String getShippingCity() { return shippingCity; }
    public void setShippingCity(String shippingCity) { this.shippingCity = shippingCity; }

    public String getShippingProvince() { return shippingProvince; }
    public void setShippingProvince(String shippingProvince) { this.shippingProvince = shippingProvince; }

    public String getShippingPostalCode() { return shippingPostalCode; }
    public void setShippingPostalCode(String shippingPostalCode) { this.shippingPostalCode = shippingPostalCode; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public List<CheckoutItemRequest> getItems() { return items; }
    public void setItems(List<CheckoutItemRequest> items) { this.items = items; }
}
