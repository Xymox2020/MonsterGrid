package com.example.monstergrid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private int gridSize = 10;
    private int minMonsters = 4;
    private TextView[][] cells;
    private List<Player> players = new ArrayList<>();
    private List<Monster> monsters = new ArrayList<>();
    private List<Obstacle> obstacles = new ArrayList<>();
    private List<Collectable> collectables = new ArrayList<>();
    private int currentPlayerIndex = 0; // 0-indexed
    private int turnCounter = 0;
    private int totalTurnsCounter = 0;
    private boolean isMoveMode = false;
    private boolean isAttackMode = false;
    private boolean isAnimating = false;
    private int numPlayers = 2;
    private int nextSpawnQuadrantIndex = 0;
    private int totalMonstersSpawned = 0;
    private int monstersInitiallySpawned = 0;

    private TextView statusText, logText, winnerName;
    private TextView[] playerStatsTexts = new TextView[4];
    private View[] playerContainers = new View[4];
    private View[] playerDividers = new View[2]; // P3 and P4 dividers
    private ProgressBar expBar;
    private Button btnMove, btnAttack;
    private View upgradeOverlay, mainRoot, gameOverOverlay, selectionOverlay;
    private FrameLayout effectLayer, particleContainer;
    private ShimmerImageView[] upgradeImages = new ShimmerImageView[3];
    private Random random = new Random();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isParticleSystemActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainRoot = findViewById(R.id.mainRoot);
        statusText = findViewById(R.id.statusText);
        playerStatsTexts[0] = findViewById(R.id.p1Stats);
        playerStatsTexts[1] = findViewById(R.id.p2Stats);
        playerStatsTexts[2] = findViewById(R.id.p3Stats);
        playerStatsTexts[3] = findViewById(R.id.p4Stats);
        
        playerContainers[2] = findViewById(R.id.p3Container);
        playerContainers[3] = findViewById(R.id.p4Container);
        playerDividers[0] = findViewById(R.id.dividerP3);
        playerDividers[1] = findViewById(R.id.dividerP4);

        logText = findViewById(R.id.logText);
        expBar = findViewById(R.id.expBar);
        btnMove = findViewById(R.id.btnAction1);
        btnAttack = findViewById(R.id.btnAction2);
        upgradeOverlay = findViewById(R.id.upgradeOverlay);
        selectionOverlay = findViewById(R.id.selectionOverlay);
        effectLayer = findViewById(R.id.effectLayer);
        particleContainer = findViewById(R.id.particleContainer);
        gameOverOverlay = findViewById(R.id.gameOverOverlay);
        winnerName = findViewById(R.id.winnerName);
        
        upgradeImages[0] = findViewById(R.id.upgrade1);
        upgradeImages[1] = findViewById(R.id.upgrade2);
        upgradeImages[2] = findViewById(R.id.upgrade3);

        findViewById(R.id.btn2Players).setOnClickListener(v -> startGame(2));
        findViewById(R.id.btn3Players).setOnClickListener(v -> startGame(3));
        findViewById(R.id.btn4Players).setOnClickListener(v -> startGame(4));

        btnMove.setOnClickListener(v -> {
            if (isAnimating || getCurrentPlayer().hasMoved) return;
            isMoveMode = !isMoveMode;
            isAttackMode = false;
            drawBoard();
            if (isMoveMode) {
                logText.setText("Move range: " + getCurrentPlayer().movementModifier);
                highlightMoveRange();
            }
        });

        btnAttack.setOnClickListener(v -> {
            if (isAnimating || getCurrentPlayer().hasAttacked) return;
            isAttackMode = !isAttackMode;
            isMoveMode = false;
            drawBoard();
            if (isAttackMode) {
                logText.setText("Attack range: " + getCurrentPlayer().rangeModifier);
                highlightAttackRange();
            }
        });

        findViewById(R.id.btnEndTurn).setOnClickListener(v -> {
            if (isAnimating) return;
            endTurn();
        });

        gameOverOverlay.setOnClickListener(v -> {
            selectionOverlay.setVisibility(View.VISIBLE);
            gameOverOverlay.setVisibility(View.GONE);
        });
    }

    private void startGame(int count) {
        numPlayers = count;
        if (numPlayers == 2) gridSize = 10;
        else if (numPlayers == 3) gridSize = 12;
        else gridSize = 14;
        
        selectionOverlay.setVisibility(View.GONE);
        setupGrid();
        initGame();
    }

    private void highlightMoveRange() {
        Player p = getCurrentPlayer();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                if (GameRules.isValidPlayerMove(p.x, p.y, i, j, p.movementModifier) && (isEmpty(i, j) || isCollectableAt(i, j)) && !isMonsterAt(i, j) && isPathClear(p.x, p.y, i, j)) {
                    cells[i][j].setBackgroundColor(Color.parseColor("#44AAFFAA"));
                }
            }
        }
    }

    private void highlightAttackRange() {
        Player p = getCurrentPlayer();
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
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
        int cellSize = availableWidth / gridSize;

        gridLayout.removeAllViews();
        gridLayout.setColumnCount(gridSize);
        gridLayout.setRowCount(gridSize);
        cells = new TextView[gridSize][gridSize];

        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
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
                cell.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(numPlayers > 2 ? R.dimen.grid_text_p3_p4 : R.dimen.grid_text_p1_p2)); 
                cell.setTextColor(Color.WHITE);
                cell.setIncludeFontPadding(false);
                
                final int row = i;
                final int col = j;
                cell.setOnClickListener(v -> handleCellClick(row, col));
                
                cells[i][j] = cell;
                gridLayout.addView(cell);
            }
        }
    }

    private void initGame() {
        players.clear();
        if (numPlayers == 2) {
            players.add(new Player(0, gridSize / 2));
            players.add(new Player(gridSize - 1, gridSize / 2));
        } else if (numPlayers == 3) {
            players.add(new Player(1, 1));
            players.add(new Player(1, gridSize - 2));
            players.add(new Player(gridSize - 2, gridSize / 2));
        } else {
            players.add(new Player(1, 1));
            players.add(new Player(1, gridSize - 2));
            players.add(new Player(gridSize - 2, 1));
            players.add(new Player(gridSize - 2, gridSize - 2));
        }

        monsters.clear();
        obstacles.clear();
        collectables.clear();
        currentPlayerIndex = 0;
        turnCounter = 0;
        totalTurnsCounter = 0;
        nextSpawnQuadrantIndex = 0;
        totalMonstersSpawned = 0;
        gameOverOverlay.setVisibility(View.GONE);
        upgradeOverlay.setVisibility(View.GONE);
        
        for (int i = 2; i < 4; i++) {
            if (i < numPlayers) {
                playerContainers[i].setVisibility(View.VISIBLE);
                playerDividers[i-2].setVisibility(View.VISIBLE);
            } else {
                playerContainers[i].setVisibility(View.GONE);
                playerDividers[i-2].setVisibility(View.GONE);
            }
        }

        int obstacleCount = (int) (gridSize * gridSize * 0.08);
        int initialMonsterCount;
        
        if (numPlayers == 2) {
            minMonsters = 4;
            initialMonsterCount = 6;
        } else if (numPlayers == 3) {
            minMonsters = 6;
            initialMonsterCount = 9;
        } else {
            minMonsters = 8;
            initialMonsterCount = 12;
        }
        monstersInitiallySpawned = initialMonsterCount;

        for (int i = 0; i < obstacleCount; i++) {
            spawnObstacleInQuadrant(i % 4);
        }
        
        for (int i = 0; i < initialMonsterCount; i++) {
            spawnMonsterInQuadrant(i % 4);
        }
        
        drawBoard();
        updateUI();
    }

    private void spawnObstacleInQuadrant(int qIdx) {
        int rx, ry;
        int attempts = 0;
        int startX = (qIdx < 2) ? 0 : gridSize / 2;
        int startY = (qIdx % 2 == 0) ? 0 : gridSize / 2;
        
        do {
            rx = startX + random.nextInt(gridSize / 2);
            ry = startY + random.nextInt(gridSize / 2);
            
            attempts++;
            if (attempts > 50) {
                rx = random.nextInt(gridSize);
                ry = random.nextInt(gridSize);
            }
        } while ((!isEmpty(rx, ry) || isAtStart(rx, ry)) && attempts < 100);
        
        if (attempts < 100) {
            obstacles.add(new Obstacle(rx, ry));
        }
    }

    private boolean isAtStart(int x, int y) {
        for (Player p : players) {
            if (Math.abs(x - p.x) < 2 && Math.abs(y - p.y) < 2) return true;
        }
        return false;
    }

    private void maintainMonsterCount() {
        while (monsters.size() < minMonsters) {
            spawnMonsterInQuadrant(nextSpawnQuadrantIndex);
            nextSpawnQuadrantIndex = (nextSpawnQuadrantIndex + 1) % 4;
        }
    }

    private void spawnMonsterInQuadrant(int qIdx) {
        int rx, ry;
        boolean tooClose;
        int attempts = 0;
        int startX = (qIdx < 2) ? 0 : gridSize / 2;
        int startY = (qIdx % 2 == 0) ? 0 : gridSize / 2;
        
        do {
            rx = startX + random.nextInt(gridSize / 2);
            ry = startY + random.nextInt(gridSize / 2);
            
            tooClose = false;
            // Check if too close to ANY player (min 3 tiles distance)
            for (Player pl : players) {
                if (pl.hp > 0 && Math.abs(rx - pl.x) < 3 && Math.abs(ry - pl.y) < 3) {
                    tooClose = true;
                    break;
                }
            }
            
            attempts++;
            if (attempts > 50) { // Fallback: random grid position if local area is full
                rx = random.nextInt(gridSize);
                ry = random.nextInt(gridSize);
                tooClose = false;
                for (Player pl : players) {
                    if (pl.hp > 0 && Math.abs(rx - pl.x) < 3 && Math.abs(ry - pl.y) < 3) {
                        tooClose = true;
                        break;
                    }
                }
            }
        } while ((!isEmpty(rx, ry) || tooClose) && attempts < 100);
        
        if (attempts < 100) {
            int level;
            if (totalMonstersSpawned < monstersInitiallySpawned) {
                level = 0;
            } else {
                int numNew = totalMonstersSpawned - monstersInitiallySpawned;
                level = 1 + (numNew / numPlayers);
            }
            monsters.add(new Monster(rx, ry, level));
            totalMonstersSpawned++;
        }
    }

    private void handleCellClick(int r, int c) {
        if (isAnimating || upgradeOverlay.getVisibility() == View.VISIBLE || gameOverOverlay.getVisibility() == View.VISIBLE) return;
        Player p = getCurrentPlayer();
        if (isMoveMode && !p.hasMoved) {
            if (GameRules.isValidPlayerMove(p.x, p.y, r, c, p.movementModifier) && (isEmpty(r, c) || isCollectableAt(r, c)) && !isMonsterAt(r, c) && isPathClear(p.x, p.y, r, c)) {
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
        for (Obstacle o : obstacles) if (o.x == x && o.y == y) return false;
        for (Monster m : monsters) if (m.x == x && m.y == y) return false;
        for (Player p : players) if (p.hp > 0 && p.x == x && p.y == y) return false;
        return true;
    }

    private void animatePlayerMove(Player p, int tr, int tc) {
        clearHighlights();
        isAnimating = true;
        isMoveMode = false;
        String tag = getPlayerTag(currentPlayerIndex, p.hp);
        int color = getPlayerColor(currentPlayerIndex);
        
        int oldX = p.x, oldY = p.y;
        p.x = tr; p.y = tc;

        GridAnimationManager.animateStationaryToTarget(cells[oldX][oldY], cells[tr][tc], tag, color, 
                ContextCompat.getDrawable(this, getPlayerBackgroundResource(currentPlayerIndex)),
                getResources().getDimension(numPlayers > 2 ? R.dimen.grid_text_p3_p4 : R.dimen.grid_text_p1_p2), effectLayer, () -> {
            p.hasMoved = true;
            isAnimating = false;
            
            Collectable col = getCollectableAt(tr, tc);
            if (col != null) {
                handleCollectable(p, col);
            } else {
                useAction("Traversed!");
            }
        });
    }

    private void handleCollectable(Player p, Collectable col) {
        collectables.remove(col);
        if (col.type == Collectable.TYPE_LOOT) {
            logText.setText("Ancient Artifact Found!");
            showUpgradeOverlay(p);
        } else {
            p.hp = Math.min(p.maxHp, p.hp + 5);
            useAction("Picked Holy Berry! +5 HP");
        }
    }

    private String getPlayerTag(int index, int hp) {
        String icon = "";
        switch (index) {
            case 0: icon = "⚔️"; break;
            case 1: icon = "🏹"; break;
            case 2: icon = "🧙"; break;
            case 3: icon = "🛡️"; break;
        }
        return icon + "\n" + hp;
    }

    private int getPlayerColor(int index) {
        switch (index) {
            case 0: return Color.parseColor("#4A0E0E");
            case 1: return Color.parseColor("#0E4A0E");
            case 2: return Color.parseColor("#0E0E4A");
            case 3: return Color.parseColor("#4A4A0E");
            default: return Color.GRAY;
        }
    }

    private int getPlayerBackgroundResource(int index) {
        switch (index) {
            case 0: return R.drawable.player_1_bg;
            case 1: return R.drawable.player_2_bg;
            case 2: return R.drawable.player_3_bg;
            case 3: return R.drawable.player_4_bg;
            default: return 0;
        }
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

            GridAnimationManager.showDamageIndicator(cells[r][c], effectLayer, (isCrit ? "CRIT! " : "") + "-" + damage, Color.YELLOW);

            if (m.hp <= 0) {
                monsters.remove(m);
                cells[r][c].setText("");
                cells[r][c].setBackgroundColor(Color.parseColor("#1A1A1A"));
                cells[r][c].setBackgroundResource(0);
                p.exp += 3;
                maintainMonsterCount();
                if (p.canLevelUp()) showUpgradeOverlay(p);
            } else {
                cells[r][c].setText("🐉\n" + m.hp);
                cells[r][c].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(numPlayers > 2 ? R.dimen.grid_text_monster_p3_p4 : R.dimen.grid_text_monster_p1_p2));
            }

            isAnimating = false;
            useAction("Struck for " + damage + " DMG!");
        });
    }

    private void attackOpponent(int r, int c) {
        clearHighlights();
        isAnimating = true;
        isAttackMode = false;
        Player p = getCurrentPlayer();
        Player target = getPlayerAt(r, c);

        GridAnimationManager.animateProjectile(cells[p.x][p.y], cells[r][c], effectLayer, () -> {
            int roll = random.nextInt(6) + 1;
            boolean isCrit = random.nextInt(100) < p.critChance;
            int damage = Math.max(0, (roll + p.damageModifier) * (isCrit ? 2 : 1) - target.armor);
            target.hp -= damage;
            p.hasAttacked = true;

            GridAnimationManager.showDamageIndicator(cells[r][c], effectLayer, (isCrit ? "CRIT! " : "") + "-" + damage, Color.RED);
            updateUI();
            
            if (target.hp <= 0) {
                target.hp = 0;
                checkWinCondition();
            }
            
            isAnimating = false;
            useAction("Slid Warrior for " + damage + " DMG!");
        });
    }

    private void checkWinCondition() {
        int aliveCount = 0;
        int winnerIdx = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).hp > 0) {
                aliveCount++;
                winnerIdx = i;
            }
        }
        if (aliveCount <= 1) {
            showGameOver("WARRIOR " + (winnerIdx + 1) + " VICTORIOUS");
        } else if (players.get(currentPlayerIndex).hp <= 0) {
            // Current player is dead, skip to next alive player
            do {
                currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            } while (players.get(currentPlayerIndex).hp <= 0);
            updateUI();
        }
    }

    private void showGameOver(String message) {
        winnerName.setText(message);
        gameOverOverlay.setVisibility(View.VISIBLE);
        GridAnimationManager.hideTurnIndicator();
    }

    private void clearHighlights() {
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                cells[i][j].setBackgroundColor(Color.parseColor("#1A1A1A"));
                cells[i][j].setBackgroundResource(0);
                cells[i][j].setBackgroundColor(Color.parseColor("#1A1A1A"));
            }
        }
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p.hp > 0) {
                cells[p.x][p.y].setBackgroundResource(getPlayerBackgroundResource(i));
            }
        }
        for (Monster m : monsters) {
            cells[m.x][m.y].setBackgroundColor(Color.parseColor("#224422"));
        }
        for (Obstacle o : obstacles) {
            cells[o.x][o.y].setBackgroundResource(o.type == 0 ? R.drawable.obstacle_rock : R.drawable.obstacle_tree);
        }
    }

    private void showUpgradeOverlay(Player p) {
        upgradeOverlay.setVisibility(View.VISIBLE);
        GridAnimationManager.hideTurnIndicator();
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
            ShimmerImageView img = upgradeImages[i];
            
            int iconRes = 0;
            if (choice.contains("DMG")) iconRes = R.drawable.dmg_upgrade;
            else if (choice.contains("RANGE")) iconRes = random.nextBoolean() ? R.drawable.range_upgrade1 : R.drawable.range_upgrade2;
            else if (choice.contains("MOVE")) iconRes = R.drawable.movement_upgrade;
            else if (choice.contains("HEAL")) iconRes = R.drawable.fish_upgrade;
            else if (choice.contains("CRIT")) iconRes = R.drawable.crit_upgrade;
            else if (choice.contains("ARMOR")) iconRes = R.drawable.armor_upgrade;

            if (iconRes != 0) {
                img.setImageResource(iconRes);
            }

            img.setOnClickListener(v -> {
                isParticleSystemActive = false;
                particleContainer.removeAllViews();
                for (ShimmerImageView sImg : upgradeImages) sImg.stopShimmer();
                applyUpgrade(p, choice);
                upgradeOverlay.setVisibility(View.GONE);
                if (p.exp >= 6) p.exp -= 6;
                p.level++;
                updateUI();
                drawBoard();
            });
            
            img.startShimmer();
        }
        
        startParticleSystem();
    }
    
    private void startParticleSystem() {
        isParticleSystemActive = true;
        particleContainer.removeAllViews();
        
        // Spawn robust initial batch spread across the screen
        for (int i = 0; i < 40; i++) {
            spawnParticle(true);
        }
        
        final int maxParticles = 60;
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!isParticleSystemActive) return;
                if (particleContainer.getChildCount() < maxParticles) {
                    spawnParticle(false);
                }
                mainHandler.postDelayed(this, 100);
            }
        });
    }

    private void spawnParticle(boolean instant) {
        final ImageView particle = new ImageView(this);
        particle.setImageResource(R.drawable.particle_star);
        int size = 10 + random.nextInt(20);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.gravity = Gravity.CENTER;
        particle.setLayoutParams(params);
        particleContainer.addView(particle);

        float angle = random.nextFloat() * 2 * (float) Math.PI;
        float distance = 250 + random.nextFloat() * 600;
        float tx = (float) Math.cos(angle) * distance;
        float ty = (float) Math.sin(angle) * distance;

        if (instant) {
            float progress = random.nextFloat();
            particle.setTranslationX(tx * progress);
            particle.setTranslationY(ty * progress);
            particle.setAlpha(progress);
            particle.setScaleX(0.5f + progress);
            particle.setScaleY(0.5f + progress);
        } else {
            particle.setAlpha(0f);
            particle.setScaleX(0.5f);
            particle.setScaleY(0.5f);
        }

        particle.animate()
                .translationX(tx)
                .translationY(ty)
                .alpha(1f)
                .scaleX(1.5f)
                .scaleY(1.5f)
                .setDuration(2500 + random.nextInt(2000))
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        particle.animate()
                                .alpha(0f)
                                .scaleX(0f)
                                .scaleY(0f)
                                .setDuration(1000)
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        particleContainer.removeView(particle);
                                    }
                                });
                    }
                });
    }

    private void applyUpgrade(Player p, String type) {
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
        if (isAnimating || (gameOverOverlay != null && gameOverOverlay.getVisibility() == View.VISIBLE)) return;
        clearHighlights();
        getCurrentPlayer().resetTurn();
        
        int prevIndex = currentPlayerIndex;
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        } while (players.get(currentPlayerIndex).hp <= 0);
        
        totalTurnsCounter++;
        if (totalTurnsCounter % numPlayers == 0) {
            turnCounter++;
            checkPeriodicSpawns();
        }

        int mid = numPlayers / 2;
        boolean crossMid = (prevIndex < mid && currentPlayerIndex >= mid);
        boolean crossEnd = (currentPlayerIndex < prevIndex);

        if (crossMid || crossEnd) {
            moveMonstersAnimated();
        } else {
            drawBoard();
            updateUI();
        }
    }

    private void checkPeriodicSpawns() {
        if (turnCounter > 0 && turnCounter % 4 == 0) {
            spawnLootPackage();
        }
        if (turnCounter > 0 && turnCounter % 3 == 0) {
            spawnBerryBush();
        }
    }

    private void spawnLootPackage() {
        Player behind = null;
        int minUpgrades = Integer.MAX_VALUE;
        for (Player p : players) {
            if (p.hp > 0) {
                int score = p.level * 2 + p.exp;
                if (score < minUpgrades) {
                    minUpgrades = score;
                    behind = p;
                }
            }
        }

        int qIdx = (behind != null) ? getPlayerQuadrant(behind) : random.nextInt(4);
        spawnCollectableInQuadrant(qIdx, Collectable.TYPE_LOOT);
    }

    private void spawnBerryBush() {
        spawnCollectableInQuadrant(random.nextInt(4), Collectable.TYPE_BERRY);
    }

    private void spawnCollectableInQuadrant(int qIdx, int type) {
        int rx, ry;
        int attempts = 0;
        int startX = (qIdx < 2) ? 0 : gridSize / 2;
        int startY = (qIdx % 2 == 0) ? 0 : gridSize / 2;
        
        do {
            rx = startX + random.nextInt(gridSize / 2);
            ry = startY + random.nextInt(gridSize / 2);
            attempts++;
        } while ((!isEmpty(rx, ry) || isCollectableAt(rx, ry)) && attempts < 50);
        
        if (attempts < 50) {
            collectables.add(new Collectable(rx, ry, type));
        }
    }

    private int getPlayerQuadrant(Player p) {
        if (p.x < gridSize / 2) {
            return (p.y < gridSize / 2) ? 0 : 1;
        } else {
            return (p.y < gridSize / 2) ? 2 : 3;
        }
    }

    private void moveMonstersAnimated() {
        isAnimating = true;
        GridAnimationManager.hideTurnIndicator();
        processMonsterSequence(0);
    }

    private void processMonsterSequence(int index) {
        if (index >= monsters.size() || (gameOverOverlay != null && gameOverOverlay.getVisibility() == View.VISIBLE)) {
            isAnimating = false;
            drawBoard();
            updateUI();
            return;
        }

        final Monster m = monsters.get(index);
        Player target = null;
        double minDist = Double.MAX_VALUE;
        for (Player p : players) {
            if (p.hp > 0) {
                double d = getDist(m.x, m.y, p.x, p.y);
                if (d < minDist) {
                    minDist = d;
                    target = p;
                }
            }
        }
        
        if (target == null) {
            processMonsterSequence(index + 1);
            return;
        }

        int[] next = PathFinder.getNextStep(m.x, m.y, target.x, target.y, gridSize, obstacles, monsters, players);
        
        if (next != null) {
            if (next[0] == target.x && next[1] == target.y) {
                final Player finalTarget = target;
                GridAnimationManager.animateMeleeAttack(cells[m.x][m.y], cells[target.x][target.y], () -> {
                    int damage = Math.max(0, m.damage - finalTarget.armor);
                    finalTarget.hp -= damage;
                    GridAnimationManager.showDamageIndicator(cells[finalTarget.x][finalTarget.y], effectLayer, "-" + damage, Color.RED);
                    
                    if (finalTarget.hp <= 0) {
                        finalTarget.hp = 0;
                        checkWinCondition();
                        if (gameOverOverlay.getVisibility() == View.VISIBLE) {
                            isAnimating = false;
                            return;
                        }
                    }
                    
                    updateUI();
                    processMonsterSequence(index + 1);
                });
            } else {
                int oldX = m.x, oldY = m.y;
                m.x = next[0]; m.y = next[1];
                GridAnimationManager.animateStationaryToTarget(cells[oldX][oldY], cells[m.x][m.y], "🐉\n" + m.hp, Color.parseColor("#224422"), 
                        getResources().getDimension(numPlayers > 2 ? R.dimen.grid_text_monster_p3_p4 : R.dimen.grid_text_monster_p1_p2), effectLayer, () -> {
                    processMonsterSequence(index + 1);
                });
            }
        } else {
            processMonsterSequence(index + 1);
        }
    }

    private double getDist(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x1-x2, 2) + Math.pow(y1-y2, 2));
    }

    private void updateUI() {
        if (gameOverOverlay != null && gameOverOverlay.getVisibility() == View.VISIBLE) {
            GridAnimationManager.hideTurnIndicator();
            return;
        }

        Player p = getCurrentPlayer();
        statusText.setText("WARRIOR " + (currentPlayerIndex + 1) + " TURN");
        for (int i = 0; i < numPlayers; i++) {
            Player pl = players.get(i);
            playerStatsTexts[i].setText("W" + (i+1) + " HP: " + pl.hp + (pl.armor > 0 ? " (🛡️" + pl.armor + ")" : ""));
        }
        
        btnMove.setEnabled(p.hp > 0 && !p.hasMoved && !isAnimating && (gameOverOverlay == null || gameOverOverlay.getVisibility() == View.GONE) && (upgradeOverlay == null || upgradeOverlay.getVisibility() == View.GONE));
        btnAttack.setEnabled(p.hp > 0 && !p.hasAttacked && !isAnimating && (gameOverOverlay == null || gameOverOverlay.getVisibility() == View.GONE) && (upgradeOverlay == null || upgradeOverlay.getVisibility() == View.GONE));
        expBar.setProgress(p.exp);

        updateBackground(currentPlayerIndex);

        if (!isAnimating && (gameOverOverlay == null || gameOverOverlay.getVisibility() == View.GONE) && (upgradeOverlay == null || upgradeOverlay.getVisibility() == View.GONE) && p.hp > 0) {
            GridAnimationManager.updateTurnIndicator(cells[p.x][p.y], effectLayer);
        } else {
            GridAnimationManager.hideTurnIndicator();
        }
    }

    private void updateBackground(int index) {
        switch (index) {
            case 0: mainRoot.setBackgroundResource(R.drawable.fantasy_bg_p1); break;
            case 1: mainRoot.setBackgroundResource(R.drawable.fantasy_bg_p2); break;
            case 2: mainRoot.setBackgroundColor(Color.parseColor("#0A0F0A")); break;
            case 3: mainRoot.setBackgroundColor(Color.parseColor("#0F0F0A")); break;
        }
    }

    private void drawBoard() {
        if (isAnimating) return;
        for (int i = 0; i < gridSize; i++) {
            for (int j = 0; j < gridSize; j++) {
                cells[i][j].setText("");
                cells[i][j].setBackgroundColor(Color.parseColor("#1A1A1A"));
                cells[i][j].setScaleX(1.0f); cells[i][j].setScaleY(1.0f);
                cells[i][j].setTranslationX(0); cells[i][j].setTranslationY(0);
                cells[i][j].setAlpha(1.0f);
                cells[i][j].setBackgroundResource(0);
                cells[i][j].setBackgroundColor(Color.parseColor("#1A1A1A"));
            }
        }
        
        // 1. Draw Collectables first (so they are at the bottom)
        for (Collectable c : collectables) {
            cells[c.x][c.y].setText(c.type == Collectable.TYPE_LOOT ? "🎁" : "🍓");
            cells[c.x][c.y].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.grid_text_collectable));
        }

        // 2. Draw Obstacles
        for (Obstacle o : obstacles) {
            cells[o.x][o.y].setText("");
            cells[o.x][o.y].setBackgroundResource(o.type == 0 ? R.drawable.obstacle_rock : R.drawable.obstacle_tree);
        }

        // 3. Draw Monsters (Dragons) above collectables
        for (Monster m : monsters) {
            cells[m.x][m.y].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(numPlayers > 2 ? R.dimen.grid_text_monster_p3_p4 : R.dimen.grid_text_monster_p1_p2));
            cells[m.x][m.y].setText("🐉\n" + m.hp);
            cells[m.x][m.y].setBackgroundColor(Color.parseColor("#224422"));
        }

        // 4. Draw Players at the very top
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p.hp > 0) {
                cells[p.x][p.y].setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(numPlayers > 2 ? R.dimen.grid_text_p3_p4 : R.dimen.grid_text_p1_p2));
                cells[p.x][p.y].setText(getPlayerTag(i, p.hp));
                cells[p.x][p.y].setBackgroundResource(getPlayerBackgroundResource(i));
            }
        }
    }

    private Player getCurrentPlayer() { return players.get(currentPlayerIndex); }
    
    private boolean isEmpty(int r, int c) {
        for (Player p : players) if (p.hp > 0 && p.x == r && p.y == c) return false;
        for (Monster m : monsters) if (m.x == r && m.y == c) return false;
        for (Obstacle o : obstacles) if (o.x == r && o.y == c) return false;
        return true;
    }

    private boolean isMonsterAt(int r, int c) {
        for (Monster m : monsters) if (m.x == r && m.y == c) return true;
        return false;
    }

    private Monster getMonsterAt(int r, int c) {
        for (Monster m : monsters) if (m.x == r && m.y == c) return m;
        return null;
    }

    private boolean isOpponentAt(int r, int c) {
        for (int i = 0; i < players.size(); i++) {
            if (i == currentPlayerIndex) continue;
            Player p = players.get(i);
            if (p.hp > 0 && p.x == r && p.y == c) return true;
        }
        return false;
    }

    private Player getPlayerAt(int r, int c) {
        for (Player p : players) if (p.hp > 0 && p.x == r && p.y == c) return p;
        return null;
    }

    private boolean isCollectableAt(int r, int c) {
        for (Collectable cl : collectables) if (cl.x == r && cl.y == c) return true;
        return false;
    }

    private Collectable getCollectableAt(int r, int c) {
        for (Collectable cl : collectables) if (cl.x == r && cl.y == c) return cl;
        return null;
    }
}
