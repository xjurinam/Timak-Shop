/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.merchant;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.paho.client.mqttv3.MqttException;
import tp.message.ConfigMessage;

/**
 *
 * @author marek
 */
public class Main {
    public static void main(String[] args) {
        if(args.length == 0) {
            System.out.println("Need configuration file name as argument!");
            System.exit(-1);
        }
        // Nacitanie konfiguracie
        ConfigMessage configMessage = new ConfigMessage(
                Main.loadConfigurationFile(args[0]));
        Merchant2 merchant = new Merchant2();
        List<Product> products = new ArrayList();
        // PARSOVANIE KONGIGURACNEHO SUBORU
        configMessage.parseJSON(merchant, products);
        Worker worker = new Worker(merchant, products);
        try {
            // System.out.println(configMessage.getUuid());
            merchant.connectToBroker();
            merchant.subsrcibeTopics(worker);
            worker.sentCongfigToPNode();
            worker.sentProductsToPNode();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    try {
                        System.out.println("Exit signal!");
                        merchant.disconnectFromBroker();
                    } catch (MqttException ex) {
                        System.err.println(ex.toString());
                    }
                }
            });
        } catch (MqttException ex) {
            System.err.println(ex.toString());
        }
    }

    /**
     * Funkcia nacita subor a ulozi ho do stringu. Meno suboru je zadane ako
     * argument.
     * 
     * @param fileName nazov nacitavaneho suboru
     * @return string, ktory obsahuje nacitany subor
     */
    public static String loadConfigurationFile(String fileName){
        try {
            String configJson;
            configJson = new String(Files.readAllBytes(Paths.get(fileName)));
            System.out.println("Loading configuration from file " +
                    fileName + " ... [OK]");
            return configJson;
        } catch (IOException ex) {
            System.err.println(ex.toString());
            System.exit(-1);
        }
        return null;
    }
}
