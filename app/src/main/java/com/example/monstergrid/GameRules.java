package com.example.monstergrid;

public class GameRules {

    public static boolean isValidPlayerMove(int startX, int startY, int endX, int endY, int maxMove) {
        int dx = Math.abs(startX - endX);
        int dy = Math.abs(startY - endY);
        // Player moves only horizontally or vertically
        boolean isHorizontalOrVertical = (dx == 0 || dy == 0);
        return isHorizontalOrVertical && (dx + dy) <= maxMove && (dx + dy) > 0;
    }

    public static boolean isValidPlayerAttack(int startX, int startY, int endX, int endY, int range) {
        int dx = Math.abs(startX - endX);
        int dy = Math.abs(startY - endY);
        // Player attacks horizontally, vertically, or diagonally
        boolean isHVD = (dx == 0 || dy == 0 || dx == dy);
        // Chebyshev distance for diagonal range
        int distance = Math.max(dx, dy);
        return isHVD && distance <= range && distance > 0;
    }

    public static int[] getMonsterMove(int mX, int mY, int pX, int pY) {
        int dx = Integer.compare(pX, mX);
        int dy = Integer.compare(pY, mY);

        // Monsters move max 1 tile (Horizontal or Vertical only)
        // Prioritize the direction with the largest distance
        if (Math.abs(pX - mX) >= Math.abs(pY - mY)) {
            return new int[]{mX + dx, mY};
        } else {
            return new int[]{mX, mY + dy};
        }
    }
}
