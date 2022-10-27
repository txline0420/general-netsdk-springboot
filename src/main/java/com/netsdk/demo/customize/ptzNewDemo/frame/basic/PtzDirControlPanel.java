package com.netsdk.demo.customize.ptzNewDemo.frame.basic;

import com.netsdk.demo.customize.ptzNewDemo.frame.SwingUtil;

import javax.swing.*;
import java.awt.*;

/**
 * PTZ 方向控制 面板
 *
 * @author 47040
 * @since Created in 2021/3/25 20:40
 */
public class PtzDirControlPanel extends JPanel {

    private final JComboBox<String> speedComboBox;
    private final JButton upBtn = new JButton("上");
    private final JButton leftUpBtn = new JButton("左上");
    private final JButton rightUpBtn = new JButton("右上");
    private final JButton downBtn = new JButton("下");
    private final JButton leftDownBtn = new JButton("左下");
    private final JButton rightDownBtn = new JButton("右下");
    private final JButton leftBtn = new JButton("左");
    private final JButton rightBtn = new JButton("右");

    {
        speedComboBox = new JComboBox<String>(new String[]{
                "速率" + " 1", "速率" + " 2", "速率" + " 3", "速率" + " 4",
                "速率" + " 5", "速率" + " 6", "速率" + " 7", "速率" + " 8"
        });
        speedComboBox.setSelectedIndex(4);
        speedComboBox.setPreferredSize(new Dimension(74, 25));
    }

    public PtzDirControlPanel() {
        SwingUtil.setBorderEx(this, "方向控制", 2);
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

        placeHolder01.add(leftUpBtn);
        placeHolder01.add(upBtn);
        placeHolder01.add(rightUpBtn);

        placeHolder02.add(leftBtn);
        placeHolder02.add(speedComboBox);
        placeHolder02.add(rightBtn);

        placeHolder03.add(leftDownBtn);
        placeHolder03.add(downBtn);
        placeHolder03.add(rightDownBtn);

        add(placeHolder01);
        add(placeHolder02);
        add(placeHolder03);
    }

    public int getSpeed() {
        return speedComboBox.getSelectedIndex() + 1;
    }

    public JButton getUpBtn() {
        return upBtn;
    }

    public JButton getLeftUpBtn() {
        return leftUpBtn;
    }

    public JButton getRightUpBtn() {
        return rightUpBtn;
    }

    public JButton getDownBtn() {
        return downBtn;
    }

    public JButton getLeftDownBtn() {
        return leftDownBtn;
    }

    public JButton getRightDownBtn() {
        return rightDownBtn;
    }

    public JButton getLeftBtn() {
        return leftBtn;
    }

    public JButton getRightBtn() {
        return rightBtn;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
