package com.netsdk.demo.customize.ptzNewDemo.frame;

import com.netsdk.demo.customize.ptzNewDemo.frame.basic.*;

import javax.swing.*;
import java.awt.*;

/**
 * PTZ 云台控制 面板
 *
 * @author 47040
 * @since Created in 2021/3/25 20:22
 */
public class PtzControlPanel extends JPanel {

    // PTZ 变倍、变焦控制 面板
    private final PtzModifyPanel ptzModifyPanel = new PtzModifyPanel();
    // PTZ 精确绝对移动 ( BaseMoveAbsolutely )
    private final PtzBaseMoveAbsolutePanel ptzBaseMoveAbsolutePanel = new PtzBaseMoveAbsolutePanel();
    // PTZ 绝对移动 ( MoveAbsolutely )
    private final PtzMoveAbsolutePanel ptzMoveAbsolutePanel = new PtzMoveAbsolutePanel();
    // PTZ 三维精确定位 ( Exact Goto )
    private final PtzExactGotoPanel ptzExactGotoPanel = new PtzExactGotoPanel();
    // PTZ 方向控制 面板
    private final PtzDirControlPanel ptzDirControlPanel = new PtzDirControlPanel();

    public PtzControlPanel() {
        SwingUtil.setBorderEx(this, "云台控制", 2);
        setLayout(new GridLayout(3, 1));
        Dimension dim = getPreferredSize();
        dim.width = 510;
        setPreferredSize(dim);

        JPanel placeHolder1 = new JPanel();
        placeHolder1.setLayout(new FlowLayout());
        JPanel placeHolder2 = new JPanel();
        placeHolder2.setLayout(new FlowLayout());
        JPanel placeHolder3 = new JPanel();
        placeHolder3.setLayout(new FlowLayout());

        placeHolder1.add(ptzDirControlPanel);
        placeHolder1.add(ptzModifyPanel);

        placeHolder2.add(ptzBaseMoveAbsolutePanel);

        placeHolder3.add(ptzMoveAbsolutePanel);
        placeHolder3.add(ptzExactGotoPanel);

        add(placeHolder1);
        add(placeHolder2);
        add(placeHolder3);
    }

    public PtzModifyPanel getPtzModifyPanel() {
        return ptzModifyPanel;
    }

    public PtzDirControlPanel getPtzDirControlPanel() {
        return ptzDirControlPanel;
    }

    public PtzBaseMoveAbsolutePanel getPtzBaseMoveAbsolutePanel() {
        return ptzBaseMoveAbsolutePanel;
    }

    public PtzMoveAbsolutePanel getPtzMoveAbsolutePanel() {
        return ptzMoveAbsolutePanel;
    }

    public PtzExactGotoPanel getPtzExactGotoPanel() {
        return ptzExactGotoPanel;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
