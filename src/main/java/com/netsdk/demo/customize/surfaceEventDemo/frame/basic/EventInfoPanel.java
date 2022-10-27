package com.netsdk.demo.customize.surfaceEventDemo.frame.basic;

import javax.swing.*;
import java.awt.*;

/**
 * @author 47040
 * @since Created in 2021/5/11 10:10
 */
public class EventInfoPanel extends JPanel {

    private final JTextArea detail;

    public EventInfoPanel(){
        SwingUtil.setBorderEx(this, "", 2);
        setLayout(new BorderLayout());
        Dimension dim = getPreferredSize();
        dim.width = 250;
        dim.height = 200;
        setPreferredSize(dim);

        JPanel placeHolder01 = new JPanel();
        placeHolder01.setLayout(new BorderLayout());

        detail = new JTextArea();
        detail.setEditable(false);

        placeHolder01.add(detail, BorderLayout.CENTER);

        this.add(placeHolder01, BorderLayout.CENTER);
    }

    public JTextArea getDetail() {
        return detail;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }
}
