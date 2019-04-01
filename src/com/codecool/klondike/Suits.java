package com.codecool.klondike;

public enum Suits {
    HEARTS(1, "red"),
    DIAMONDS(2, "red"),
    SPADES(3, "black"),
    CLUBS(4, "black");

    private int value;
    private String color;

    Suits(int value, String color) {
        this.value = value;
        this.color = color;
    }

    public int getValue() {
        return value;
    }

    public String getColor() {
        return color;
    }    
}