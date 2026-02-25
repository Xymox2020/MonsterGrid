package com.example.monstergrid;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int GRID_SIZE = 5;
    private TextView[][] cells = new TextView[GRID_SIZE][GRID_SIZE];
    private Player player1, player2;
    private List<Monster> monsters = new ArrayList<>();
    private int currentPlayer = 1; // 1 or 2
    private int actionsLeft = 2;
    private int turnCounter = 0;
    private boolean isMoveMode = false;
    private boolean isAttackMode = false;

    private TextView statusText, playerStats, logText;
    private Button btnMove, btnAttack;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        playerStats = findViewById(R.id.playerStats);
        logText = findViewById(R.id.logText);
        btnMove = findViewById(R.id.btnAction1);
        btnAttack = findViewById(R.id.btnAction2);

        setupGrid();
        initGame();

        btnMove.setOnClickListener(v -> {
            isMoveMode = true;
            isAttackMode = false;
            logText.setText("Select a square to move to (Range: " + getCurrentPlayer().movementModifier + ")");
        });

        btnAttack.setOnClickListener(v -> {
            isAttackMode = true;
            isMoveMode = false;
            logText.setText("Select a target to attack (Range: " + getCurrentPlayer().rangeModifier + ")");
        });

        updateUI();
    }

    private void setupGrid() {
        GridLayout gridLayout = findViewById(R.id.gameGrid);
        int cellSize = getResources().getDisplayMetrics().widthPixels / 6;

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(2, 2, 2, 2);
                cell.setLayoutParams(params);
                cell.setBackgroundColor(Color.DKGRAY);
                cell.setGravity(Gravity.CENTER);
                cell.setTextColor(Color.WHITE);
                cell.setTextSize(18);
                
                final int row = i;
                final int col = j;
                cell.setOnClickListener(v -> handleCellClick(row, col));
                
                cells[i][j] = cell;
                gridLayout.addView(cell);
            }
        }
    }

    private void initGame() {
        player1 = new Player(0, 2);
        player2 = new Player(4, 2);
        
        monsters.add(new Monster(1, 1));
        monsters.add(new Monster(1, 3));
        monsters.add(new Monster(3, 1));
        monsters.add(new Monster(3, 3));
        
        drawBoard();
    }

    private void handleCellClick(int r, int c) {
        if (actionsLeft <= 0) return;

        Player p = getCurrentPlayer();

        if (isMoveMode) {
            int dist = Math.abs(p.x - r) + Math.abs(p.y - c);
            if (dist <= p.movementModifier && dist > 0 && isEmpty(r, c)) {
                p.x = r;
                p.y = c;
                useAction("Moved to " + r + "," + c);
            } else {
                Toast.makeText(this, "Invalid move!", Toast.LENGTH_SHORT).show();
            }
        } else if (isAttackMode) {
            int dist = Math.abs(p.x - r) + Math.abs(p.y - c);
            if (dist <= p.rangeModifier) {
                if (isMonsterAt(r, c)) {
                    attackMonster(r, c);
                } else if (isOpponentAt(r, c)) {
                    attackOpponent();
                } else {
                    Toast.makeText(this, "No target there!", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Out of range!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void attackMonster(int r, int c) {
        Monster m = getMonsterAt(r, c);
        int roll = random.nextInt(6) + 1;
        int damage = roll + getCurrentPlayer().damageModifier;
        m.hp -= damage;
        logText.setText("Rolled " + roll + "! Dealt " + damage + " damage to monster.");
        
        if (m.hp <= 0) {
            monsters.remove(m);
            getCurrentPlayer().exp += 3;
            logText.append("\nMonster killed! +3 EXP");
            checkLevelUp(getCurrentPlayer());
        }
        useAction("");
    }

    private void attackOpponent() {
        Player target = (currentPlayer == 1) ? player2 : player1;
        int roll = random.nextInt(6) + 1;
        int damage = roll + getCurrentPlayer().damageModifier;
        target.hp -= damage;
        logText.setText("Rolled " + roll + "! Dealt " + damage + " damage to Player " + (currentPlayer == 1 ? 2 : 1));
        
        if (target.hp <= 0) {
            target.hp = 0;
            updateUI();
            Toast.makeText(this, "Player " + currentPlayer + " WINS!", Toast.LENGTH_LONG).show();
            finish();
        }
        useAction("");
    }

    private void checkLevelUp(Player p) {
        if (p.exp >= 6) {
            p.exp -= 6;
            int upgrade = random.nextInt(3);
            String msg = "";
            switch (upgrade) {
                case 0: p.damageModifier++; msg = "Damage +1"; break;
                case 1: p.movementModifier++; msg = "Movement +1"; break;
                case 2: p.rangeModifier++; msg = "Range +1"; break;
            }
            Toast.makeText(this, "LEVEL UP! " + msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void useAction(String msg) {
        actionsLeft--;
        isMoveMode = false;
        isAttackMode = false;
        if (!msg.isEmpty()) logText.setText(msg);
        
        if (actionsLeft == 0) {
            endTurn();
        }
        drawBoard();
        updateUI();
    }

    private void endTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        actionsLeft = 2;
        turnCounter++;
        
        if (turnCounter % 2 == 0) {
            moveMonsters();
        }
        
        Toast.makeText(this, "Player " + currentPlayer + "'s Turn", Toast.LENGTH_SHORT).show();
    }

    private void moveMonsters() {
        for (Monster m : monsters) {
            int dir = random.nextInt(4);
            int nx = m.x, ny = m.y;
            if (dir == 0) nx--; else if (dir == 1) nx++; else if (dir == 2) ny--; else ny++;
            
            if (nx >= 0 && nx < GRID_SIZE && ny >= 0 && ny < GRID_SIZE && isEmpty(nx, ny)) {
                m.x = nx;
                m.y = ny;
            }
        }
    }

    private void updateUI() {
        statusText.setText("Player " + currentPlayer + " Turn (" + actionsLeft + " actions left)");
        playerStats.setText("P1 HP: " + player1.hp + " (EXP: " + player1.exp + ") | P2 HP: " + player2.hp + " (EXP: " + player2.exp + ")");
    }

    private void drawBoard() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                cells[i][j].setText("");
                cells[i][j].setBackgroundColor(Color.DKGRAY);
            }
        }

        cells[player1.x][player1.y].setText("P1");
        cells[player1.x][player1.y].setBackgroundColor(Color.BLUE);
        
        cells[player2.x][player2.y].setText("P2");
        cells[player2.x][player2.y].setBackgroundColor(Color.RED);

        for (Monster m : monsters) {
            cells[m.x][m.y].setText("M\n" + m.hp);
            cells[m.x][m.y].setBackgroundColor(Color.parseColor("#442200"));
        }
    }

    private Player getCurrentPlayer() {
        return (currentPlayer == 1) ? player1 : player2;
    }

    private boolean isEmpty(int r, int c) {
        if (player1.x == r && player1.y == c) return false;
        if (player2.x == r && player2.y == c) return false;
        for (Monster m : monsters) if (m.x == r && m.y == c) return false;
        return true;
    }

    private boolean isMonsterAt(int r, int c) {
        return getMonsterAt(r, c) != null;
    }

    private Monster getMonsterAt(int r, int c) {
        for (Monster m : monsters) if (m.x == r && m.y == c) return m;
        return null;
    }

    private boolean isOpponentAt(int r, int c) {
        Player opp = (currentPlayer == 1) ? player2 : player1;
        return opp.x == r && opp.y == c;
    }
}
