/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.merchant;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 *
 * @author marek
 */
public class Merchant2 implements IMqttMerchant{
    
    private List<String> IBANs;
    private String uuid;
    private String merchantName;
    private MqttAsyncClient client;
    private MqttConnectOptions connOpts;
    private IMqttToken token;

    public Merchant2() {
        this.IBANs = new ArrayList();
    }

    public IMqttToken getToken() {
        return token;
    }

    public void setToken(IMqttToken token) {
        this.token = token;
    }

    public MqttConnectOptions getConnOpts() {
        return connOpts;
    }

    public void setConnOpts(MqttConnectOptions connOpts) {
        this.connOpts = connOpts;
    }

    public MqttAsyncClient getClient() {
        return client;
    }

    public void setClient(MqttAsyncClient client) {
        this.client = client;
    }

    public List<String> getIBANs() {
        return IBANs;
    }

    public void setIBANs(List<String> IBANs) {
        this.IBANs = IBANs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }
    
    public void connectToBroker() throws MqttException{
        MemoryPersistence persistance = new MemoryPersistence();
        this.connOpts.setCleanSession(false);
        this.connOpts.setAutomaticReconnect(true);
        this.client = new MqttAsyncClient(this.connOpts.getServerURIs()[1], this.merchantName, persistance);
        this.token = client.connect(this.connOpts);
        this.token.waitForCompletion();
        System.out.println("Connecting to broker ... [OK]");
    }
    
    public void disconnectFromBroker() throws MqttException{
        if(this.client.isConnected()) {
            token = this.client.disconnect();
            token.waitForCompletion();
            System.out.println("Disconnecting from broker ... [OK]");
        }
    }
    
    public void subsrcibeTopics(Worker worker) throws MqttException{
        // ODCHYTAVANIE SPRAV A POTOM ICH ROZTRIEDENIE NA NASLEDUJUCE SPRACOVANIE
        this.token = this.client.subscribe(IMqttMerchant.subTopic + this.merchantName,
                IMqttMerchant.QoS,
                new IMqttMessageListener(){
            @Override
            public void messageArrived(String topic, MqttMessage m) throws Exception {
                worker.divideMessages(topic, m.toString());
            }
        });
    }
    
    public void sendMessage(String topic, String payload, boolean retain) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        message.setRetained(retain);
        message.setQos(2);
        this.client.publish(topic, message);
        System.out.println("S: "+topic+" : "+payload);
    }
}
