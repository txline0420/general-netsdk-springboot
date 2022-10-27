package com.netsdk.demo.customize.surfaceEventDemo.frame;

import com.netsdk.demo.customize.surfaceEventDemo.frame.basic.CanvasPanel;
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
 * @since Created in 2021/5/11 13:52
 */
public class EventPicPanel extends JPanel {

    private final CanvasPanel canvasPanel = new CanvasPanel();

    public EventPicPanel() {
        SwingUtil.setBorderEx(this, "事件图片", 2);
        setLayout(new BorderLayout());

        add(canvasPanel, BorderLayout.CENTER);
    }

    public JPanel getCanvasPanel() {
        return canvasPanel;
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

    // 清空数据
    public void clearPic() {
        canvasPanel.showCaptureOnPanel(null);
    }
}
