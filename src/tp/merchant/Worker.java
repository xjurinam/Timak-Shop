/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.merchant;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
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
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
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
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void divideMessages(String topic, String payload){
        JsonReader reader = Json.createReader(new StringReader(payload));
        JsonObject object = reader.readObject();
        String messageId = "";
        if(object.containsKey("messageId"))
            messageId = object.getString("messageId");
        String messageType = object.getString("messageType");
        String senderUuid = "";
        if(object.containsKey("senderUuid"))
            senderUuid = object.getString("senderUuid");
        String json = object.getJsonObject("payload").toString();
        if(messageType.equals("00000011")){
            reserveProduct(json);
        }
        else if(messageType.equals("00000101")){
            endReservation(json);
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
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void endReservation(String json){
        JsonReader reader = Json.createReader(new StringReader(json));
        JsonObject object = reader.readObject();
        String reservationUuid = object.getString("reservationUuid");
        int orderId = object.getInt("orderId");
        int success = object.getInt("success");
        String mail = object.getString("mail");
        String message = object.getString("message");
        try{
            int counter = 0; // Pocitadlo kolko je v zozname rezervacii z rovnakej objednavky
            String mailMessage = "";
            synchronized(this){
                for(Reservation reservation : reservations){
                    if(reservation.getReservationUuid().equals(reservationUuid)){
                        reservation.setState(success);
                        Product product = reservation.getProduct();
                        if(success == 1){
                            product.removeObjednane(reservation.getAmount());
                            product.addPredane(reservation.getAmount());
                        }else{
                            product.removeObjednane(reservation.getAmount());
                            product.addSklad(reservation.getAmount());
                        }
                    }
                    if(reservation.getOrderId() == orderId && reservation.getState() == -1)
                        counter++;
                }
                System.out.println("End reservation with uuid " + reservationUuid + " ... [OK]");
                if(counter == 0){
                    List<Reservation> remeveReservation = new ArrayList();
                    //System.out.println("R: "+reservations.size());
                    mailMessage += "Doručujeme Vám nasledovné produkty:\n";
                    for(Reservation reservation : reservations){
                        if(reservation.getOrderId() == orderId){
                            mailMessage +=  reservation.getProduct().getName() + " - "
                                    + reservation.getAmount() + "x\t" + 
                                    String.format("%.2f", (reservation.getProduct().getPrice() * reservation.getAmount())) + " €\n";
                            remeveReservation.add(reservation);
                        }
                    }
                    reservations.removeAll(remeveReservation);
                    mailMessage += "\nĎakujeme za Váš nákup.";
                    //System.out.println("R: "+reservations.size());
                    sentProductsToPNode();
                    if(Main.useMail)
                        sendEmail(mail, mailMessage);
                }
            }
        }catch(Exception ex){
            System.out.println("End reservation with uuid " + reservationUuid + " ... [FAIL]");
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendEmail(String mailTo, String sprava) {
        final String username = "nodeprocess";
	final String password = "Lap53dun";
        
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class",
                        "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.port", "465");
        
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(username, password);
			}
		  });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("nodeprocess@gmail.com"));
            message.setRecipients(Message.RecipientType.TO,
            InternetAddress.parse(mailTo));
            message.setSubject("Doručenie objednávky od " + this.merchant.getMerchantName());
            message.setText(sprava);

            Transport.send(message);
            System.out.println("Send email to " + mailTo + " ... [OK]");
        } catch (MessagingException ex) {
            System.out.println("Send email to " + mailTo + " ... [FAIL]");
            Logger.getLogger(Worker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
