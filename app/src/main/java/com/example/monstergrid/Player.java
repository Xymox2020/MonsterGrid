package com.example.monstergrid;

public class Player {
    public int x, y;
    public int hp, maxHp;
    public int exp, level;
    public int damageModifier;
    public int rangeModifier;
    public int movementModifier;
    
    // Turn state
    public boolean hasMoved = false;
    public boolean hasAttacked = false;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
        this.maxHp = 20;
        this.hp = 20;
        this.exp = 0;
        this.level = 1;
        this.damageModifier = 0;
        this.rangeModifier = 3;    // Base range 3
        this.movementModifier = 2; // Base movement 2
    }

    public void resetTurn() {
        hasMoved = false;
        hasAttacked = false;
    }

    public boolean canLevelUp() {
        return exp >= 6;
    }
}
