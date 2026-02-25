package com.example.monstergrid;

public class Player {
    public int x, y;
    public int hp, maxHp;
    public int exp, level;
    public int damageModifier;
    public int rangeModifier;
    public int movementModifier;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.maxHp = 20;
        this.hp = 20;
        this.exp = 0;
        this.level = 1;
        this.damageModifier = 0;
        this.rangeModifier = 1;
        this.movementModifier = 1;
    }

    public void addExp(int amount) {
        this.exp += amount;
    }

    public boolean canLevelUp() {
        return exp >= 6;
    }
}
