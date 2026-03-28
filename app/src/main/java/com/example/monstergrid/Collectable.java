package com.example.monstergrid;

public class Collectable {
    public static final int TYPE_LOOT = 0;
    public static final int TYPE_BERRY = 1;

    public int x, y;
    public int type;

    public Collectable(int x, int y, int type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }
}
