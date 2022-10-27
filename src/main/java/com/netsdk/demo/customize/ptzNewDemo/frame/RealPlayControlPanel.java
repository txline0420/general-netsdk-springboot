package com.netsdk.demo.customize.ptzNewDemo.frame;

import javax.swing.*;
import java.awt.*;

/**
 * 播放控制面板
 *
 * @author 47040
 * @since Created in 2021/3/25 19:41
 */
public class RealPlayControlPanel extends JPanel {

    private final JComboBox<String> channelComboBox = new JComboBox<String>(new String[]{" 通道: 0 "});

    private final JButton playBtn = new JButton("开始播放");
    private final JButton stopPlayBtn = new JButton("结束播放");

    public RealPlayControlPanel() {
        SwingUtil.setBorderEx(this, "播放控制", 2);
        setLayout(new FlowLayout());
        Dimension dim = getPreferredSize();
        dim.setSize(480, 70);
        setPreferredSize(dim);

        JLabel focusMapLabel = new JLabel(" 选择通道:");

        add(focusMapLabel);
        add(channelComboBox);
        add(playBtn);
        add(stopPlayBtn);
    }

    public JComboBox<?> getChannelComboBox() {
        return channelComboBox;
    }

    public JButton getPlayBtn() {
        return playBtn;
    }

    public JButton getStopPlayBtn() {
        return stopPlayBtn;
    }

    public void resetChannelComboBox(int channelCount) {
        String[] channelLists = new String[channelCount];
        for (int i = 0; i < channelCount; i++) {
            channelLists[i] = String.format(" 通道: %s ", i);
        }
        channelComboBox.setModel(new DefaultComboBoxModel<String>(channelLists));
        channelComboBox.setSelectedIndex(0);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        channelComboBox.setSelectedIndex(0);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
