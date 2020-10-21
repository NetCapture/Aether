package cn.demo.appq.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

import com.blankj.utilcode.util.ScreenUtils;

public class FastScrollView extends ScrollView {

    public FastScrollView(Context context) {
        super(context);
    }

    public FastScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FastScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FastScrollView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void fling(int velocityY) {
        int viewHeight = getChildAt(0).getHeight();
        int screenHeight = ScreenUtils.getScreenHeight();
        if (viewHeight * 1.0f / screenHeight > 8) {
            super.fling((int) (velocityY * (viewHeight * 0.05f / screenHeight)));
        } else {
            super.fling(velocityY);
        }
    }
}
