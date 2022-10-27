package com.netsdk.demo.customize.ptzNewDemo.frame.basic;

import com.netsdk.demo.customize.ptzNewDemo.frame.SwingUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 绝对移动 ( MoveAbsolutely ) 面板
 *
 * @author 47040
 * @since Created in 2021/3/25 20:30
 */
public class PtzMoveAbsolutePanel extends JPanel {

    private final JTextField ptzMoveAbsoluteX = new JTextField("0", 8);
    private final JTextField ptzMoveAbsoluteY = new JTextField("0", 8);
    private final JTextField ptzMoveAbsoluteZoom = new JTextField("0", 8);

    private final JButton ptzMoveAbsoluteBtn = new JButton("前往坐标");
    private final JButton ptzMoveAbsoluteCheck = new JButton("是否支持");
    private final JButton ptzGetLocationBtn = new JButton("当前坐标");

    public PtzMoveAbsolutePanel() {
        SwingUtil.setBorderEx(this, "绝对移动 ( MoveAbsolutely ) ", 2);
        setLayout(new GridLayout(3, 1));
        Dimension dim = getPreferredSize();
        dim.width = 240;
        dim.height = 160;
        setPreferredSize(dim);

        JPanel placeHolder01 = new JPanel();
        placeHolder01.setLayout(new FlowLayout());
        JPanel placeHolder02 = new JPanel();
        placeHolder02.setLayout(new FlowLayout());
        JPanel placeHolder03 = new JPanel();
        placeHolder03.setLayout(new FlowLayout());

        JLabel xParamLabel = new JLabel("PosX(10倍 ):");
        JLabel yParamLabel = new JLabel("PosY(10倍 ):");
        JLabel zoomParamLabel = new JLabel("Zoom(1,128):");

        placeHolder01.add(xParamLabel);
        placeHolder01.add(ptzMoveAbsoluteX);
        placeHolder01.add(ptzMoveAbsoluteBtn);

        placeHolder02.add(yParamLabel);
        placeHolder02.add(ptzMoveAbsoluteY);
        placeHolder02.add(ptzMoveAbsoluteCheck);

        placeHolder03.add(zoomParamLabel);
        placeHolder03.add(ptzMoveAbsoluteZoom);
        placeHolder03.add(ptzGetLocationBtn);

        add(placeHolder01);
        add(placeHolder02);
        add(placeHolder03);
    }

    public int getXParam() {
        return Integer.parseInt(ptzMoveAbsoluteX.getText());
    }

    public int getYParam() {
        return Integer.parseInt(ptzMoveAbsoluteY.getText());
    }

    public int getZoomParam() {
        return Integer.parseInt(ptzMoveAbsoluteZoom.getText());
    }

    public void setPtzMoveAbsoluteParams(int xParam, int yParam, int zParam) {
        ptzMoveAbsoluteX.setText(String.valueOf(xParam));
        ptzMoveAbsoluteY.setText(String.valueOf(yParam));
        ptzMoveAbsoluteZoom.setText(String.valueOf(zParam));
    }

    public JButton getPtzMoveAbsoluteBtn() {
        return ptzMoveAbsoluteBtn;
    }

    public JButton getPtzMoveAbsoluteCheck() {
        return ptzMoveAbsoluteCheck;
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
