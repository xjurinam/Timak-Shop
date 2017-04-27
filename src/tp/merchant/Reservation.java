/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.merchant;

import java.util.Date;

/**
 *
 * @author marek
 */
public class Reservation {
    
    private int orderId;
    private Product product;
    private int productId;
    private int amount;
    private int state;
    private String reservationUuid;
    private Date reservationDate;

    public Reservation() {
        this.reservationDate = new Date();
        this.state = -1;
    }

    public Reservation(int orderId, int productId, int amount, String reservationUuid) {
        this();
        this.orderId = orderId;
        this.productId = productId;
        this.amount = amount;
        this.reservationUuid = reservationUuid;
    }

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getReservationUuid() {
        return reservationUuid;
    }

    public void setReservationUuid(String reservationUuid) {
        this.reservationUuid = reservationUuid;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public Date getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(Date reservationDate) {
        this.reservationDate = reservationDate;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }
    
}
