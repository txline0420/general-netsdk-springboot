package com.netsdk.demo.customize.surfaceEventDemo.frame;

import com.netsdk.demo.customize.surfaceEventDemo.frame.basic.SwingUtil;

import javax.swing.*;
import java.awt.*;

/**
 * 播放控制面板
 *
 * @author 47040
 * @since Created in 2021/3/25 19:41
 */
public class ControlPanel extends JPanel {

    private final JComboBox<String> channelComboBox = new JComboBox<>(new String[]{" 通道号:   1 "});

    private final JButton playBtn = new JButton("开始播放");
    private final JButton stopPlayBtn = new JButton("结束播放");

    private final JButton attachBtn = new JButton("全订阅");
    private final JButton detachBtn = new JButton("全退订");
    private final JButton clearBtn = new JButton("清空数据");

    public ControlPanel() {
        SwingUtil.setBorderEx(this, "控制", 2);
        setLayout(new GridLayout(3, 1));
        Dimension dim = getPreferredSize();
        dim.setSize(340, 120);
        setPreferredSize(dim);

        JLabel channelLabel = new JLabel("选择通道: ");

        JPanel placeHolder01 = new JPanel();
        placeHolder01.setLayout(new FlowLayout(FlowLayout.LEFT));
        placeHolder01.add(channelLabel);
        placeHolder01.add(channelComboBox);

        JLabel realPlayLabel = new JLabel("视频预览: ");
        JPanel placeHolder02 = new JPanel();
        placeHolder02.setLayout(new FlowLayout(FlowLayout.LEFT));
        placeHolder02.add(realPlayLabel);
        placeHolder02.add(playBtn);
        placeHolder02.add(stopPlayBtn);

        JLabel attachLabel = new JLabel("事件订阅: ");
        JPanel placeHolder03 = new JPanel();
        placeHolder03.setLayout(new FlowLayout(FlowLayout.LEFT));
        placeHolder03.add(attachLabel);
        placeHolder03.add(attachBtn);
        placeHolder03.add(detachBtn);
        placeHolder03.add(clearBtn);

        add(placeHolder01);
        add(placeHolder02);
        add(placeHolder03);
    }

    public JComboBox<?> getChannelComboBox() {
        return channelComboBox;
    }

    public int getSelectedSDKChannel() {
        return channelComboBox.getSelectedIndex();
    }

    public JButton getPlayBtn() {
        return playBtn;
    }

    public JButton getStopPlayBtn() {
        return stopPlayBtn;
    }

    public JButton getAttachBtn() {
        return attachBtn;
    }

    public JButton getDetachBtn() {
        return detachBtn;
    }

    public JButton getClearBtn() {
        return clearBtn;
    }

    public void resetChannelComboBox(int channelCount) {
        String[] channelLists = new String[channelCount];
        for (int i = 0; i < channelCount; i++) {
            channelLists[i] = String.format(" 通道号: %3d ", i + 1);
        }
        channelComboBox.setModel(new DefaultComboBoxModel<>(channelLists));
        channelComboBox.setSelectedIndex(0);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        channelComboBox.setSelectedIndex(0);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
