/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.merchant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import tp.message.ConfigMessage;

/**
 *
 * @author marek
 */
public class Merchant implements IMqttMerchant{
    private String IBAN;
    private String merchantName;
    private String host;
    private int port;
    private List<Product> products;
    
    public MqttAsyncClient client;
    
    public Merchant(){
        this.products = new ArrayList();
    }

    /**
     * @param args the command line arguments
     */
    /*public static void main(String[] args) {
        Merchant merchant = new Merchant();
        // NACITANIE KONFIGURACNEHO SUBORU
        if(args.length == 0) {
            System.out.println("Need configuration file name as argument!");
            return;
        }
        //c.parseJSON();
        merchant.loadConfigurationFromFile(new File(args[0]));
        merchant.connectToBroker();
        merchant.sendProductsForNode("offer");
        
        try {
            merchant.client.subscribe("PN/"+merchant.merchantName, 2, new IMqttMessageListener(){
                @Override
                public void messageArrived(String topic, MqttMessage m) throws Exception {
                    merchant.spracujObjednavku(m.toString());
                }
            });
            // ODCHYTAVANIE SPRAV NA POTVRDENIE STAVU NAKUPOV
            merchant.client.subscribe("/control/pn/"+merchant.merchantName+"/confirmation", 2, new IMqttMessageListener(){
                @Override
                public void messageArrived(String topic, MqttMessage m) throws Exception {
                    merchant.spracujPotvrdenie(m.toString());
                }
            });
        }
            //TimeUnit.SECONDS.sleep(10);
            //merchant.disconnectFromBroker();
            
        //} catch (InterruptedException ex) {
        //    Logger.getLogger(Merchant.class.getName()).log(Level.SEVERE, null, ex);
        //    System.out.println(ex.toString());} 
    catch (MqttException ex) {
            Logger.getLogger(Merchant.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.toString());
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Shutdown hook ran!");
                merchant.disconnectFromBroker();
            }
        });
    }*/
    
    public void spracujPotvrdenie(String json) {
        System.out.println(json+"\n");
        try(JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject object = reader.readObject();
            if(!object.containsKey("command") ||
                    !object.containsKey("success") ||
                    !object.containsKey("products") ||
                    !object.getString("command").equals("purchaseConfirmation"))
                return;
            int success = object.getInt("success");
            String mailTo = object.getString("mail");
            String mail = "Dorucenie: \n\n";
            JsonArray array = object.getJsonArray("products");
            for(int i = 0; i < array.size(); i++) {
                object = array.getJsonObject(i);
                Product product = this.getProduct(object.getInt("productID"));
                if(product != null){
                    if(success == 1) {
                        product.removeObjednane(object.getInt("amount"));
                        product.addPredane(object.getInt("amount"));
                        mail +=product.getName()+"   "+object.getInt("amount")+"x\n";
                    }
                    else {
                        product.removeObjednane(object.getInt("amount"));
                        product.addSklad(object.getInt("amount"));
                    }
                }
            }
            mail += "\n\nDakujeme za vas nakup.";
            System.out.println(mail);
            this.sendEmail(mailTo, mail);
            this.sendProductsForNode("update");
        } catch(Exception e) {
            Logger.getLogger(Merchant.class.getName()).log(Level.SEVERE, null, e);
            System.out.println(e.toString());
        }
    }
    
    public void spracujObjednavku(String json){
        int id = -1;
        try(JsonReader reader = Json.createReader(new StringReader(json))) {
            JsonObject object = reader.readObject();
            String command = object.getString("command");
            int orderID = object.getInt("orderID");
            int merchantID = object.getInt("merchantID");
            int productID = object.getInt("productID");
            id = productID;
            int amount = object.getInt("amount");
            for(Product product : products) {
                if(product.getId() == productID){
                    String sprava = null;
                    IMqttToken token;
                    MqttMessage message = new MqttMessage();
                    message.setQos(2);
                    if(product.getSklad() >= amount) {
                        product.setSklad(product.getSklad()-amount);
                        product.setObjednane(product.getObjednane() + amount);
                        sprava = "{\"orderID\":"+orderID+",\"merchantID\":"+merchantID+
                                ",\"productID\":"+productID+",\"amount\":"+amount+",\"success\":1}";
                    }
                    else {
                        sprava = "{\"orderID\":"+orderID+",\"merchantID\":"+merchantID+
                                ",\"productID\":"+productID+",\"amount\":"+product.getSklad()+",\"success\":0}";
                        product.setSklad(0);
                        product.setObjednane(product.getObjednane() + product.getSklad());
                    }
                    message.setPayload(sprava.getBytes());
                    token = this.client.publish("/control/pn/responses", message);
                    token.waitForCompletion();
                    System.out.println("Objednavka produktu "+id+" :\t[OK]");
                    return;
                }
            }
        }catch(Exception e){
            System.out.println("Objednavka produktu "+id+" :\t[FAIL]");
            System.out.println(e.toString());
        }
    }
    
