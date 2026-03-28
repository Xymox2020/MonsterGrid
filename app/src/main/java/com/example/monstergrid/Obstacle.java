package com.example.monstergrid;

public class Obstacle {
    public int x;
    public int y;
    public int type; // 0 for rock, 1 for tree

    public Obstacle(int x, int y) {
        this.x = x;
        this.y = y;
        this.type = new java.util.Random().nextInt(2);
    }
}
