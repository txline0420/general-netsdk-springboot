package com.netsdk.demo.customize.surfaceEventDemo.frame;

import com.netsdk.demo.customize.surfaceEventDemo.frame.basic.SwingUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

/**
 * @author 47040
 * @since Created in 2021/5/11 11:01
 */
public class EventGroupPanel extends JPanel {

    private final EventDetailPanel detailPanel01 = new EventDetailPanel("index 0");
    private final EventDetailPanel detailPanel02 = new EventDetailPanel("index 1");
    private final EventDetailPanel detailPanel03 = new EventDetailPanel("index 2");
    private final EventDetailPanel detailPanel04 = new EventDetailPanel("index 3");

    private final ArrayList<EventDetailPanel> groupPanels = new ArrayList<>();

    public EventGroupPanel(String title) {
        SwingUtil.setBorderEx(this, title, 2);
        setLayout(new GridLayout(4, 1));
        Dimension dim = getPreferredSize();
        dim.width = 480;
        dim.height = 1024;
        setPreferredSize(dim);

        add(detailPanel01);
        add(detailPanel02);
        add(detailPanel03);
        add(detailPanel04);

        groupPanels.add(detailPanel01);
        groupPanels.add(detailPanel02);
        groupPanels.add(detailPanel03);
        groupPanels.add(detailPanel04);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        SwingUtil.SetEnableAllInnerComponent(this, enabled);
    }

    public EventDetailPanel getDetailPanel01() {
        return detailPanel01;
    }

    public EventDetailPanel getDetailPanel02() {
        return detailPanel02;
    }

    public EventDetailPanel getDetailPanel03() {
        return detailPanel03;
    }

    public EventDetailPanel getDetailPanel04() {
        return detailPanel04;
    }

    public ArrayList<EventDetailPanel> getGroupPanels() {
        return groupPanels;
    }

    // 清空数据
    public void clearAll() {
        for (EventDetailPanel e : groupPanels) {
            e.clearPicAndInfo();
        }
    }
}
