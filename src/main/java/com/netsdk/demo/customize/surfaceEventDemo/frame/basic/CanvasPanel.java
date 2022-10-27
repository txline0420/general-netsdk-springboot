package com.netsdk.demo.customize.surfaceEventDemo.frame.basic;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * @author 47040
 * @since Created in 2021/5/12 19:37
 */
public class CanvasPanel extends JPanel {

    private BufferedImage image;

    public CanvasPanel() {
        super();
        setBackground(new Color(163, 163, 163));
        setForeground(new Color(0, 0, 0));
    }

    // 展示图片
    public void showCaptureOnPanel(BufferedImage bufferImg) {
        if(bufferImg==null){
            this.setOpaque(true);
            this.setImage(null);
        }else{
            this.setOpaque(false);
            this.setImage(bufferImg);
        }
        this.repaint();
    }

    private void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (image != null) {
            //绘制图片与组件大小相同
            g.drawImage(image, 0, 0, getWidth(), getHeight(), this);
        }
        super.paintComponent(g);
    }
}
