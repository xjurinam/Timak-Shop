/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.merchant;

import java.util.Random;

/**
 *
 * @author marek
 */
public class Product {
    private String name;
    private double price;
    private int id;
    private String topic;
    private int sklad;
    private int objednane;
    private int predane;

    public Product(String name, double price, int id, String topic) {
        this.name = name;
        this.price = price;
        this.id = id;
        this.topic = topic;
        Random rand = new Random(); 
        this.sklad = rand.nextInt(50) + 5;
        this.objednane = 0;
        this.predane = 0;
    }
    
    public Product() {
        Random rand = new Random(); 
        this.sklad = rand.nextInt(50) + 5;
        this.objednane = 0;
        this.predane = 0;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getSklad() {
        return sklad;
    }

    public void setSklad(int sklad) {
        this.sklad = sklad;
    }

    public void addSklad(int sklad) {
        this.sklad += sklad;
    }
    
    public void removeSklad(int sklad) {
        if((this.sklad - sklad) >= 0)
            this.sklad -= sklad;
    }
    
    public int getObjednane() {
        return objednane;
    }

    public void setObjednane(int objednane) {
        this.objednane = objednane;
    }
    
    public void addObjednane(int objednane) {
        this.objednane += objednane;
    }
    
    public void removeObjednane(int objednane) {
        if((this.objednane - objednane) >= 0)
            this.objednane -= objednane;
    }

    public int getPredane() {
        return predane;
    }

    public void setPredane(int predane) {
        this.predane = predane;
    }

    public void addPredane(int predane) {
        this.predane += predane;
    }
    
    public void removePredane(int predane) {
        if((this.predane - predane) >= 0)
            this.predane -= predane;
    }
    
    @Override
    public String toString() {
        return "Product{" + "name=" + name + ", price=" + price + ", id=" + id + ", topic=" + topic + ", sklad=" + sklad + ", objednane=" + objednane + ", predane=" + predane + '}';
    }
    
    public String productForNode() {
        return "{\"product_name\":\""+this.name+"\",\"product_price\":"+this.price+",\"amount\":"+
                this.sklad+",\"product_id\":"+this.id+"}";
    }
}
