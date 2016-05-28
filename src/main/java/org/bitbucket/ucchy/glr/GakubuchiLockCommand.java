package org.bitbucket.ucchy.glr;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * GakubuchiLockのコマンドクラス
 * @author ucchy
 */
public class GakubuchiLockCommand implements TabExecutor {

    protected static final String META_INFO_COMMAND = "gakubuchiinfo";
    protected static final String META_PRIVATE_COMMAND = "gakubuchiprivate";
    protected static final String META_REMOVE_COMMAND = "gakubuchiremove";
    protected static final String META_PERSIST_MODE = "gakubuchipersist";

    private static final String PERMISSION = "gakubuchilock.command";

    private GakubuchiLockReloaded parent;
    private LockDataManager lockManager;
    private GakubuchiLockConfig config;

    /**
     * コンストラクタ
     * @param parent
     */
    public GakubuchiLockCommand(GakubuchiLockReloaded parent) {
        this.parent = parent;
        this.lockManager = parent.getLockDataManager();
        this.config = parent.getGLConfig();
    }

    /**
     * コマンドが実行された時に呼び出されるメソッド
     * @see org.bukkit.command.CommandExecutor#onCommand(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // 各種短縮コマンド
        if ( command.getName().equals("ginfo") ) {
            return doInfo(sender, command, label, args);
        } else if ( command.getName().equals("glimits") ) {
            return doLimits(sender, command, label, args);
        } else if ( command.getName().equals("gprivate") ) {
            return doPrivate(sender, command, label, args);
        } else if ( command.getName().equals("gremove") ) {
            return doRemove(sender, command, label, args);
        } else if ( command.getName().equals("gpersist") ) {
            return doPersist(sender, command, label, args);
        }

        // 以下、/gakubuchilock コマンドに対する処理

        // 引数なしの場合は、falseを返してusage表示
        if ( args.length == 0 ) {
            return false;
        }

        // 後は、そのままそれぞれのサブコマンドを実行するようにする。
        if ( args[0].equalsIgnoreCase("info") ) {
            return doInfo(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("limits") ) {
            return doLimits(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("private") ) {
            return doPrivate(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("remove") ) {
            return doRemove(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("persist") ) {
            return doPersist(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("cleanup") ) {
            return doCleanup(sender, command, label, args);
        } else if ( args[0].equalsIgnoreCase("reload") ) {
            return doReload(sender, command, label, args);
        }

        return false;
    }

    /**
     * タブキーで補完された時に呼び出されるメソッド
     * @see org.bukkit.command.TabCompleter#onTabComplete(org.bukkit.command.CommandSender, org.bukkit.command.Command, java.lang.String, java.lang.String[])
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if ( command.getName().equals("gakubuchilock") ) {
            if ( args.length == 1 ) {
                String pre = args[0].toLowerCase();
                ArrayList<String> kouho = new ArrayList<String>();
                for ( String com : new String[]{"info", "limits", "private", "remove", "reload"} ) {
                    if ( com.startsWith(pre) && sender.hasPermission(PERMISSION + "." + com) ) {
                        kouho.add(com);
                    }
                }
                return kouho;
            }
        }
        return null;
    }

    /**
     * infoコマンドを実行する
     * @param sender 実行者
     * @param command コマンド
     * @param label ラベル
     * @param args 引数
     * @return コマンド実行が成功したかどうか（falseを返すとusageを表示する）
     */
    private boolean doInfo(CommandSender sender, Command command, String label, String[] args) {

        if  ( !sender.hasPermission(PERMISSION + ".info") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        if  ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("NotInGame"));
            return true;
        }

        Player player = (Player)sender;

