package com.example.monstergrid;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
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
    private List<Obstacle> obstacles = new ArrayList<>();
    private int currentPlayer = 1;
    private int turnCounter = 0;
    private boolean isMoveMode = false;
    private boolean isAttackMode = false;
    private boolean isAnimating = false;

    private TextView statusText, p1Stats, p2Stats, logText;
    private ProgressBar expBar;
    private Button btnMove, btnAttack;
    private View upgradeOverlay, mainRoot;
    private FrameLayout effectLayer;
    private Button[] upgradeButtons = new Button[3];
    private Random random = new Random();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainRoot = findViewById(R.id.mainRoot);
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
                if (GameRules.isValidPlayerMove(p.x, p.y, i, j, p.movementModifier) && isEmpty(i, j) && isPathClear(p.x, p.y, i, j)) {
                    cells[i][j].setBackgroundColor(Color.parseColor("#44AAFFAA"));
                }
            }
        }
    }

    private void highlightAttackRange() {
        Player p = getCurrentPlayer();
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                if (GameRules.isValidPlayerAttack(p.x, p.y, i, j, p.rangeModifier) && isPathClear(p.x, p.y, i, j)) {
                    cells[i][j].setBackgroundColor(Color.parseColor("#44FFAAAA"));
                }
            }
        }
    }

    private void setupGrid() {
        GridLayout gridLayout = findViewById(R.id.gameGrid);
        int displayWidth = getResources().getDisplayMetrics().widthPixels;
        int availableWidth = displayWidth - 48; 
        int cellSize = availableWidth / GRID_SIZE;

        gridLayout.removeAllViews();
        gridLayout.setColumnCount(GRID_SIZE);
        gridLayout.setRowCount(GRID_SIZE);

        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                TextView cell = new TextView(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                        GridLayout.spec(i, GridLayout.FILL),
                        GridLayout.spec(j, GridLayout.FILL)
                );
                params.width = cellSize;
                params.height = cellSize;
                params.setMargins(1, 1, 1, 1);
                
                cell.setLayoutParams(params);
                cell.setBackgroundColor(Color.parseColor("#1A1A1A"));
                cell.setGravity(Gravity.CENTER);
                cell.setTextSize(14);
                cell.setTextColor(Color.WHITE);
                cell.setIncludeFontPadding(false);
                cell.setLineSpacing(0, 0.9f);
                
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
        
        // 8 Obstacles
        for (int i = 0; i < 8; i++) spawnObstacle();
        
        for (int i = 0; i < 8; i++) spawnMonster();
        drawBoard();
    }

    private void spawnObstacle() {
        int rx, ry;
        do {
            rx = random.nextInt(GRID_SIZE);
            ry = random.nextInt(GRID_SIZE);
        } while (!isEmpty(rx, ry) || isAtStart(rx, ry));
        obstacles.add(new Obstacle(rx, ry));
    }

    private boolean isAtStart(int x, int y) {
        return (Math.abs(x - 0) < 2 && Math.abs(y - 4) < 2) || (Math.abs(x - 9) < 2 && Math.abs(y - 4) < 2);
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
            if (GameRules.isValidPlayerMove(p.x, p.y, r, c, p.movementModifier) && isEmpty(r, c) && isPathClear(p.x, p.y, r, c)) {
                animatePlayerMove(p, r, c);
            }
        } else if (isAttackMode && !p.hasAttacked) {
            if (GameRules.isValidPlayerAttack(p.x, p.y, r, c, p.rangeModifier) && isPathClear(p.x, p.y, r, c)) {
                if (isMonsterAt(r, c)) attackMonster(r, c);
                else if (isOpponentAt(r, c)) attackOpponent(r, c);
            }
        }
    }

    private boolean isPathClear(int x1, int y1, int x2, int y2) {
        int dx = Integer.compare(x2, x1);
        int dy = Integer.compare(y2, y1);
        int x = x1 + dx;
        int y = y1 + dy;
        while (x != x2 || y != y2) {
            if (!isShootThrough(x, y)) return false;
            x += dx;
            y += dy;
        }
        return true;
    }

    private boolean isShootThrough(int x, int y) {
        // Can't shoot through or walk through obstacles, monsters, or other player
        for (Obstacle o : obstacles) if (o.x == x && o.y == y) return false;
        for (Monster m : monsters) if (m.x == x && m.y == y) return false;
        if (player1.x == x && player1.y == y) return false;
        if (player2.x == x && player2.y == y) return false;
        return true;
    }

    private void animatePlayerMove(Player p, int tr, int tc) {
        clearHighlights();
        isAnimating = true;
        isMoveMode = false;
        String tag = (currentPlayer == 1) ? "P1\n🔫" : "P2\n🔫";
        int color = (currentPlayer == 1) ? Color.parseColor("#004466") : Color.parseColor("#660000");
        
        int oldX = p.x, oldY = p.y;
        p.x = tr; p.y = tc;

        GridAnimationManager.animateStationaryToTarget(cells[oldX][oldY], cells[tr][tc], tag, color, effectLayer, () -> {
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
            boolean isCrit = random.nextInt(100) < p.critChance;
            int damage = (roll + p.damageModifier) * (isCrit ? 2 : 1);
            m.hp -= damage;
            p.hasAttacked = true;

            if (m.hp <= 0) {
                monsters.remove(m);
                cells[r][c].setText("");
                cells[r][c].setBackgroundColor(Color.parseColor("#1A1A1A"));
                p.exp += 3;
                if (p.canLevelUp()) showUpgradeOverlay();
            } else {
                cells[r][c].setText("🧟\n" + m.hp);
            }

            isAnimating = false;
            String msg = (isCrit ? "CRIT! " : "") + "Shot for " + damage + " DMG!";
            useAction(msg);
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
            boolean isCrit = random.nextInt(100) < p.critChance;
            int damage = Math.max(0, (roll + p.damageModifier) * (isCrit ? 2 : 1) - target.armor);
            target.hp -= damage;
            p.hasAttacked = true;

            updateUI();
            if (target.hp <= 0) {
                target.hp = 0;
                Toast.makeText(this, "GAME OVER!", Toast.LENGTH_LONG).show();
                finish();
            }
            isAnimating = false;
            String msg = (isCrit ? "CRIT! " : "") + "Hit Player for " + damage + " DMG!";
            useAction(msg);
        });
    }

    private void clearHighlights() {
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                cells[i][j].setBackgroundColor(Color.parseColor("#1A1A1A"));
            }
        }
        cells[player1.x][player1.y].setBackgroundColor(Color.parseColor("#004466"));
        cells[player2.x][player2.y].setBackgroundColor(Color.parseColor("#660000"));
        for (Monster m : monsters) {
            cells[m.x][m.y].setBackgroundColor(Color.parseColor("#224422"));
        }
        for (Obstacle o : obstacles) {
            cells[o.x][o.y].setBackgroundColor(Color.parseColor("#444444"));
        }
    }

    private void showUpgradeOverlay() {
        upgradeOverlay.setVisibility(View.VISIBLE);
        List<String> options = new ArrayList<>();
        options.add("DMG +2"); 
        options.add("RANGE +1"); 
        options.add("MOVE +1");
        options.add("HEAL +10 HP"); 
        options.add("CRIT +20%"); 
        options.add("ARMOR +1");
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
        else if (type.contains("CRIT")) p.critChance += 20;
        else if (type.contains("ARMOR")) p.armor += 1;
    }

    private void useAction(String msg) {
        logText.setText(msg);
        if (getCurrentPlayer().hasMoved && getCurrentPlayer().hasAttacked) {
            mainHandler.postDelayed(this::endTurn, 400);
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
        GridAnimationManager.hideTurnIndicator();
        processMonsterSequence(0);
    }

    private void processMonsterSequence(int index) {
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
            GridAnimationManager.animateMeleeAttack(cells[m.x][m.y], cells[target.x][target.y], () -> {
                int damage = Math.max(0, m.damage - target.armor);
                target.hp -= damage;
                updateUI();
                processMonsterSequence(index + 1);
            });
        } else if (isEmpty(next[0], next[1])) {
            int oldX = m.x, oldY = m.y;
            m.x = next[0]; m.y = next[1];
            GridAnimationManager.animateStationaryToTarget(cells[oldX][oldY], cells[m.x][m.y], "🧟\n" + m.hp, Color.parseColor("#224422"), effectLayer, () -> {
                processMonsterSequence(index + 1);
            });
        } else {
            processMonsterSequence(index + 1);
        }
    }

    private double getDist(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }

    private void updateUI() {
        Player p = getCurrentPlayer();
        statusText.setText("Player " + currentPlayer + " Turn");
        p1Stats.setText("P1 HP: " + player1.hp + (player1.armor > 0 ? " (🛡️" + player1.armor + ")" : ""));
        p2Stats.setText("P2 HP: " + player2.hp + (player2.armor > 0 ? " (🛡️" + player2.armor + ")" : ""));
        btnMove.setEnabled(!p.hasMoved && !isAnimating);
        btnAttack.setEnabled(!p.hasAttacked && !isAnimating);
        expBar.setProgress(p.exp);

        if (currentPlayer == 1) {
            mainRoot.setBackgroundResource(R.drawable.gradient_player1);
        } else {
            mainRoot.setBackgroundResource(R.drawable.gradient_player2);
        }

        if (!isAnimating) {
            GridAnimationManager.updateTurnIndicator(cells[p.x][p.y], effectLayer);
        }
    }

    private void drawBoard() {
        if (isAnimating) return;
        for (int i = 0; i < GRID_SIZE; i++) {
            for (int j = 0; j < GRID_SIZE; j++) {
                cells[i][j].setText("");
                cells[i][j].setBackgroundColor(Color.parseColor("#1A1A1A"));
                cells[i][j].setScaleX(1.0f); cells[i][j].setScaleY(1.0f);
                cells[i][j].setTranslationX(0); cells[i][j].setTranslationY(0);
                cells[i][j].setAlpha(1.0f);
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
        for (Obstacle o : obstacles) {
            cells[o.x][o.y].setText("🗿");
            cells[o.x][o.y].setBackgroundColor(Color.parseColor("#444444"));
        }
    }

    private Player getCurrentPlayer() { return (currentPlayer == 1) ? player1 : player2; }
    
    private boolean isEmpty(int r, int c) {
        if (player1.x == r && player1.y == c || player2.x == r && player2.y == c) return false;
        for (Monster m : monsters) if (m.x == r && m.y == c) return false;
        for (Obstacle o : obstacles) if (o.x == r && o.y == c) return false;
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
