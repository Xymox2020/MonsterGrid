package com.example.monstergrid;

import java.util.*;

public class PathFinder {
    public static int[] getNextStep(int startX, int startY, int targetX, int targetY, int gridSize, List<Obstacle> obstacles, List<Monster> monsters, List<Player> players) {
        Queue<Node> queue = new LinkedList<>();
        queue.add(new Node(startX, startY, null));
        
        boolean[][] visited = new boolean[gridSize][gridSize];
        visited[startX][startY] = true;
        
        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};
        
        Node resultNode = null;
        
        while (!queue.isEmpty()) {
            Node current = queue.poll();
            
            if (current.x == targetX && current.y == targetY) {
                resultNode = current;
                break;
            }
            
            for (int i = 0; i < 4; i++) {
                int nx = current.x + dx[i];
                int ny = current.y + dy[i];
                
                if (nx >= 0 && nx < gridSize && ny >= 0 && ny < gridSize && !visited[nx][ny]) {
                    boolean isTarget = (nx == targetX && ny == targetY);
                    if (isTarget || isPassable(nx, ny, obstacles, monsters, players)) {
                        visited[nx][ny] = true;
                        queue.add(new Node(nx, ny, current));
                    }
                }
            }
        }
        
        if (resultNode == null) return null;
        
        Node step = resultNode;
        while (step.parent != null && (step.parent.x != startX || step.parent.y != startY)) {
            step = step.parent;
        }
        
        return new int[]{step.x, step.y};
    }
    
    private static boolean isPassable(int x, int y, List<Obstacle> obstacles, List<Monster> monsters, List<Player> players) {
        for (Obstacle o : obstacles) if (o.x == x && o.y == y) return false;
        for (Monster m : monsters) if (m.x == x && m.y == y) return false;
        for (Player p : players) if (p.hp > 0 && p.x == x && p.y == y) return false;
        return true;
    }
    
    private static class Node {
        int x, y;
        Node parent;
        Node(int x, int y, Node parent) {
            this.x = x; this.y = y; this.parent = parent;
        }
    }
}
