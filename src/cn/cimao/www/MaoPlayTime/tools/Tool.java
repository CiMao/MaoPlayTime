package cn.cimao.www.MaoPlayTime.tools;

public class Tool {
    public static String rlore (String lore) {
        lore = lore.replaceAll("&","§");
        return lore;
    }
}
