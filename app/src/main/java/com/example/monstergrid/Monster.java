package com.example.monstergrid;

public class Monster {
    public int x;
    public int y;
    public int hp;
    public int maxHp;
    public int damage;

    public Monster(int x, int y, int level) {
        this.x = x;
        this.y = y;
        this.maxHp = 6 + level;
        this.hp = maxHp;
        this.damage = 1 + (level / 2);
    }
}
