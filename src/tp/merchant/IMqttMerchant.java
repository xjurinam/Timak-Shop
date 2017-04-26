/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.merchant;
/**
 *
 * @author marek
 * TP Bezpecne sprostredkovanie online platieb
 */
public interface IMqttMerchant {
    static final String subTopic = "shops/"; // za lomitkom pribudne v kode identifikator obchodnika
    static final String sendProductsTopic = "pn/products";
    static final String sendConfigTopic = "pn/shops";
    static final String sendAvailabilityTopic = "pn/response/products";
    static final int QoS = 2;
    
}
