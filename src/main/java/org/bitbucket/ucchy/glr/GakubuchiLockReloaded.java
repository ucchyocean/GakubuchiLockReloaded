/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.io.File;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ItemFrame Lock Plugin
 * @author ucchy
 */
public class GakubuchiLockReloaded extends JavaPlugin {

    private static final String DATA_FOLDER = "data";

    protected static final String PERMISSION_INFINITE_PLACE =
            "gakubuchilock.entity.infinite-place";

    private LockDataManager lockManager;
    private GakubuchiLockConfig config;
    private GakubuchiLockCommand command;

    private PermissionsExBridge pex;

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

        // メッセージをロードする
        Messages.initialize(getFile(), getDataFolder());
        Messages.reload(config.getLang());

        // PermissionsExをロード
        if ( getServer().getPluginManager().isPluginEnabled("PermissionsEx") ) {
            pex = PermissionsExBridge.load(
                    getServer().getPluginManager().getPlugin("PermissionsEx"));
        }

        // リスナークラスを登録する
        getServer().getPluginManager().registerEvents(
                new GakubuchiLockListener(this), this);

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
    protected LockDataManager getLockDataManager() {
        return lockManager;
    }

    /**
     * コンフィグデータを返す
     * @return
     */
    protected GakubuchiLockConfig getGLConfig() {
        return config;
    }

    /**
     * PermissionsExへのアクセスブリッジを取得する
     * @return PermissionsExBridge、ロードされていなければnullになる
     */
    public PermissionsExBridge getPex() {
        return pex;
    }

    /**
     * このプラグインのJarファイルを返す
     * @return Jarファイル
     */
    protected File getJarFile() {
        return getFile();
    }

    /**
     * このプラグインのインスタンスを返す。
     * @return インスタンス
     */
    protected static GakubuchiLockReloaded getInstance() {
        return (GakubuchiLockReloaded)Bukkit.getPluginManager().getPlugin("GakubuchiLockReloaded");
    }
}
