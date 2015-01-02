package org.bitbucket.ucchy.glr;

import java.util.ArrayList;
import java.util.List;

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

    private static final String PERMISSION = "gakubuchilock.command";
    private static final String PERMISSION_INFINITE_PLACE =
            "gakubuchilock.entity.infinite-place";

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
        int limit = config.getItemFrameLimit();
        if ( limit >= 0 && !player.hasPermission(PERMISSION_INFINITE_PLACE) ) {
            player.sendMessage(Messages.getMessageWithKeywords(
                    "InformationLimits",
                    new String[]{"%now", "%limit"},
                    new String[]{Integer.toString(now), Integer.toString(limit)}));
        } else {
            player.sendMessage(Messages.getMessageWithKeywords(
                    "InformationLimits",
                    new String[]{"%now", "%limit"},
                    new String[]{Integer.toString(now), Messages.get("Infinity")}));
        }
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
    }
}
