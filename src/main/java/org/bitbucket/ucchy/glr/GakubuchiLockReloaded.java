/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.io.File;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * ItemFrame Lock Plugin
 * @author ucchy
 */
public class GakubuchiLockReloaded extends JavaPlugin {

    private static final String DATA_FOLDER = "data";

    private LockDataManager lockManager;
    private GakubuchiLockConfig config;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // マネージャを生成し、データをロードする
        lockManager = new LockDataManager(
                new File(getDataFolder(), DATA_FOLDER));

        // コンフィグをロードする
        config = new GakubuchiLockConfig(this);

        // リスナークラスを登録する
        getServer().getPluginManager().registerEvents(
                new GakubuchiPlayerListener(this), this);
    }

    /**
     * ロックデータマネージャを返す
     * @return ロックデータマネージャ
     */
    public LockDataManager getLockDataManager() {
        return lockManager;
    }

    /**
     * コンフィグデータを返す
     * @return
     */
    public GakubuchiLockConfig getGLConfig() {
        return config;
    }

    /**
     * このプラグインのJarファイルを返す
     * @return Jarファイル
     */
    protected File getJarFile() {
        return getFile();
    }
}
