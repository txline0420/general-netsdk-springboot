package com.netsdk.demo.util;

import javax.swing.*;
import java.awt.*;

/**
 * 带背景的面板组件
 * @author 29779
 *
 */
public class PaintJPanel extends JPanel{

	private static final long serialVersionUID = 1L;
	private Image image; //背景图片	
	
	public PaintJPanel() {
		super();
		setOpaque(true); //非透明
		setLayout(null);
		setBackground(new Color(153, 240, 255));
		setForeground(new Color(0, 0, 0));
	}
	
	//设置图片的方法
	public void setImage(Image image) {
		this.image = image;
	}
	
	protected void paintComponent(Graphics g) {    //重写绘制组件外观
		if(image != null) {
			g.drawImage(image, 0, 0, getWidth(), getHeight(), this);//绘制图片与组件大小相同
		}
		super.paintComponent(g); // 执行超类方法
	}	

}
