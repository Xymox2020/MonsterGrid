package com.example.monstergrid;

public class GameRules {

    public static boolean isValidPlayerMove(int startX, int startY, int endX, int endY, int maxMove) {
        int dx = Math.abs(startX - endX);
        int dy = Math.abs(startY - endY);
        // Allow horizontal, vertical, and 45-degree diagonal moves.
        // Diagonal moves cost double (1+1=2 distance per tile).
        boolean isHVD = (dx == 0 || dy == 0 || dx == dy);
        return isHVD && (dx + dy) <= maxMove && (dx + dy) > 0;
    }

    public static boolean isValidPlayerAttack(int startX, int startY, int endX, int endY, int range) {
        int dx = Math.abs(startX - endX);
        int dy = Math.abs(startY - endY);
        boolean isHVD = (dx == 0 || dy == 0 || dx == dy);
        int distance = Math.max(dx, dy);
        return isHVD && distance <= range && distance > 0;
    }

    public static int[] getMonsterMove(int mX, int mY, int pX, int pY) {
        int targetX = mX;
        int targetY = mY;
        
        int dx = (pX > mX) ? 1 : (pX < mX) ? -1 : 0;
        int dy = (pY > mY) ? 1 : (pY < mY) ? -1 : 0;
        
        // Monsters move max 1 tile (STRICTLY Horizontal or Vertical only)
        // Move in the direction with the largest gap first to maintain grid alignment
        if (Math.abs(pX - mX) >= Math.abs(pY - mY)) {
            if (dx != 0) targetX = mX + dx;
            else if (dy != 0) targetY = mY + dy;
        } else {
            if (dy != 0) targetY = mY + dy;
            else if (dx != 0) targetX = mX + dx;
        }
        
        return new int[]{targetX, targetY};
    }
}
