/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.message;

import java.io.StringReader;
import java.util.List;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonReader;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import tp.merchant.Merchant2;
import tp.merchant.Product;

/**
 *
 * @author marek
 */
public class ConfigMessage extends MQTTMessage{
    
    public static final String messageType = "00000001";

    public ConfigMessage(String json) {
        super(json);
    }

    public ConfigMessage(String uuid, String json) {
        super(uuid, json);
    }

    public ConfigMessage() {
    }
    

    public void parseJSON(Merchant2 merchant, List<Product> products) {
        JsonReader reader = Json.createReader(new StringReader(super.getJson()));
        JsonObject object = reader.readObject();
        JsonObject obj;
        merchant.setMerchantName(object.getString("merchant_name"));
        merchant.setUuid(object.getString("merchant_uuid"));
        // connection_options
        obj = object.getJsonObject("connection_options");
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(obj.getBoolean("clean_session"));
        connOpts.setUserName(obj.getString("username"));
        connOpts.setPassword(obj.getString("password").toCharArray());
        String[] uris = new String[2];
        uris[0] = "tcp://localhost:1883";
        uris[1] = "tcp://"+
                obj.getString("host")+":"+
                obj.getInt("port");
        connOpts.setServerURIs(uris);
        merchant.setConnOpts(connOpts);
        // bank_options
        JsonArray array = object.getJsonArray("bank_options");
        for(int i = 0; i < array.size(); i++){
            obj = array.getJsonObject(i);
            merchant.getIBANs().add(obj.getString("IBAN"));
        }
        // products
        array = object.getJsonArray("products");
        for(int i = 0; i < array.size(); i++){
            object = array.getJsonObject(i);
            Product product = new Product();
            product.setName(object.getString("product_name"));
            product.setPrice(object.getJsonNumber("product_price").doubleValue());
            product.setId(object.getInt("product_id"));
            product.setTopic(object.getString("topic"));
            products.add(product);
        }
    }
    
    public String createJSONForNode(Merchant2 merchant){
        JsonBuilderFactory factory = Json.createBuilderFactory(null);
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for(String iban:merchant.getIBANs()){
            builder.add(Json.createObjectBuilder().add("IBAN", iban));
        }
        JsonObject object = factory.createObjectBuilder()
                .add("messageId", super.getUuid())  // message id
                .add("messageType", ConfigMessage.messageType) // message type
                .add("senderUuid", merchant.getUuid()) //merchant uuid
                .add("payload", Json.createObjectBuilder()
                        .add("merchantName", merchant.getMerchantName())
                        .add("IBANs", builder))
                .build();
        super.setJson(object.toString());
        return object.toString();
    }
    
}
