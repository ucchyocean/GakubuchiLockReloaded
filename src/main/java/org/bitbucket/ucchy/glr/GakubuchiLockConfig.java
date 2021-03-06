/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.io.File;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * コンフィグ管理クラス
 * @author ucchy
 */
public class GakubuchiLockConfig {

    private GakubuchiLockReloaded parent;

    private String lang;
    private int itemFrameLimit;
    private WallMode wallMode;
    private boolean autoLock;
    private int cleanupTaskDelay;
    private boolean cleanupTaskLog;

    /**
     * コンストラクタ
     * @param parent
     */
    public GakubuchiLockConfig(GakubuchiLockReloaded parent) {

        this.parent = parent;
        reloadConfig();
    }

    /**
     * コンフィグを読み込む
     */
    protected void reloadConfig() {

        if ( !parent.getDataFolder().exists() ) {
            parent.getDataFolder().mkdirs();
        }

        File file = new File(parent.getDataFolder(), "config.yml");
        if ( !file.exists() ) {
            Utility.copyFileFromJar(
                    parent.getJarFile(), file, "config_ja.yml", false);
        }

        parent.reloadConfig();
        FileConfiguration conf = parent.getConfig();

        lang = conf.getString("lang", "ja");
        itemFrameLimit = conf.getInt("itemFrameLimit", 100);
        wallMode = WallMode.fromString(
                conf.getString("wallMode"), WallMode.REGEN_STONE_NO_DROP);
        autoLock = conf.getBoolean("autoLock", true);

        cleanupTaskDelay = conf.getInt("cleanupTaskDelay", -1);
        cleanupTaskLog = conf.getBoolean("cleanupTaskLog", true);
    }

    /**
     * @return lang
     */
    public String getLang() {
        return lang;
    }

    /**
     * @return itemFrameLimit
     */
    public int getItemFrameLimit() {
        return itemFrameLimit;
    }

    /**
     * @return wallMode
     */
    public WallMode getWallMode() {
        return wallMode;
    }

    /**
     * @return autoLock
     */
    public boolean isAutoLock() {
        return autoLock;
    }

    /**
     * @return cleanupTaskDelay
     */
    public int getCleanupTaskDelay() {
        return cleanupTaskDelay;
    }

    /**
     * @return cleanupTaskLog
     */
    public boolean isCleanupTaskLog() {
        return cleanupTaskLog;
    }
}
