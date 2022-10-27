package com.netsdk.demo.customize.ptzNewDemo.frame;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Swing 工具类
 *
 * @author 47040
 * @since Created in 2021/3/26 18:42
 */
public class SwingUtil {

    // 设置边框
    public static void setBorderEx(JComponent object, String title, int margin) {
        Border innerBorder = BorderFactory.createTitledBorder(title);
        Border outerBorder = BorderFactory.createEmptyBorder(margin, margin, margin, margin);
        object.setBorder(BorderFactory.createCompoundBorder(outerBorder, innerBorder));
    }

    // 禁用 Container 内所有组件
    public static void SetEnableAllInnerComponent(Component container, boolean enable) {
        for (Component component : getComponents(container)) {
            component.setEnabled(enable);
        }
    }

    // 获取 Swing Container 内所有的非 Container 组件
    public static Component[] getComponents(Component container) {
        ArrayList<Component> list;
        try {
            list = new ArrayList<Component>(Arrays.asList(
                    ((Container) container).getComponents()));
            for (int index = 0; index < list.size(); index++) {
                list.addAll(Arrays.asList(getComponents(list.get(index))));
            }
        } catch (ClassCastException e) {
            list = new ArrayList<Component>();
        }
        return list.toArray(new Component[0]);
    }

}
