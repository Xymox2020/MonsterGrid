package com.example.monstergrid;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int GRID_SIZE = 10;
    private TextView[][] cells = new TextView[GRID_SIZE][GRID_SIZE];
    private Player player1, player2;
    private List<Monster> monsters = new ArrayList<>();
    private int currentPlayer = 1;
    private int actionsLeft = 2;
    private int turnCounter = 0;
    private boolean isMoveMode = false;
    private boolean isAttackMode = false;

    private TextView statusText, p1Stats, p2Stats, logText;
    private ProgressBar expBar;
    private Button btnMove, btnAttack;
    private View upgradeOverlay;
    private Button[] upgradeButtons = new Button[3];
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        p1Stats = findViewById(R.id.p1Stats);
        p2Stats = findViewById(R.id.p2Stats);
        logText = findViewById(R.id.logText);
        expBar = findViewById(R.id.expBar);
        btnMove = findViewById(R.id.btnAction1);
        btnAttack = findViewById(R.id.btnAction2);
        upgradeOverlay = findViewById(R.id.upgradeOverlay);
        upgradeButtons[0] = findViewById(R.id.upgrade1);
        upgradeButtons[1] = findViewById(R.id.upgrade2);
        upgradeButtons[2] = findViewById(R.id.upgrade3);

        setupGrid();
        initGame();

        btnMove.setOnClickListener(v -> {
            isMoveMode = !isMoveMode;
            isAttackMode = false;
            drawBoard();
            if (isMoveMode) {
                logText.setText("Select a square to move to (Range: " + getCurrentPlayer().movementModifier + ")");
                highlightRange(getCurrentPlayer().x, getCurrentPlayer().y, getCurrentPlayer().movementModifier, Color.parseColor("#44AAFFAA"));
            } else {
                logText.setText("Movement cancelled.");
            }
        });

        btnAttack.setOnClickListener(v -> {
            isAttackMode = !isAttackMode;
            isMoveMode = false;
            drawBoard();
            if (isAttackMode) {
                logText.setText("Select a target (Range: " + getCurrentPlayer().rangeModifier + ")");
                highlightRange(getCurrentPlayer().x, getCurrentPlayer().y, getCurrentPlayer().rangeModifier, Color.parseColor("#44FFAAAA"));
            } else {
                logText.setText("Attack cancelled.");
            }
        });

        updateUI();
    }

    private void setupGrid() {
        GridLayout gridLayout = findViewById(R.id.gameGrid);
        int cellSize = getResources().getDisplayMetrics().widthPixels / 11;

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(1, 1, 1, 1);
                cell.setLayoutParams(params);
                cell.setBackgroundColor(Color.parseColor("#222222"));
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(14);
                
                final int row = i;
                final int col = j;
                cell.setOnClickListener(v -> handleCellClick(row, col));
                
                cells[i][j] = cell;
                gridLayout.addView(cell);
            }
        }
    }

    private void initGame() {
        player1 = new Player(0, 4);
        player2 = new Player(9, 4);
        
        // Add more initial monsters
        for (int i = 0; i < 8; i++) {
            spawnMonster();
        }
        
        drawBoard();
    }

    private void spawnMonster() {
        int rx, ry;
        do {
            rx = random.nextInt(GRID_SIZE);
            ry = random.nextInt(GRID_SIZE);
        } while (!isEmpty(rx, ry) || (Math.abs(rx-player1.x) < 2) || (Math.abs(rx-player2.x) < 2));
        monsters.add(new Monster(rx, ry, turnCounter / 4));
    }

    private void handleCellClick(int r, int c) {
        if (actionsLeft <= 0 || upgradeOverlay.getVisibility() == View.VISIBLE) return;

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
        logText.setText("Rolled 🎲" + roll + "! Dealt " + damage + " DMG to Monster.");
        
        if (m.hp <= 0) {
            monsters.remove(m);
            getCurrentPlayer().exp += 3;
            logText.append("\nMonster slain! +3 EXP");
            if (getCurrentPlayer().canLevelUp()) {
                showUpgradeOverlay();
            }
        }
        useAction("");
    }

    private void attackOpponent() {
        Player target = (currentPlayer == 1) ? player2 : player1;
        int roll = random.nextInt(6) + 1;
        int damage = roll + getCurrentPlayer().damageModifier;
        target.hp -= damage;
        logText.setText("Rolled 🎲" + roll + "! Shot Player " + (currentPlayer == 1 ? 2 : 1) + " for " + damage + " DMG!");
        
        if (target.hp <= 0) {
            target.hp = 0;
            updateUI();
            Toast.makeText(this, "PLAYER " + currentPlayer + " IS THE SURVIVOR!", Toast.LENGTH_LONG).show();
            finish();
        }
        useAction("");
    }

    private void showUpgradeOverlay() {
        upgradeOverlay.setVisibility(View.VISIBLE);
        List<String> options = new ArrayList<>();
        options.add("DMG +2");
        options.add("RANGE +1");
        options.add("MOVE +1");
        options.add("HEAL +10 HP");
        options.add("MAX HP +5");
        Collections.shuffle(options);

        for (int i = 0; i < 3; i++) {
            final String choice = options.get(i);
            upgradeButtons[i].setText(choice);
            upgradeButtons[i].setOnClickListener(v -> {
                applyUpgrade(choice);
                upgradeOverlay.setVisibility(View.GONE);
                getCurrentPlayer().exp -= 6;
                getCurrentPlayer().level++;
                updateUI();
                drawBoard();
            });
        }
    }

    private void applyUpgrade(String type) {
        Player p = getCurrentPlayer();
        if (type.contains("DMG")) p.damageModifier += 2;
        else if (type.contains("RANGE")) p.rangeModifier += 1;
        else if (type.contains("MOVE")) p.movementModifier += 1;
        else if (type.contains("HEAL")) p.hp = Math.min(p.maxHp, p.hp + 10);
        else if (type.contains("MAX HP")) { p.maxHp += 5; p.hp += 5; }
        Toast.makeText(this, "Upgraded: " + type, Toast.LENGTH_SHORT).show();
    }

    private void useAction(String msg) {
        actionsLeft--;
        isMoveMode = false;
        isAttackMode = false;
        if (!msg.isEmpty()) logText.setText(msg);
        
        if (actionsLeft == 0) {
            endTurn();
        } else {
            drawBoard();
            updateUI();
        }
    }

    private void endTurn() {
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        actionsLeft = 2;
        turnCounter++;
        
        moveMonsters();
        
        // Spawn a new monster every 2 turns
        if (turnCounter % 2 == 0 && monsters.size() < 15) {
            spawnMonster();
        }
        
        drawBoard();
        updateUI();
        Toast.makeText(this, "Player " + currentPlayer + "'s Turn", Toast.LENGTH_SHORT).show();
    }

    private void moveMonsters() {
        for (Monster m : monsters) {
            // Find nearest player
            Player target = (getDist(m.x, m.y, player1.x, player1.y) < getDist(m.x, m.y, player2.x, player2.y)) ? player1 : player2;
            
            int dx = Integer.compare(target.x, m.x);
            int dy = Integer.compare(target.y, m.y);
            
            int nx = m.x + dx;
            int ny = m.y + dy;

            // Attack player if adjacent
            if ((nx == player1.x && ny == player1.y) || (nx == player2.x && ny == player2.y)) {
                Player hit = (nx == player1.x && ny == player1.y) ? player1 : player2;
                hit.hp -= m.damage;
                logText.setText("A Monster bit Player " + (hit == player1 ? 1 : 2) + " for " + m.damage + " DMG!");
            } else if (isEmpty(nx, ny)) {
                m.x = nx;
                m.y = ny;
            }
        }
    }

    private double getDist(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }

    private void highlightRange(int cx, int cy, int range, int color) {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                int dist = Math.abs(cx - i) + Math.abs(cy - j);
                if (dist <= range && dist > 0) {
                    cells[i][j].setBackgroundColor(color);
                }
            }
        }
    }

    private void updateUI() {
        statusText.setText("Player " + currentPlayer + " Turn (" + actionsLeft + " actions)");
        p1Stats.setText("P1 HP: " + player1.hp + "/" + player1.maxHp);
        p2Stats.setText("P2 HP: " + player2.hp + "/" + player2.maxHp);
        
        expBar.setProgress(getCurrentPlayer().exp);
        expBar.setProgressTintList(android.content.res.ColorStateList.valueOf(currentPlayer == 1 ? Color.CYAN : Color.RED));
    }

    private void drawBoard() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                cells[i][j].setText("");
                cells[i][j].setBackgroundColor(Color.parseColor("#222222"));
                cells[i][j].setAlpha(1.0f);
            }
        }

        // Draw Player 1 (Gunner Blue)
        cells[player1.x][player1.y].setText("P1\n🔫");
        cells[player1.x][player1.y].setBackgroundColor(Color.parseColor("#004466"));
        
        // Draw Player 2 (Gunner Red)
        cells[player2.x][player2.y].setText("P2\n🔫");
        cells[player2.x][player2.y].setBackgroundColor(Color.parseColor("#660000"));

        // Draw Monsters
        for (Monster m : monsters) {
            cells[m.x][m.y].setText("🧟\n" + m.hp);
            cells[m.x][m.y].setBackgroundColor(Color.parseColor("#224422"));
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
