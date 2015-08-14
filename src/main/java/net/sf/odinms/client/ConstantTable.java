/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.odinms.client;

import java.util.Calendar;

/**
 *
 * @author Administrator
 */
public class ConstantTable {

    public static final String _S_ONLINE_MINUTE = "_ONLINE_MINUTE";
    public static final String _PLAYER_DATA_LOGINTIME = "_LoginTime";
    private static String CURRENT_DAY;
    private static Calendar now = Calendar.getInstance();

    static {
        refCurrentDay();
    }

    private static void refCurrentDay() {
        CURRENT_DAY = ((Integer) now.get(Calendar.DAY_OF_YEAR)).toString();
    }

    public static String getCurrentDay() {
        if (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR)) {
            now = Calendar.getInstance();
            refCurrentDay();
        }
        return CURRENT_DAY;
    }
}
