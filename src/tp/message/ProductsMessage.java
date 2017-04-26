/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.message;

import java.util.List;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import tp.merchant.Product;

/**
 *
 * @author marek
 */
public class ProductsMessage extends MQTTMessage{

    public static final String messageType = "00000010";
    
    public ProductsMessage() {
    }
    
    public String createProductsMessage(List<Product> products, String senderUuid) {
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(Product product : products){
            JsonObjectBuilder object = Json.createObjectBuilder();
            object.add("productName", product.getName());
            object.add("productPrice", product.getPrice());
            object.add("amount", product.getSklad());
            object.add("productId", product.getId());
            builder.add(object);
        }
        JsonObject object = factory.createObjectBuilder()
                .add("messageId", super.getUuid())  // message id
                .add("messageType", ProductsMessage.messageType) // message type
                .add("senderUuid", senderUuid) // sender uuid
                .add("payload", builder)
                .build();
        super.setJson(object.toString());
        return object.toString();
    }
}
