package com.netsdk.demo.customize.surfaceEventDemo.frame;

import com.netsdk.demo.customize.surfaceEventDemo.frame.basic.CanvasPanel;
import com.netsdk.demo.customize.surfaceEventDemo.frame.basic.EventInfoPanel;
import com.netsdk.demo.customize.surfaceEventDemo.frame.basic.SwingUtil;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author 47040
 * @since Created in 2021/5/11 9:41
 */
public class EventDetailPanel extends JPanel {

    private final CanvasPanel canvasPanel = new CanvasPanel();

    private final EventInfoPanel eventInfoPanel = new EventInfoPanel();

    public EventDetailPanel(String title) {
        SwingUtil.setBorderEx(this, title, 2);
        setLayout(new BorderLayout());
        Dimension dim = getPreferredSize();
        dim.width = 480;
        dim.height = 200;
        setPreferredSize(dim);

        add(canvasPanel, BorderLayout.CENTER);
        add(eventInfoPanel, BorderLayout.EAST);
    }

    public CanvasPanel getCanvasPanel() {
        return canvasPanel;
    }

    public EventInfoPanel getEventInfoPanel() {
        return eventInfoPanel;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }

    // 展示图片
    public void showPicOnCanvas(byte[] data) {
        BufferedImage buffImg = null;
        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(data);
            buffImg = ImageIO.read(new BufferedInputStream(inputStream));
            if (buffImg == null) return;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        canvasPanel.showCaptureOnPanel(buffImg);
    }

    // 展示详情
    public void setDetail(String detail) {
        eventInfoPanel.getDetail().setText(detail);
    }

    // 清空数据
    public void clearPicAndInfo() {
        eventInfoPanel.getDetail().setText("");
        canvasPanel.showCaptureOnPanel(null);
    }
}
