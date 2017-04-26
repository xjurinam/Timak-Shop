/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.message;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;
import tp.merchant.Reservation;

/**
 *
 * @author marek
 */
public class ReservationMessage extends MQTTMessage{
    
    public static final String messageType = "00000011";

    public ReservationMessage() {
    }

    public ReservationMessage(String uuid,String json) {
        super(uuid, json);
    }

    public ReservationMessage(String json) {
        super(json);
    }
    
    public Reservation parseJson(){
        JsonReader reader = Json.createReader(new StringReader(this.getJson()));
        System.out.println(this.getJson());
        JsonObject object = reader.readObject();
        Reservation reservation = new Reservation();
        reservation.setOrderId(object.getInt("orderId"));
        reservation.setProductId(object.getInt("productId"));
        reservation.setAmount(object.getInt("amount"));
        reservation.setReservationUuid(object.getString("reservationUuid"));
        return reservation;
    }
    
    public String createJson(int success, Reservation reservation){
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonObject object = factory.createObjectBuilder()
                //.add("messageId", super.getUuid())  // message id
                .add("messageType", this.messageType) // message type
                //.add("senderUuid", "") //merchant uuid
                //.add("payload", Json.createObjectBuilder()
                        .add("orderId", reservation.getOrderId())
                        .add("success", success)
                        .add("reservationUuid", reservation.getReservationUuid())
                .build();
        return object.toString();
    }
}
