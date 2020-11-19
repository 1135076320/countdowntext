package com.example.countdowntext;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CountDownTextView extends androidx.appcompat.widget.AppCompatTextView implements LifecycleObserver {
    final private static String TAG = CountDownTextView.class.getSimpleName();
    //持久化常量
    private static final String SHARED_PREFERENCE_FILE = "CountDownTextView";
    private static final String SHARED_PREFERENCE_FILE_START_TIME = "last_start_time";
    private static final String sa = "dd";
    final private static int capacity = 2;
    /**
     * 计时单位
     */
    private TimeUnit timeUnit = TimeUnit.SECONDS;
    /**
     * 计时间隔，以计时单位为单位
     */
    private long interval = 1;
    /**
     * 计时时长,以计时单位为单位
     */
    private long endtime = 60;
    /**
     * 倒计时文本
     */
    private String countDownText = this.getContext().getString(R.string.default_countdown_text);
    /**
     * 倒计时之前的文本
     */
    private String textBeforeCountDown = this.getContext().getString(R.string.default_text_before_countdown);
    /**
     * 倒计时之后的文本
     */
    private String textAfterCountDown = this.getContext().getString(R.string.default_text_after_countdown);
    /**
     * 真正计时的类
     */
    private CountDownTimer countDownTimer;
    /**
     * 倒计时开始监听器集合
     */
    private List<CountDownStartListener> countDownStartListeners;
    /**
     * 倒计时结束监听器集合
     */
    private List<CountDownOverListener> countDownOverListeners;
    /**
     * 倒计时Tick监听器集合
     */
    private List<CountDownTickListener> countDownTickListeners;

    public CountDownTextView(Context context) {
        this(context,null);
    }

    public CountDownTextView(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public CountDownTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    /**
     * 初始化函数
     * @param context 上下文
     */
    private void init(Context context) {
        autoBindLifecycle(context);
    }


    /**
     * 设置倒计时文本
     * @param front 前
     * @param latter 后
     * @return this
     */
    public CountDownTextView setCountDownText(String front, String latter){
        countDownText = front + "%1$s" + latter;
        return this;
    }

    /**
     * 设置计时间隔
     * @param timeUnit 计时单位
     * @return this
     */
    public CountDownTextView setTimeUnit(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
        return this;
    }

    /**
     * 设置计时间隔
     * @param interval 计时间隔
     * @return this
     */
    public CountDownTextView setInterval(long interval){
        this.interval = interval;
        return this;
    }

    /**
     * 设置计时前文本,不存在单独的意义，可复用TextView的文本
     * @param textBeforeCountDown 文本
     * @return this
     */
    public CountDownTextView setTextBeforeCountDown(String textBeforeCountDown) {
        this.textBeforeCountDown = textBeforeCountDown;
        return this;
    }

    /**
     * 设置计时后文本
     * @param textAfterCountDown 文本
     * @return this
     */
    public CountDownTextView setTextAfterCountDown(String textAfterCountDown) {
        this.textAfterCountDown = textAfterCountDown;
        return this;
    }


    /**
     * 开始计时
     */
    public void startCount(){
        countDown(TimeUnit.MILLISECONDS.convert(endtime,timeUnit),
                0,
                TimeUnit.MICROSECONDS.convert(interval,timeUnit),
                timeUnit);
    }

    /**
     * 实际计时的类
     * @param endTime 多长时间之后计时结束
     * @param offset 已经计时了的时间
     * @param interval 计时间隔
     * @param timeUnit 计时单位
     */
    private void countDown(long endTime, long offset, long interval, final TimeUnit timeUnit){
        if (offset == 0 && countDownStartListeners.size() != 0){
            for (CountDownStartListener countDownStartListener : countDownStartListeners) {
                countDownStartListener.onCountDownStart();
            }
        }
        countDownTimer = new CountDownTimer(endTime,interval) {
            @Override
            public void onTick(long l) {
                setText(String.format(countDownText, String.valueOf(timeUnit.convert(l, TimeUnit.MILLISECONDS))));
                if (countDownTickListeners.size() > 0){
                    for (CountDownTickListener countDownTickListener : countDownTickListeners) {
                        countDownTickListener.onCountDownTick();
                    }
                }
            }

            @Override
            public void onFinish() {
                setText(textAfterCountDown);
                if (countDownOverListeners.size() > 0){
                    for (CountDownOverListener countDownOverListener : countDownOverListeners) {
                        countDownOverListener.onCountDownOver();
                    }
                }
            }
        };
        countDownTimer.start();


    }

    /**
     * 添加开始倒计时事件的监听器
     * @param listener 监听器
     */
    public void addCountDownStartListener(CountDownStartListener listener){
        if (countDownStartListeners == null){
            countDownStartListeners = new ArrayList<>(capacity);
        }
        countDownStartListeners.add(listener);
    }

    /**
     * 添加倒计时结束事件的监听器
     * @param listener 监听器
     */
    public void addCountDownOverListener(CountDownOverListener listener){
        if (countDownOverListeners == null){
            countDownOverListeners =  new ArrayList<>(capacity);
        }
        countDownOverListeners.add(listener);
    }

    /**
     * 添加倒计时Tick的监听器
     * @param listener this
     */
    public void addCountDownTickListener(CountDownTickListener listener){
        if (countDownTickListeners == null){
            countDownTickListeners = new ArrayList<>(capacity);
        }
        countDownTickListeners.add(listener);
    }
    /**
     * 开始倒计时事件
     */
    interface CountDownStartListener{
        void onCountDownStart();
    }

    /**
     * 倒计时Tick事件
     */
    interface CountDownTickListener{
        void onCountDownTick();
    }

    /**
     * 倒计时完成事件
     */
    interface CountDownOverListener{
        void onCountDownOver();
    }
    /**
     * 与fragment或者activity进行生命周期绑定，及时清理资源，避免内存泄漏
     * @param context context
     */
    private void autoBindLifecycle(Context context){
        if (context instanceof FragmentActivity){
            FragmentActivity fragmentActivity = (FragmentActivity)context;
            FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
            List<Fragment> fragmentList = fragmentManager.getFragments();
            for (Fragment fragment : fragmentList) {
                View parent = fragment.getView();
                if (this == parent.findViewById(getId())){
                    fragment.getLifecycle().addObserver(this);
                    return;
                }
            }
        }
        if (context instanceof LifecycleOwner){
            ((LifecycleOwner) context).getLifecycle().addObserver(this);
        }
    }

    /**
     * 生命周期绑定事件
     */
    /**
     * 组件重新恢复时，如果需要，则继续计时
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private void onLifecycleOwnerResume(){
        //todo
    }

    /**
     * 绑定组件销毁时，取消计时，避免内存泄漏
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private void onLifecycleOwnerDestroy(){
        if (countDownTimer != null){
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    /**
     * 当前组件从window中移除时，也需要取消计时
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        onLifecycleOwnerDestroy();
    }
}
