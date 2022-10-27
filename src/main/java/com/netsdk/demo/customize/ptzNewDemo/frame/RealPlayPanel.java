package com.netsdk.demo.customize.ptzNewDemo.frame;

import com.netsdk.demo.customize.ptzNewDemo.frame.basic.AwtCanvasPanel;

import javax.swing.*;
import java.awt.*;

/**
 * 实时预览 面板
 *
 * @author 47040
 * @since Created in 2021/3/25 19:41
 */
public class RealPlayPanel extends JPanel {

    private final Panel canvasPanel = new AwtCanvasPanel();

    public RealPlayPanel() {
        SwingUtil.setBorderEx(this, "实时预览", 2);
        setLayout(new BorderLayout());

        add(canvasPanel, BorderLayout.CENTER);
    }

    public Panel getCanvasPanel() {
        return canvasPanel;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
