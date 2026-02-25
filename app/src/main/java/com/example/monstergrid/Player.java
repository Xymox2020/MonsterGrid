package com.example.monstergrid;

public class Player {
    int x;
    int y;
    int hp;
    int exp;
    int damageModifier;
    int rangeModifier;
    int movementModifier;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.hp = 20;
        this.exp = 0;
        this.damageModifier = 0;
        this.rangeModifier = 1;
        this.movementModifier = 1;
    }
}
