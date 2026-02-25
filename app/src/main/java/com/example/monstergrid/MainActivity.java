package com.example.monstergrid;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.FrameLayout;
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
    private int turnCounter = 0;
    private boolean isMoveMode = false;
    private boolean isAttackMode = false;
    private boolean isAnimating = false;

    private TextView statusText, p1Stats, p2Stats, logText;
    private ProgressBar expBar;
    private Button btnMove, btnAttack;
    private View upgradeOverlay;
    private FrameLayout effectLayer;
    private Button[] upgradeButtons = new Button[3];
    private Random random = new Random();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

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
        effectLayer = findViewById(R.id.effectLayer);
        upgradeButtons[0] = findViewById(R.id.upgrade1);
        upgradeButtons[1] = findViewById(R.id.upgrade2);
        upgradeButtons[2] = findViewById(R.id.upgrade3);

        setupGrid();
        initGame();

        btnMove.setOnClickListener(v -> {
            if (isAnimating || getCurrentPlayer().hasMoved) return;
            isMoveMode = !isMoveMode;
            isAttackMode = false;
            drawBoard();
            if (isMoveMode) {
                logText.setText("Move (HV only): " + getCurrentPlayer().movementModifier);
                highlightMoveRange();
            }
        });

        btnAttack.setOnClickListener(v -> {
            if (isAnimating || getCurrentPlayer().hasAttacked) return;
            isAttackMode = !isAttackMode;
            isMoveMode = false;
            drawBoard();
            if (isAttackMode) {
                logText.setText("Attack (HV+Diag): " + getCurrentPlayer().rangeModifier);
                highlightAttackRange();
            }
        });

        findViewById(R.id.btnEndTurn).setOnClickListener(v -> {
            if (isAnimating) return;
            endTurn();
        });

        updateUI();
    }

    private void highlightMoveRange() {
        Player p = getCurrentPlayer();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (GameRules.isValidPlayerMove(p.x, p.y, i, j, p.movementModifier)) {
                    cells[i][j].setBackgroundColor(Color.parseColor("#44AAFFAA"));
                }
            }
        }
    }

    private void highlightAttackRange() {
        Player p = getCurrentPlayer();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (GameRules.isValidPlayerAttack(p.x, p.y, i, j, p.rangeModifier)) {
                    cells[i][j].setBackgroundColor(Color.parseColor("#44FFAAAA"));
                }
            }
        }
    }

    private void setupGrid() {
        GridLayout gridLayout = findViewById(R.id.gameGrid);
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        int cellSize = (displayWidth - 40) / GRID_SIZE; // Adjust for padding

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(1, 1, 1, 1);
                cell.setLayoutParams(params);
                cell.setBackgroundColor(Color.parseColor("#1A1A1A"));
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
        for (int i = 0; i < 8; i++) spawnMonster();
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
        if (isAnimating || upgradeOverlay.getVisibility() == View.VISIBLE) return;

        Player p = getCurrentPlayer();

        if (isMoveMode && !p.hasMoved) {
            if (GameRules.isValidPlayerMove(p.x, p.y, r, c, p.movementModifier) && isEmpty(r, c)) {
                animatePlayerMove(p, r, c);
            } else {
                Toast.makeText(this, "Invalid move!", Toast.LENGTH_SHORT).show();
            }
        } else if (isAttackMode && !p.hasAttacked) {
            if (GameRules.isValidPlayerAttack(p.x, p.y, r, c, p.rangeModifier)) {
                if (isMonsterAt(r, c)) attackMonster(r, c);
                else if (isOpponentAt(r, c)) attackOpponent(r, c);
            }
        }
    }

    private void animatePlayerMove(Player p, int tr, int tc) {
        clearHighlights();
        isAnimating = true;
        isMoveMode = false;
        String tag = (currentPlayer == 1) ? "P1\n🔫" : "P2\n🔫";
        int color = (currentPlayer == 1) ? Color.parseColor("#004466") : Color.parseColor("#660000");
        
        GridAnimationManager.animateStationaryToTarget(cells[p.x][p.y], cells[tr][tc], tag, color, () -> {
            p.x = tr;
            p.y = tc;
            p.hasMoved = true;
            isAnimating = false;
            useAction("Moved!");
        });
    }

    private void attackMonster(int r, int c) {
        clearHighlights();
        isAnimating = true;
        isAttackMode = false;
        Player p = getCurrentPlayer();
        Monster m = getMonsterAt(r, c);

        GridAnimationManager.animateProjectile(cells[p.x][p.y], cells[r][c], effectLayer, () -> {
            int roll = random.nextInt(6) + 1;
            int damage = roll + p.damageModifier;
            m.hp -= damage;
            p.hasAttacked = true;

            if (m.hp <= 0) {
                monsters.remove(m);
                p.exp += 3;
                if (p.canLevelUp()) showUpgradeOverlay();
            }
            isAnimating = false;
            useAction("Shot for " + damage + " DMG!");
        });
    }

    private void attackOpponent(int r, int c) {
        clearHighlights();
        isAnimating = true;
        isAttackMode = false;
        Player p = getCurrentPlayer();
        Player target = (currentPlayer == 1) ? player2 : player1;

        GridAnimationManager.animateProjectile(cells[p.x][p.y], cells[r][c], effectLayer, () -> {
            int roll = random.nextInt(6) + 1;
            int damage = roll + p.damageModifier;
            target.hp -= damage;
            p.hasAttacked = true;

            if (target.hp <= 0) {
                target.hp = 0;
                Toast.makeText(this, "GAME OVER! PLAYER " + currentPlayer + " WINS!", Toast.LENGTH_LONG).show();
                finish();
            }
            isAnimating = false;
            useAction("Hit Player for " + damage + " DMG!");
        });
    }

    private void clearHighlights() {
        isMoveMode = false;
        isAttackMode = false;
        // Direct color reset to ensure highlights are gone immediately
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                cells[i][j].setBackgroundColor(Color.parseColor("#1A1A1A"));
            }
        }
        // Redraw essential elements without highlights
        cells[player1.x][player1.y].setBackgroundColor(Color.parseColor("#004466"));
        cells[player2.x][player2.y].setBackgroundColor(Color.parseColor("#660000"));
        for (Monster m : monsters) {
            cells[m.x][m.y].setBackgroundColor(Color.parseColor("#224422"));
        }
    }

    private void showUpgradeOverlay() {
        upgradeOverlay.setVisibility(View.VISIBLE);
        List<String> options = new ArrayList<>();
        options.add("DMG +2"); options.add("RANGE +1"); options.add("MOVE +1");
        options.add("HEAL +10 HP"); options.add("MAX HP +5");
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
    }

    private void useAction(String msg) {
        logText.setText(msg);
        if (getCurrentPlayer().hasMoved && getCurrentPlayer().hasAttacked) {
            mainHandler.postDelayed(this::endTurn, 600);
        } else {
            drawBoard();
            updateUI();
        }
    }

    private void endTurn() {
        if (isAnimating) return;
        clearHighlights();
        getCurrentPlayer().resetTurn();
        currentPlayer = (currentPlayer == 1) ? 2 : 1;
        turnCounter++;
        moveMonstersAnimated();
    }

    private void moveMonstersAnimated() {
        isAnimating = true;
        animateNextMonster(0);
    }

    private void animateNextMonster(int index) {
        if (index >= monsters.size()) {
            isAnimating = false;
            drawBoard();
            updateUI();
            return;
        }

        final Monster m = monsters.get(index);
        Player target = (getDist(m.x, m.y, player1.x, player1.y) < getDist(m.x, m.y, player2.x, player2.y)) ? player1 : player2;
        int[] next = GameRules.getMonsterMove(m.x, m.y, target.x, target.y);
        
        if (next[0] == target.x && next[1] == target.y) {
            target.hp -= m.damage;
            Animation anim = new AlphaAnimation(1.0f, 0.4f);
            anim.setDuration(250);
            cells[m.x][m.y].startAnimation(anim);
            mainHandler.postDelayed(() -> animateNextMonster(index + 1), 400);
        } else if (isEmpty(next[0], next[1])) {
            int oldX = m.x;
            int oldY = m.y;
            m.x = next[0];
            m.y = next[1];
            
            GridAnimationManager.animateStationaryToTarget(cells[oldX][oldY], cells[m.x][m.y], "🧟\n" + m.hp, Color.parseColor("#224422"), () -> {
                animateNextMonster(index + 1);
            });
        } else {
            // Monster is blocked, skip to next monster immediately but with a small delay for visual clarity
            mainHandler.postDelayed(() -> animateNextMonster(index + 1), 50);
        }
    }

    private double getDist(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }

    private void updateUI() {
        Player p = getCurrentPlayer();
        statusText.setText("Player " + currentPlayer + " Turn");
        p1Stats.setText("P1 HP: " + player1.hp);
        p2Stats.setText("P2 HP: " + player2.hp);
        btnMove.setEnabled(!p.hasMoved && !isAnimating);
        btnAttack.setEnabled(!p.hasAttacked && !isAnimating);
        expBar.setProgress(p.exp);
    }

    private void drawBoard() {
        if (isAnimating) return;
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                cells[i][j].setText("");
                cells[i][j].setBackgroundColor(Color.parseColor("#1A1A1A"));
            }
        }
        cells[player1.x][player1.y].setText("P1\n🔫");
        cells[player1.x][player1.y].setBackgroundColor(Color.parseColor("#004466"));
        cells[player2.x][player2.y].setText("P2\n🔫");
        cells[player2.x][player2.y].setBackgroundColor(Color.parseColor("#660000"));
        for (Monster m : monsters) {
            cells[m.x][m.y].setText("🧟\n" + m.hp);
            cells[m.x][m.y].setBackgroundColor(Color.parseColor("#224422"));
        }
    }

    private Player getCurrentPlayer() { return (currentPlayer == 1) ? player1 : player2; }
    private boolean isEmpty(int r, int c) {
        if (player1.x == r && player1.y == c || player2.x == r && player2.y == c) return false;
        for (Monster m : monsters) if (m.x == r && m.y == c) return false;
        return true;
    }
    private boolean isMonsterAt(int r, int c) { return getMonsterAt(r, c) != null; }
    private Monster getMonsterAt(int r, int c) {
        for (Monster m : monsters) if (m.x == r && m.y == c) return m;
        return null;
    }
    private boolean isOpponentAt(int r, int c) {
        Player opp = (currentPlayer == 1) ? player2 : player1;
        return opp.x == r && opp.y == c;
    }
}
