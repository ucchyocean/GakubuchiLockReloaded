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
    private static final String COMPENSATION_FOLDER = "compensation";

    private LockDataManager lockManager;
    private CompensationDataManager compManager;

    /**
     * プラグインが有効化されたときに呼び出されるメソッド
     * @see org.bukkit.plugin.java.JavaPlugin#onEnable()
     */
    @Override
    public void onEnable() {

        // マネージャを生成し、データをロードする
        lockManager = new LockDataManager(
                new File(getDataFolder(), DATA_FOLDER));
        compManager = new CompensationDataManager(
                new File(getDataFolder(), COMPENSATION_FOLDER));

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
     * 補償アイテムデータマネージャを返す
     * @return 補償アイテムデータマネージャ
     */
    public CompensationDataManager getCompensationDataManager() {
        return compManager;
    }
}
