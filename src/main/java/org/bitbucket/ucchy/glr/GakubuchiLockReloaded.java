/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.io.File;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ItemFrame Lock Plugin
 * @author ucchy
 */
public class GakubuchiLockReloaded extends JavaPlugin {

    private static final String DATA_FOLDER = "data";

    private LockDataManager lockManager;
    private GakubuchiLockConfig config;
    private GakubuchiLockCommand command;

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

        // コマンドクラスを作成する
        command = new GakubuchiLockCommand(this);
    }

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return this.command.onCommand(sender, command, label, args);
    }

    /**
     * @see org.bukkit.plugin.java.JavaPlugin#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return this.command.onTabComplete(sender, command, alias, args);
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
