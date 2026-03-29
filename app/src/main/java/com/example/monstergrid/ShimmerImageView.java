package com.example.monstergrid;

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
    private float mProgress = -1f;
    private LinearGradient mShader;
    private Matrix mMatrix = new Matrix();
    private ValueAnimator mAnimator;

    public ShimmerImageView(Context context) { super(context); }
    public ShimmerImageView(Context context, AttributeSet attrs) { super(context, attrs); }

    public void startShimmer() {
        if (mAnimator != null) mAnimator.cancel();
        mAnimator = ValueAnimator.ofFloat(-1.5f, 2.5f);
        mAnimator.setDuration(2500);
        mAnimator.setInterpolator(new LinearInterpolator());
        mAnimator.setRepeatCount(ValueAnimator.INFINITE);
        mAnimator.setStartDelay(new Random().nextInt(1500));
        mAnimator.addUpdateListener(animation -> {
            mProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        mAnimator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Create a layer to apply Xfermode correctly
        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);
        
        super.onDraw(canvas);

        if (mProgress > -1.5f && mProgress < 2.5f) {
            int w = getWidth();
            int h = getHeight();
            if (w > 0 && h > 0) {
                if (mShader == null) {
                    mShader = new LinearGradient(0, 0, w / 2, h,
                            new int[]{0x00FFFFFF, 0x00FFFFFF, 0xCCFFFFFF, 0x00FFFFFF, 0x00FFFFFF},
                            new float[]{0f, 0.4f, 0.5f, 0.6f, 1f}, Shader.TileMode.CLAMP);
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
