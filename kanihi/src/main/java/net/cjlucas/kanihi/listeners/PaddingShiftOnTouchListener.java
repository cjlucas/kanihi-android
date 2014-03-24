package net.cjlucas.kanihi.listeners;

import android.view.MotionEvent;
import android.view.View;

/**
 * Created by chris on 3/22/14.
 */
public class PaddingShiftOnTouchListener implements View.OnTouchListener {
    private static final int SHIFT_LEFT = 2;
    private static final int SHIFT_TOP = 2;
    private static final int SHIFT_RIGHT = -2;
    private static final int SHIFT_BOTTOM = -2;

    private int mPadLeft;
    private int mPadTop;
    private int mPadRight;
    private int mPadBottom;

    private void onActionDown(View view) {
        view.setPadding(mPadLeft + SHIFT_LEFT,
                mPadTop + SHIFT_TOP,
                mPadRight + SHIFT_RIGHT,
                mPadBottom + SHIFT_BOTTOM);
    }

    private void onActionUp(View view) {
        view.setPadding(mPadLeft - SHIFT_LEFT,
                mPadTop - SHIFT_TOP,
                mPadRight - SHIFT_RIGHT,
                mPadBottom - SHIFT_BOTTOM);
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        mPadLeft = view.getPaddingLeft();
        mPadTop = view.getPaddingTop();
        mPadRight = view.getPaddingRight();
        mPadBottom = view.getPaddingBottom();

        switch(motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                onActionDown(view);
                break;
            case MotionEvent.ACTION_UP:
                onActionUp(view);
                break;
            default:
                break;
        }

        return true;
    }
}
