package com.magicfield.backend.dto;

import java.util.List;

public class CheckoutRequest {

    private String customerName;
    private String customerLastName;
    private String customerPhone;
    private String customerEmail;
    private String userId; // ID del usuario si está logueado, null si no

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

    public List<CheckoutItemRequest> getItems() { return items; }
    public void setItems(List<CheckoutItemRequest> items) { this.items = items; }
}