        // プレイヤーにメタデータを仕込む
        removeAllMetadata(player);
        player.setMetadata(META_INFO_COMMAND, new FixedMetadataValue(parent, false));
        sender.sendMessage(Messages.get("PunchInfo"));
        return true;
    }

    /**
     * limitsコマンドを実行する
     * @param sender 実行者
     * @param command コマンド
     * @param label ラベル
     * @param args 引数
     * @return コマンド実行が成功したかどうか（falseを返すとusageを表示する）
     */
    private boolean doLimits(CommandSender sender, Command command, String label, String[] args) {

        if  ( !sender.hasPermission(PERMISSION + ".limits") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        if  ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("NotInGame"));
            return true;
        }

        Player player = (Player)sender;

        // 現在の設置数と、制限数を取得し、表示する。
        int now = lockManager.getPlayerLockNum(player.getUniqueId());
        int limit = lockManager.getPlayerStandLimit(player);
        String limits = limit < 0 ? Messages.get("Infinity") : limit + "";
        player.sendMessage(Messages.getMessageWithKeywords(
                "InformationLimits",
                new String[]{"%now", "%limit"},
                new String[]{Integer.toString(now), limits}));
        return true;
    }

    /**
     * privateコマンドを実行する
     * @param sender 実行者
     * @param command コマンド
     * @param label ラベル
     * @param args 引数
     * @return コマンド実行が成功したかどうか（falseを返すとusageを表示する）
     */
    private boolean doPrivate(CommandSender sender, Command command, String label, String[] args) {

        if  ( !sender.hasPermission(PERMISSION + ".private") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        if  ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("NotInGame"));
            return true;
        }

        Player player = (Player)sender;

        // プレイヤーにメタデータを仕込む
        removeAllMetadata(player);
        player.setMetadata(META_PRIVATE_COMMAND, new FixedMetadataValue(parent, false));
        sender.sendMessage(Messages.get("PunchLock"));
        return true;
    }

    /**
     * removeコマンドを実行する
     * @param sender 実行者
     * @param command コマンド
     * @param label ラベル
     * @param args 引数
     * @return コマンド実行が成功したかどうか（falseを返すとusageを表示する）
     */
    private boolean doRemove(CommandSender sender, Command command, String label, String[] args) {

        if  ( !sender.hasPermission(PERMISSION + ".remove") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        if  ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("NotInGame"));
            return true;
        }

        Player player = (Player)sender;

        // プレイヤーにメタデータを仕込む
        removeAllMetadata(player);
        player.setMetadata(META_REMOVE_COMMAND, new FixedMetadataValue(parent, false));
        sender.sendMessage(Messages.get("PunchUnlock"));
        return true;
    }

    /**
     * persistコマンドを実行する
     * @param sender 実行者
     * @param command コマンド
     * @param label ラベル
     * @param args 引数
     * @return コマンド実行が成功したかどうか（falseを返すとusageを表示する）
     */
    private boolean doPersist(CommandSender sender, Command command, String label, String[] args) {

        if  ( !sender.hasPermission(PERMISSION + ".persist") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        if  ( !(sender instanceof Player) ) {
            sender.sendMessage(Messages.get("NotInGame"));
            return true;
        }

        Player player = (Player)sender;

        // コマンドパラメータの解析
        ArrayList<String> queue = new ArrayList<String>();
        for ( String a : args ) {
            queue.add(a);
        }

        boolean isInfo = false;
        boolean isLock = false;
        boolean isOn = true;
        if ( queue.size() > 0 && queue.get(0).equalsIgnoreCase("persist") ) {
            queue.remove(0);
        }
        if ( queue.size() > 0 && queue.get(0).equalsIgnoreCase("info") ) {
            isInfo = true;
        } else if ( queue.size() > 0 && queue.get(0).equalsIgnoreCase("lock") ) {
            isLock = true;
        } else if ( queue.size() > 0 && queue.get(0).equalsIgnoreCase("unlock") ) {
            isLock = false;
        } else if ( queue.size() > 0 && queue.get(0).equalsIgnoreCase("off") ) {
            isOn = false;
        } else {
            // パラメータ無し、または無効な文字列なら、
            // - モードオフの状態ならロックモードにする。
            // - モードオンの状態ならオフにする。
            if ( !player.hasMetadata(META_PERSIST_MODE) ) {
                isLock = true;
            } else {
                isOn = false;
            }
        }

        if ( !isOn ) {
            // オフにする
            removeAllMetadata(player);
            sender.sendMessage(Messages.get("PersistOff"));
        } else {
            removeAllMetadata(player); // 一旦全解除
            if ( isInfo ) {
                // 連続インフォモードにする
                player.setMetadata(META_INFO_COMMAND, new FixedMetadataValue(parent, false));
                player.setMetadata(META_PERSIST_MODE, new FixedMetadataValue(parent, false));
                sender.sendMessage(Messages.get("PersistMode"));
                sender.sendMessage(Messages.get("PunchInfo"));
            } else if ( isLock ) {
                // 連続ロックモードにする
                player.setMetadata(META_PRIVATE_COMMAND, new FixedMetadataValue(parent, false));
                player.setMetadata(META_PERSIST_MODE, new FixedMetadataValue(parent, false));
                sender.sendMessage(Messages.get("PersistMode"));
                sender.sendMessage(Messages.get("PunchLock"));
            } else {
                // 連続解除モードにする
                player.setMetadata(META_REMOVE_COMMAND, new FixedMetadataValue(parent, false));
                player.setMetadata(META_PERSIST_MODE, new FixedMetadataValue(parent, false));
                sender.sendMessage(Messages.get("PersistMode"));
                sender.sendMessage(Messages.get("PunchUnlock"));
            }
        }

        return true;
    }

    /**
     * cleanupコマンドを実行する
     * @param sender 実行者
     * @param command コマンド
     * @param label ラベル
     * @param args 引数
     * @return コマンド実行が成功したかどうか（falseを返すとusageを表示する）
     */
    private boolean doCleanup(CommandSender sender, Command command, String label, String[] args) {

        if  ( !sender.hasPermission(PERMISSION + ".cleanup") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // ワールドが存在するか確認する
        String worldName = "world";
        if ( args.length >= 2 ) {
            worldName = args[1];
        }
        if ( Bukkit.getWorld(worldName) == null ) {
            sender.sendMessage(Messages.get("WorldNotFound"));
            return true;
        }

        // ワールドにロックデータが存在するか確認する
        int num = lockManager.getWorldLockDataNum(worldName);
        if ( num <= 0 ) {
            sender.sendMessage(Messages.get("WorldLockDataNotFound"));
            return true;
        }

        // データをクリーンアップする
        lockManager.cleanupWorldLockData(worldName);
        sender.sendMessage(Messages.getMessageWithKeywords("InformationCleanup",
                new String[]{"%world", "%num"}, new String[]{worldName, num + ""}));
        return true;
    }

    /**
     * reloadコマンドを実行する
     * @param sender 実行者
     * @param command コマンド
     * @param label ラベル
     * @param args 引数
     * @return コマンド実行が成功したかどうか（falseを返すとusageを表示する）
     */
    private boolean doReload(CommandSender sender, Command command, String label, String[] args) {

        if  ( !sender.hasPermission(PERMISSION + ".reload") ) {
            sender.sendMessage(Messages.get("PermissionDeniedCommand"));
            return true;
        }

        // データをリロードする
        lockManager.reloadData();
        config.reloadConfig();
        Messages.reload(config.getLang());
        sender.sendMessage(Messages.get("InformationReload"));
        return true;
    }

    /**
     * GakubuchiLockのメタデータを削除する
     * @param player
     */
    private void removeAllMetadata(Player player) {
        player.removeMetadata(META_INFO_COMMAND, parent);
        player.removeMetadata(META_PRIVATE_COMMAND, parent);
        player.removeMetadata(META_REMOVE_COMMAND, parent);
        player.removeMetadata(META_PERSIST_MODE, parent);
    }
}
