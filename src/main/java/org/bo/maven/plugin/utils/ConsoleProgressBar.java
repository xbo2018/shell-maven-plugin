package org.bo.maven.plugin.utils;

import java.text.DecimalFormat;

/**
 * Created by zhuzhibo on 2016/9/9.
 */
public class ConsoleProgressBar {

    private long minimum = 0; // ��������ʼֵ

    private long maximum = 100; // ���������ֵ

    private long barLen = 100; // ����������

    private char showChar = '='; // ���ڽ�������ʾ���ַ�

    private DecimalFormat formater = new DecimalFormat("#.##%");

    /**
     * ʹ��ϵͳ��׼�������ʾ�ַ�����������ٷֱȡ�
     */
    public ConsoleProgressBar() {
    }

    /**
     * ʹ��ϵͳ��׼�������ʾ�ַ�����������ٷֱȡ�
     *
     * @param minimum ��������ʼֵ
     * @param maximum ���������ֵ
     * @param barLen ����������
     */
    public ConsoleProgressBar(long minimum, long maximum,
                              long barLen) {
        this(minimum, maximum, barLen, '=');
    }

    /**
     * ʹ��ϵͳ��׼�������ʾ�ַ�����������ٷֱȡ�
     *
     * @param minimum ��������ʼֵ
     * @param maximum ���������ֵ
     * @param barLen ����������
     * @param showChar ���ڽ�������ʾ���ַ�
     */
    public ConsoleProgressBar(long minimum, long maximum,
                              long barLen, char showChar) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.barLen = barLen;
        this.showChar = showChar;
    }

    /**
     * ��ʾ��������
     *
     * @param value ��ǰ���ȡ����ȱ�����ڻ������ʼ����С�ڵ��ڽ����㣨start <= current <= end����
     */
    public void show(long value) {
        if (value < minimum || value > maximum) {
            return;
        }
        //��ǰ�����ô�ͷ��ʼ���ƽ���
        reset();
        minimum = value;
        float rate = (float) (minimum*1.0 / maximum);
        long len = (long) (rate * barLen);
        //����
        draw(len, rate);
        if (minimum == maximum) {
            afterComplete();
        }
    }

    private void draw(long len, float rate) {
        for (int i = 0; i < len; i++) {
            System.out.print(showChar);
        }
        System.out.print(' ');
        System.out.print(format(rate));
    }

    private void reset() {
        System.out.print('\r');
    }

    private void afterComplete() {
        System.out.print('\n');
    }

    private String format(float num) {
        return formater.format(num);
    }
}
