package com.example.monstergrid;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import androidx.appcompat.widget.AppCompatImageView;
import java.util.Random;

public class ShimmerImageView extends AppCompatImageView {
    private Paint mPaint = new Paint();
    private float mProgress = -3f;
    private LinearGradient mShader;
    private Matrix mMatrix = new Matrix();
    private ValueAnimator mAnimator;
    private Random mRandom = new Random();
    
    private final Runnable mRestartRunnable = new Runnable() {
        @Override
        public void run() {
            if (mAnimator != null && getVisibility() == VISIBLE) {
                mAnimator.start();
            }
        }
    };

    public ShimmerImageView(Context context) { super(context); }
    public ShimmerImageView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void startShimmer() {
        stopShimmer();
        
        mAnimator = ValueAnimator.ofFloat(-3.0f, 4.0f);
        mAnimator.setDuration(1000);
        mAnimator.setInterpolator(new LinearInterpolator());
        
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            invalidate();
        });

        mAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                postDelayed(mRestartRunnable, 2500 + mRandom.nextInt(2000));
            }
        });

        mAnimator.setStartDelay(mRandom.nextInt(1000));
        mAnimator.start();
    }

    public void stopShimmer() {
        if (mAnimator != null) {
            mAnimator.removeAllUpdateListeners();
            mAnimator.removeAllListeners();
            mAnimator.cancel();
            mAnimator = null;
        }
        removeCallbacks(mRestartRunnable);
        mProgress = -3.0f;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopShimmer();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getDrawable() == null) return;
        
        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        super.onDraw(canvas);

        if (mProgress > -3.0f && mProgress < 4.0f) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                if (mShader == null) {
                    mShader = new LinearGradient(0, 0, w * 0.6f, h,
                            new int[]{0x00FFFFFF, 0x00FFFFFF, 0xB3FFFFFF, 0x00FFFFFF, 0x00FFFFFF},
                            new float[]{0f, 0.35f, 0.5f, 0.65f, 1f}, Shader.TileMode.CLAMP);
                    mPaint.setShader(mShader);
                    mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));
                }
                mMatrix.setTranslate(w * mProgress, 0);
                mShader.setLocalMatrix(mMatrix);
                canvas.drawRect(0, 0, w, h, mPaint);
            }
        }
        
        canvas.restoreToCount(saveCount);
    }
}
