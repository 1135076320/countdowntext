package com.example.countdowntext;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CountDownTextView extends androidx.appcompat.widget.AppCompatTextView {
    final private static String TAG = CountDownTextView.class.getSimpleName();
    final private static int capacity = 2;
    /**
     * 计时间隔
     */
    private TimeUnit interval = TimeUnit.SECONDS;

    /**
     * 倒计时文本
     */
    private String countDownTextView = this.getContext().getString(R.string.default_countdown_text);
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
        super(context);
    }

    public CountDownTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CountDownTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置倒计时文本
     * @param front 前
     * @param latter 后
     * @return this
     */
    public CountDownTextView setCountDownText(String front, String latter){
        countDownTextView = front + "%1$s" + latter;
        return this;
    }

    /**
     * 设置计时间隔
     * @param interval 计时间隔
     * @return this
     */
    public CountDownTextView setInterval(TimeUnit interval) {
        this.interval = interval;
        return this;
    }

    /**
     * 设置计时前文本
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

    }


    private void countDown(){

    }

    /**
     * 添加开始倒计时事件的监听器
     * @param listener 监听器
     */
    public void addCountDownStartListener(CountDownStartListener listener){
        if (countDownStartListeners == null){
            countDownStartListeners = new ArrayList<>(2);
        }
        countDownStartListeners.add(listener);
    }

    /**
     * 添加倒计时结束事件的监听器
     * @param listener 监听器
     */
    public void addCountDownOverListener(CountDownOverListener listener){
        if (countDownOverListeners == null){
            countDownOverListeners =  new ArrayList<>(2);
        }
        countDownOverListeners.add(listener);
    }

    /**
     * 添加倒计时Tick的监听器
     * @param listener
     */
    public void addCountDownTickListener(CountDownTickListener listener){
        if (countDownTickListeners == null){
            countDownTickListeners = new ArrayList<>(2);
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
}
