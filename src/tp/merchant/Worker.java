/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.merchant;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.eclipse.paho.client.mqttv3.MqttException;
import tp.message.ConfigMessage;
import tp.message.ProductsMessage;
import tp.message.ReservationMessage;

/**
 *
 * @author marek
 */
public class Worker implements IMqttMerchant{
    private List<Product> products;
    private List<Reservation> reservations;
    private Merchant2 merchant;

    public Worker(Merchant2 merchant, List<Product> products) {
        this.products = products;
        this.merchant = merchant;
        this.reservations = new ArrayList();
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }

    public List<Reservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<Reservation> reservations) {
        this.reservations = reservations;
    }

    public Merchant2 getMerchant() {
        return merchant;
    }

    public void setMerchant(Merchant2 merchant) {
        this.merchant = merchant;
    }
    
    public void sentProductsToPNode(){
        ProductsMessage productsMessage = new ProductsMessage();
        productsMessage.createProductsMessage(this.products, this.merchant.getUuid());
        try {
            merchant.sendMessage(IMqttMerchant.sendProductsTopic, productsMessage.getJson(), false);
            System.out.println("Sending products to PNode ... [OK]");
        } catch (MqttException ex) {
            System.out.println("Sending products to PNode ... [FAIL]");
            System.err.printf(ex.toString());
        }
    }
    
    public void sentCongfigToPNode(){
        ConfigMessage configMessage = new ConfigMessage();
        configMessage.createJSONForNode(merchant);
        try {
            merchant.sendMessage(IMqttMerchant.sendConfigTopic, configMessage.getJson(), false);
            System.out.println("Sending initial message to PNode ... [OK]");
        } catch (MqttException ex) {
            System.out.println("Sending initial message to PNode ... [FAIL]");
            System.err.printf(ex.toString());
        }
    }
    
    public void divideMessages(String topic, String payload){
        JsonReader reader = Json.createReader(new StringReader(payload));
        JsonObject object = reader.readObject();
        String messageId = object.getString("messageId");
        String messageType = object.getString("messageType");
        String senderUuid = object.getString("senderUuid");
        String json = object.getJsonObject("payload").toString();
        if(messageType.equals("00000011")){
            reserveProduct(json);
        }
        else{
            System.out.println("Unknown incoming message type ... [FAIL]");
        }
    }
    
    public void reserveProduct(String payload){
        ReservationMessage reservationMessage =
                new ReservationMessage(payload);
        Reservation reservation = reservationMessage.parseJson();
        int succes = 0;
        synchronized(this) {
            for(Product product : products){
                if(reservation.getProductId() == product.getId())
                    reservation.setProduct(product);
            }
            if(reservation.getProduct().getSklad() >= reservation.getAmount()){
                succes = 1;
                reservation.getProduct().addObjednane(reservation.getAmount());
                reservation.getProduct().removeSklad(reservation.getAmount());
                this.reservations.add(reservation);
            }else{
                succes = 0;
            }
        }
        try {
            merchant.sendMessage(IMqttMerchant.sendAvailabilityTopic, 
                    reservationMessage.createJson(succes, reservation), false);
            System.out.println("Reservation product " + reservation.getProduct().getName()
                    + " amount " + reservation.getAmount() + " ... [OK]");
        } catch (MqttException ex) {
            System.out.println("Reservation product " + reservation.getProduct().getName()
                    + " amount " + reservation.getAmount() + " ... [FAIL]");
            System.err.printf(ex.toString());
        }
    }
}
