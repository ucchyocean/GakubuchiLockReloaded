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

    private LockDataManager manager;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // マネージャを生成し、データをロードする
        manager = new LockDataManager(new File(getDataFolder(), DATA_FOLDER));

        // リスナークラスを登録する
        getServer().getPluginManager().registerEvents(
                new GakubuchiPlayerListener(manager), this);
    }

    /**
     * ロックデータマネージャを返す
     * @return ロックデータマネージャ
     */
    public LockDataManager getLockDataManager() {
        return manager;
    }
}
