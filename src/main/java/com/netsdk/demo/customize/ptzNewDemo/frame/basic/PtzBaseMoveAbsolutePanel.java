package com.netsdk.demo.customize.ptzNewDemo.frame.basic;

import com.netsdk.demo.customize.ptzNewDemo.frame.SwingUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 精确绝对移动 ( BaseMoveAbsolutely ) 面板
 *
 * @author 47040
 * @since Created in 2021/3/25 20:30
 */
public class PtzBaseMoveAbsolutePanel extends JPanel {

    private final JTextField ptzBaseMoveAbsoluteX = new JTextField("0", 12);
    private final JTextField ptzBaseMoveAbsoluteY = new JTextField("0", 12);
    private final JTextField ptzBaseZoomMap = new JTextField("0", 12);
    private final JTextField ptzBaseFocusMap = new JTextField("0", 12);

    private final JButton ptzMoveAbsoluteBtn = new JButton("前往坐标");
    private final JButton ptzGetLocationBtn = new JButton("当前坐标");
    private final JButton ptzSetFocusMapBtn = new JButton("设聚焦值");

    public PtzBaseMoveAbsolutePanel() {
        SwingUtil.setBorderEx(this, "精确绝对移动 ( BaseMoveAbsolutely ) ", 2);
        setLayout(new GridLayout(3, 1));
        Dimension dim = getPreferredSize();
        dim.width = 480;
        dim.height = 160;
        setPreferredSize(dim);

        JPanel placeHolder01 = new JPanel();
        placeHolder01.setLayout(new FlowLayout());
        JPanel placeHolder02 = new JPanel();
        placeHolder02.setLayout(new FlowLayout());
        JPanel placeHolder03 = new JPanel();
        placeHolder03.setLayout(new FlowLayout());

        JLabel xParamLabel   = new JLabel(" PosX(10倍 ) :");
        JLabel yParamLabel   = new JLabel(" PosY(10倍 ) :");
        JLabel zoomMapLabel  = new JLabel("ZoomMapValue :");
        JLabel focusMapLabel = new JLabel("FocusMapValue:");

        placeHolder01.add(xParamLabel);
        placeHolder01.add(ptzBaseMoveAbsoluteX);
        placeHolder01.add(yParamLabel);
        placeHolder01.add(ptzBaseMoveAbsoluteY);

        placeHolder02.add(zoomMapLabel);
        placeHolder02.add(ptzBaseZoomMap);
        placeHolder02.add(focusMapLabel);
        placeHolder02.add(ptzBaseFocusMap);

        placeHolder03.add(ptzGetLocationBtn);
        placeHolder03.add(ptzMoveAbsoluteBtn);
        placeHolder03.add(ptzSetFocusMapBtn);

        add(placeHolder01);
        add(placeHolder02);
        add(placeHolder03);
    }

    public int getXParam() {
        return Integer.parseInt(ptzBaseMoveAbsoluteX.getText());
    }

    public int getYParam() {
        return Integer.parseInt(ptzBaseMoveAbsoluteY.getText());
    }

    public int getZoomMapValue() {
        return Integer.parseInt(ptzBaseZoomMap.getText());
    }

    public int getFocusMapValue() {
        return Integer.parseInt(ptzBaseFocusMap.getText());
    }

    public void setPtzMoveAbsoluteParams(int xParam, int yParam, int zParam, int focusMap) {
        ptzBaseMoveAbsoluteX.setText(String.valueOf(xParam));
        ptzBaseMoveAbsoluteY.setText(String.valueOf(yParam));
        ptzBaseZoomMap.setText(String.valueOf(zParam));
        ptzBaseFocusMap.setText(String.valueOf(focusMap));
    }

    public JButton getPtzMoveAbsoluteBtn() {
        return ptzMoveAbsoluteBtn;
    }

    public JButton getPtzSetFocusMapBtn() {
        return ptzSetFocusMapBtn;
    }

    public JButton getGetLocationBtn() {
        return ptzGetLocationBtn;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
