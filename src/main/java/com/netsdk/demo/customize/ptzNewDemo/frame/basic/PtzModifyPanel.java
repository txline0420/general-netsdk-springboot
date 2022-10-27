package com.netsdk.demo.customize.ptzNewDemo.frame.basic;

import com.netsdk.demo.customize.ptzNewDemo.frame.SwingUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 变倍、变焦设置 面板
 *
 * @author 47040
 * @since Created in 2021/3/25 20:27
 */
public class PtzModifyPanel extends JPanel {

    private final JButton zoomAddBtn = new JButton("变倍+");
    private final JButton zoomDecBtn = new JButton("变倍-");
    private final JComboBox<String> speedComboBox;
    private final JButton focusAddBtn = new JButton("变焦+");
    private final JButton focusDecBtn = new JButton("变焦-");

    {
        speedComboBox = new JComboBox<String>(new String[]{
                "速率" + " 1", "速率" + " 2", "速率" + " 3", "速率" + " 4",
                "速率" + " 5", "速率" + " 6", "速率" + " 7", "速率" + " 8"
        });
        speedComboBox.setSelectedIndex(4);
        speedComboBox.setPreferredSize(new Dimension(74, 25));
    }

    public PtzModifyPanel() {
        SwingUtil.setBorderEx(this, "变倍、变焦设置", 2);
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

        placeHolder01.add(zoomAddBtn);
        placeHolder01.add(zoomDecBtn);

        placeHolder02.add(speedComboBox);

        placeHolder03.add(focusAddBtn);
        placeHolder03.add(focusDecBtn);

        add(placeHolder01);
        add(placeHolder02);
        add(placeHolder03);
    }

    public int getSpeed() {
        return speedComboBox.getSelectedIndex() + 1;
    }

    public JButton getZoomAddBtn() {
        return zoomAddBtn;
    }

    public JButton getZoomDecBtn() {
        return zoomDecBtn;
    }

    public JButton getFocusAddBtn() {
        return focusAddBtn;
    }

    public JButton getFocusDecBtn() {
        return focusDecBtn;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
