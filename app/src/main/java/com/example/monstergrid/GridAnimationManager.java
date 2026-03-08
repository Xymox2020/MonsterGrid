package com.example.monstergrid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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
    private static FrameLayout turnIndicatorContainer;
    private static TextView turnIndicatorView;

    /**
     * Animates a character moving from one cell to another.
     * Also animates the turn indicator in sync if it is currently visible.
     */
    public static void animateStationaryToTarget(TextView sourceCell, TextView targetCell, String characterTag, int targetBgColor, float textSize, FrameLayout effectLayer, AnimationCallback callback) {
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
        targetCell.setTextSize(textSize);
        targetCell.setBackgroundColor(targetBgColor);
        targetCell.setTranslationX(diffX);
        targetCell.setTranslationY(diffY);
        targetCell.setAlpha(1.0f);
        
        sourceCell.setText("");
        sourceCell.setBackgroundColor(Color.parseColor(DEFAULT_CELL_COLOR));

        targetCell.bringToFront();

        // Animate turn indicator if it's currently active
        if (turnIndicatorContainer != null && turnIndicatorContainer.getVisibility() == View.VISIBLE && effectLayer != null) {
            turnIndicatorContainer.animate().cancel();
            
            // Position at final destination (targetPos) without considering current cell translations
            int[] layerPos = new int[2];
            effectLayer.getLocationInWindow(layerPos);
            
            int cellWidth = targetCell.getWidth();
            int cellHeight = targetCell.getHeight();

            updateIndicatorLayoutParams(
                targetPos[0] - layerPos[0],
                targetPos[1] - layerPos[1],
                cellWidth,
                cellHeight,
                effectLayer
            );

            // Start from the source offset and animate to 0 (the target)
            turnIndicatorContainer.setTranslationX(diffX);
            turnIndicatorContainer.setTranslationY(diffY);
            turnIndicatorContainer.animate()
                    .translationX(0)
                    .translationY(0)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }

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

    private static void updateIndicatorLayoutParams(int left, int top, int width, int height, FrameLayout effectLayer) {
        if (turnIndicatorContainer == null) return;
        
        if (turnIndicatorContainer.getParent() != effectLayer) {
            if (turnIndicatorContainer.getParent() != null) {
                ((ViewGroup) turnIndicatorContainer.getParent()).removeView(turnIndicatorContainer);
            }
            effectLayer.addView(turnIndicatorContainer);
        }

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) turnIndicatorContainer.getLayoutParams();
        if (lp == null) {
            lp = new FrameLayout.LayoutParams(width, height);
        } else {
            lp.width = width;
            lp.height = height;
        }

        lp.leftMargin = left;
        lp.topMargin = top - (int)(height * 0.85f);
        lp.gravity = Gravity.TOP | Gravity.START;

        turnIndicatorContainer.setLayoutParams(lp);
        turnIndicatorContainer.setVisibility(View.VISIBLE);
        turnIndicatorContainer.bringToFront();
    }

    /**
     * Updates the position of the turn indicator arrow.
     */
    public static void updateTurnIndicator(final View playerCell, final FrameLayout effectLayer) {
        if (playerCell == null || effectLayer == null) return;

        if (turnIndicatorContainer == null) {
            turnIndicatorContainer = new FrameLayout(playerCell.getContext());
            turnIndicatorView = new TextView(playerCell.getContext());
            turnIndicatorView.setText("▼");
            turnIndicatorView.setTextColor(Color.YELLOW);
            turnIndicatorView.setTextSize(24);
            turnIndicatorView.setGravity(Gravity.CENTER);
            turnIndicatorView.setShadowLayer(8, 0, 2, Color.BLACK);
            
            turnIndicatorContainer.addView(turnIndicatorView, new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

            ObjectAnimator animator = ObjectAnimator.ofFloat(turnIndicatorView, "translationY", 0f, -25f, 0f);
            animator.setDuration(1000);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        }

        playerCell.post(() -> {
            int[] cellPos = new int[2];
            int[] layerPos = new int[2];
            playerCell.getLocationInWindow(cellPos);
            effectLayer.getLocationInWindow(layerPos);

            if (cellPos[0] == 0 && cellPos[1] == 0) return;

            updateIndicatorLayoutParams(
                cellPos[0] - layerPos[0],
                cellPos[1] - layerPos[1],
                playerCell.getWidth(),
                playerCell.getHeight(),
                effectLayer
            );
            
            turnIndicatorContainer.setTranslationX(0);
            turnIndicatorContainer.setTranslationY(0);
        });
    }
    
    public static void hideTurnIndicator() {
        if (turnIndicatorContainer != null) {
            turnIndicatorContainer.setVisibility(View.GONE);
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

    public static void showDamageIndicator(View targetCell, FrameLayout effectLayer, String text, int color) {
        if (targetCell == null || effectLayer == null) return;

        final TextView damageText = new TextView(targetCell.getContext());
        damageText.setText(text);
        damageText.setTextColor(color);
        damageText.setTextSize(18);
        damageText.setTypeface(Typeface.DEFAULT_BOLD);
        damageText.setShadowLayer(4, 2, 2, Color.BLACK);
        damageText.setGravity(Gravity.CENTER);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        damageText.setLayoutParams(lp);
        effectLayer.addView(damageText);

        int[] targetPos = new int[2];
        int[] layerPos = new int[2];
        targetCell.getLocationInWindow(targetPos);
        effectLayer.getLocationInWindow(layerPos);

        damageText.post(() -> {
            float startX = targetPos[0] - layerPos[0] + (targetCell.getWidth() / 2f) - (damageText.getWidth() / 2f);
            float startY = targetPos[1] - layerPos[1] + (targetCell.getHeight() / 2f) - (damageText.getHeight() / 2f);

            damageText.setX(startX);
            damageText.setY(startY);

            damageText.animate()
                    .translationYBy(-120)
                    .alpha(0f)
                    .setDuration(1000)
                    .setInterpolator(new DecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            effectLayer.removeView(damageText);
                        }
                    })
                    .start();
        });
    }
}
