package com.netsdk.demo.customize.ptzNewDemo.frame.basic;

import com.netsdk.demo.customize.ptzNewDemo.frame.SwingUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 三维精确定位 ( ExactGoto ) 面板
 *
 * @author 47040
 * @since Created in 2021/3/25 20:30
 */
public class PtzExactGotoPanel extends JPanel {

    private final JTextField ptzExactGotoX = new JTextField("0", 8);
    private final JTextField ptzExactGotoY = new JTextField("0", 8);
    private final JTextField ptzExactGotoZoom = new JTextField("0", 8);

    private final JButton ptzExactGotoBtn = new JButton("前往坐标");
    private final JButton ptzExactGotoCheckBtn = new JButton("是否支持");
    private final JButton ptzGetLocationBtn = new JButton("当前坐标");

    public PtzExactGotoPanel() {
        SwingUtil.setBorderEx(this, "三维精确定位 ( ExactGoto ) ", 2);
        setLayout(new GridLayout(3, 1));
        Dimension dim = getPreferredSize();
        dim.width = 240;
        dim.height = 160;
        setPreferredSize(dim);

        JPanel placeHolder01 = new JPanel();
        JPanel placeHolder02 = new JPanel();
        JPanel placeHolder03 = new JPanel();

        placeHolder01.setLayout(new FlowLayout());
        placeHolder02.setLayout(new FlowLayout());
        placeHolder03.setLayout(new FlowLayout());

        JLabel xParamLabel = new JLabel("PosX(10倍 ):");
        JLabel yParamLabel = new JLabel("PosY(10倍 ):");
        JLabel zoomParamLabel = new JLabel("Zoom(1,128):");

        placeHolder01.add(xParamLabel);
        placeHolder01.add(ptzExactGotoX);
        placeHolder01.add(ptzExactGotoBtn);

        placeHolder02.add(yParamLabel);
        placeHolder02.add(ptzExactGotoY);
        placeHolder02.add(ptzExactGotoCheckBtn);

        placeHolder03.add(zoomParamLabel);
        placeHolder03.add(ptzExactGotoZoom);
        placeHolder03.add(ptzGetLocationBtn);

        add(placeHolder01);
        add(placeHolder02);
        add(placeHolder03);
    }

    public int getXParam() {
        return Integer.parseInt(ptzExactGotoX.getText());
    }

    public int getYParam() {
        return Integer.parseInt(ptzExactGotoY.getText());
    }

    public int getZoomParam() {
        return Integer.parseInt(ptzExactGotoZoom.getText());
    }

    public void setExactGotoParams(int xParam, int yParam, int zParam) {
        ptzExactGotoX.setText(String.valueOf(xParam));
        ptzExactGotoY.setText(String.valueOf(yParam));
        ptzExactGotoZoom.setText(String.valueOf(zParam));
    }

    public JButton getPtzExactGotoBtn() {
        return ptzExactGotoBtn;
    }

    public JButton getPtzExactCheckBtn() {
        return ptzExactGotoCheckBtn;
    }

    public JButton getLocationBtn() {
        return ptzGetLocationBtn;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
