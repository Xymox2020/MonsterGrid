package com.example.monstergrid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

public class GridAnimationManager {

    public interface AnimationCallback {
        void onFinished();
    }

    private static final String DEFAULT_CELL_COLOR = "#1A1A1A";
    private static TextView turnIndicator;

    /**
     * Animates a character moving from one cell to another.
     */
    public static void animateStationaryToTarget(TextView sourceCell, TextView targetCell, String characterTag, int targetBgColor, AnimationCallback callback) {
        sourceCell.animate().cancel();
        sourceCell.setTranslationX(0);
        sourceCell.setTranslationY(0);
        sourceCell.setScaleX(1.0f);
        sourceCell.setScaleY(1.0f);

        targetCell.animate().cancel();
        targetCell.setTranslationX(0);
        targetCell.setTranslationY(0);
        targetCell.setScaleX(1.0f);
        targetCell.setScaleY(1.0f);
        
        int[] sourcePos = new int[2];
        int[] targetPos = new int[2];
        sourceCell.getLocationInWindow(sourcePos);
        targetCell.getLocationInWindow(targetPos);

        float diffX = sourcePos[0] - targetPos[0];
        float diffY = sourcePos[1] - targetPos[1];

        targetCell.setText(characterTag);
        targetCell.setBackgroundColor(targetBgColor);
        targetCell.setTranslationX(diffX);
        targetCell.setTranslationY(diffY);
        targetCell.setAlpha(1.0f);
        
        sourceCell.setText("");
        sourceCell.setBackgroundColor(Color.parseColor(DEFAULT_CELL_COLOR));

        targetCell.bringToFront();

        targetCell.animate()
                .translationX(0)
                .translationY(0)
                .setDuration(300) 
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (callback != null) callback.onFinished();
                    }
                })
                .start();
    }

    /**
     * Updates the position of the turn indicator arrow.
     * Uses LayoutParams to avoid conflicts with translation animations.
     */
    public static void updateTurnIndicator(final View playerCell, final FrameLayout effectLayer) {
        if (playerCell == null || effectLayer == null) return;

        if (turnIndicator == null) {
            turnIndicator = new TextView(playerCell.getContext());
            turnIndicator.setText("▼");
            turnIndicator.setTextColor(Color.YELLOW);
            turnIndicator.setTextSize(24);
            turnIndicator.setGravity(Gravity.CENTER);
            turnIndicator.setShadowLayer(8, 0, 2, Color.BLACK);
            
            // Subtle floating animation using translationY as an offset
            ObjectAnimator animator = ObjectAnimator.ofFloat(turnIndicator, "translationY", 0f, -25f, 0f);
            animator.setDuration(1000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        }

        // Ensure measurements are ready before positioning
        playerCell.post(() -> {
            if (turnIndicator.getParent() != effectLayer) {
                if (turnIndicator.getParent() != null) {
                    ((FrameLayout) turnIndicator.getParent()).removeView(turnIndicator);
                }
                effectLayer.addView(turnIndicator);
            }

            int[] cellPos = new int[2];
            int[] layerPos = new int[2];
            playerCell.getLocationInWindow(cellPos);
            effectLayer.getLocationInWindow(layerPos);

            // Fail-safe: if the cell isn't visible or positioned yet, hide the indicator
            if (cellPos[0] == 0 && cellPos[1] == 0) {
                turnIndicator.setVisibility(View.GONE);
                return;
            }

            int cellWidth = playerCell.getWidth();
            int cellHeight = playerCell.getHeight();

            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) turnIndicator.getLayoutParams();
            if (lp == null) {
                lp = new FrameLayout.LayoutParams(cellWidth, cellHeight);
            } else {
                lp.width = cellWidth;
                lp.height = cellHeight;
            }

            // Position the arrow's layout box exactly above the cell
            lp.leftMargin = cellPos[0] - layerPos[0];
            lp.topMargin = cellPos[1] - layerPos[1] - (int)(cellHeight * 0.85f);
            lp.gravity = Gravity.TOP | Gravity.START;

            turnIndicator.setLayoutParams(lp);
            turnIndicator.setVisibility(View.VISIBLE);
            turnIndicator.bringToFront();
        });
    }
    
    public static void hideTurnIndicator() {
        if (turnIndicator != null) {
            turnIndicator.setVisibility(View.GONE);
        }
    }

    public static void animateMeleeAttack(View attackerCell, View targetCell, AnimationCallback callback) {
        int[] attackerPos = new int[2];
        int[] targetPos = new int[2];
        attackerCell.getLocationInWindow(attackerPos);
        targetCell.getLocationInWindow(targetPos);

        float diffX = (targetPos[0] - attackerPos[0]) * 0.5f;
        float diffY = (targetPos[1] - attackerPos[1]) * 0.5f;

        attackerCell.bringToFront();
        attackerCell.animate()
                .translationX(diffX)
                .translationY(diffY)
                .setDuration(150)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        attackerCell.animate()
                                .translationX(0)
                                .translationY(0)
                                .setDuration(150)
                                .setInterpolator(new DecelerateInterpolator())
                                .setListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        if (callback != null) callback.onFinished();
                                    }
                                })
                                .start();
                    }
                })
                .start();
    }

    public static void animateProjectile(View sourceCell, View targetCell, FrameLayout effectLayer, AnimationCallback callback) {
        View bullet = new View(sourceCell.getContext());
        int size = sourceCell.getWidth() / 4;
        bullet.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        bullet.setBackgroundColor(Color.YELLOW);
        effectLayer.addView(bullet);

        int[] sourcePos = new int[2];
        int[] targetPos = new int[2];
        int[] layerPos = new int[2];
        
        sourceCell.getLocationInWindow(sourcePos);
        targetCell.getLocationInWindow(targetPos);
        effectLayer.getLocationInWindow(layerPos);

        float startX = sourcePos[0] - layerPos[0] + (sourceCell.getWidth() / 2f) - (size / 2f);
        float startY = sourcePos[1] - layerPos[1] + (sourceCell.getHeight() / 2f) - (size / 2f);
        float endX = targetPos[0] - layerPos[0] + (targetCell.getWidth() / 2f) - (size / 2f);
        float endY = targetPos[1] - layerPos[1] + (targetCell.getHeight() / 2f) - (size / 2f);

        bullet.setX(startX);
        bullet.setY(startY);

        bullet.animate()
                .x(endX)
                .y(endY)
                .setDuration(250)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        effectLayer.removeView(bullet);
                        showImpactEffect(targetCell, effectLayer, callback);
                    }
                })
                .start();
    }

    public static void showImpactEffect(View targetCell, FrameLayout effectLayer, AnimationCallback callback) {
        View impact = new View(targetCell.getContext());
        int size = (int)(targetCell.getWidth() * 1.2);
        impact.setLayoutParams(new FrameLayout.LayoutParams(size, size));
        impact.setBackgroundColor(Color.WHITE);
        impact.setAlpha(0.8f);
        effectLayer.addView(impact);

        int[] targetPos = new int[2];
        int[] layerPos = new int[2];
        targetCell.getLocationInWindow(targetPos);
        effectLayer.getLocationInWindow(layerPos);

        impact.setX(targetPos[0] - layerPos[0] - (size - targetCell.getWidth())/2f);
        impact.setY(targetPos[1] - layerPos[1] - (size - targetCell.getHeight())/2f);

        impact.animate()
                .scaleX(1.8f)
                .scaleY(1.8f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        effectLayer.removeView(impact);
                        if (callback != null) callback.onFinished();
                    }
                })
                .start();
    }
}