    public void sendProductsForNode(String command) {
        IMqttToken token;
        MqttMessage message = new MqttMessage();
        message.setQos(2);
        String sprava = "";
        try {
            sprava += "{\"command\":\""+command+"\",\"merchant_name\":\""+this.merchantName+"\","
                    + "\"IBAN\":\""+this.IBAN+"\","
                    +"\"products\":[";
            for(Product product : this.products) {
                if(sprava.endsWith("}"))
                    sprava += ",";
                sprava += product.productForNode();
            }
            sprava += "]}";
            message.setPayload(sprava.getBytes());
            token = this.client.publish("/control/pn/products", message);
            System.out.println(message);
            token.waitForCompletion();
            System.out.println("Sending products for procesing node ... [OK]");
        } catch (MqttException ex) {
            System.out.println("Sending products for procesing node ... [FAIL]");
            Logger.getLogger(Merchant.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.toString());
        }
    }
    
    public void disconnectFromBroker(){
        IMqttToken token;
        try {
            if(this.client.isConnected()) {
                token = this.client.disconnect();
                token.waitForCompletion();
                System.out.println("Disconnecting from broker ... [OK]");
            }
        } catch (MqttException ex) {
            System.out.println("Disconnecting from broker ... [FAIL]");
            Logger.getLogger(Merchant.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.toString());
        }
    }
    
    public void connectToBroker(){
        //String broker       = "tcp://test.mosquitto.org:1883";
        String broker = "tcp://"+this.host+":"+this.port;
        IMqttToken token;
        MemoryPersistence persistance = new MemoryPersistence();
        MqttConnectOptions connOpts = new MqttConnectOptions();
        //connOpts.setPassword("marek".toCharArray());
        connOpts.setCleanSession(true);
        try {
            this.client = new MqttAsyncClient(broker, this.merchantName, persistance);
            token = client.connect(connOpts);
            token.waitForCompletion();
            System.out.println("Connecting to broker ... [OK]");
        } catch (Exception ex) {
            System.out.println("Connecting to broker ... [FAIL]");
            Logger.getLogger(Merchant.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.getMessage());
            System.exit(0);
        }
    }

    public void loadConfigurationFromFile(File configFile) {
        try {
            InputStream fis = new FileInputStream(configFile);
            JsonReader reader = Json.createReader(fis);
            JsonObject object = reader.readObject();
            
            this.merchantName = object.getString("merchant_name");
            this.host = object.getString("host");
            this.port = object.getInt("port");
            this.IBAN = object.getString("IBAN");
            JsonArray array = object.getJsonArray("products");
            for(int i = 0; i < array.size(); i++){
                JsonObject obj = array.getJsonObject(i);
                Product product = new Product();
                product.setName(obj.getString("product_name"));
                product.setPrice(obj.getJsonNumber("product_price").doubleValue());
                product.setId(obj.getInt("product_id"));
                product.setTopic(obj.getString("topic"));
                this.products.add(product);
            }
            
            fis.close();
            reader.close();
            System.out.println("Loading configuration from file "+configFile.getName()+" ... [OK]");
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Merchant.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Configuration file doesn't exist!");
            System.out.println("Loading configuration from file "+configFile.getName()+" ... [FAIL]");
            System.out.println(ex.toString());
            System.exit(0);
        } catch (IOException ex) {
            System.out.println("Loading configuration from file "+configFile.getName()+" ... [FAIL]");
            Logger.getLogger(Merchant.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println(ex.toString());
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
            message.setSubject("Doručenie objednávky");
            message.setText(sprava);

            Transport.send(message);
            System.out.println("Sent message successfully....");
            System.out.println(mailTo+"\n"+sprava);
      }catch (Exception e) {
         e.printStackTrace();
            System.out.println(e.toString());
      }
    }
    
    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
    
    public Product getProduct(int id) {
        for(Product product : this.products) {
            if(product.getId() == id)
                return product;
        }
        return null;
    }
    
    public void sendMessage(String topic, String payload, boolean retain) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setRetained(retain);
        message.setQos(2);
        this.client.publish(topic, message);
    }

    public void sendMessage(String topic) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}