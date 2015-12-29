/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * GakubuchiLockのリスナークラス
 * @author ucchy
 */
public class GakubuchiLockListener implements Listener {

    private static final String PERMISSION = "gakubuchilock.entity";

    private GakubuchiLockReloaded parent;
    private LockDataManager lockManager;
    private GakubuchiLockConfig config;

    private SimpleDateFormat format;

    /**
     * コンストラクタ
     * @param parent
     */
    public GakubuchiLockListener(GakubuchiLockReloaded parent) {
        this.parent = parent;
        this.lockManager = parent.getLockDataManager();
        this.config = parent.getGLConfig();

        this.format = new SimpleDateFormat(Messages.get("DateTimeFormat"));
    }

    /**
     * かべかけ物が設置された時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHangingPlace(HangingPlaceEvent event) {

        Hanging hanging = event.getEntity();

        // 額縁でなければ無視する
        if ( !(hanging instanceof ItemFrame) ) {
            return;
        }

        // 権限がなければ、操作を禁止する
        if ( !event.getPlayer().hasPermission(PERMISSION + ".place") ) {
            event.getPlayer().sendMessage(Messages.get("PermissionDeniedPlace"));
            event.setCancelled(true);
            return;
        }

        // 同じLocationを持つ既存のItemFrameが存在しないか、確認する。
        // 本プラグイン導入後は、同じ位置へのItemFrameの設置を認めない。
        Location location = hanging.getLocation();
        if ( GakubuchiUtility.getFrameFromLocation(location) != null ) {
            event.setCancelled(true);
            return;
        }

        if ( config.isAutoLock() ) {
            // 自動ロック処理

            // 設置数制限を超える場合は、設置を許可しない。
            int locknum = lockManager.getPlayerLockNum(event.getPlayer().getUniqueId());
            int limit = lockManager.getPlayerStandLimit(event.getPlayer());
            if ( limit >= 0 && locknum >= limit ) {
                event.getPlayer().sendMessage(Messages.get("ExceedLockLimit"));
                event.setCancelled(true);
                return;
            }

            // 新しいロックデータを登録する
            lockManager.addLockData(event.getPlayer().getUniqueId(), hanging);
            event.getPlayer().sendMessage(Messages.get("Locked"));
        }
    }

    /**
     * かべかけ物が破壊された時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakEvent event) {

        Hanging hanging = event.getEntity();

        // 額縁でなければ無視する
        if ( !(hanging instanceof ItemFrame) ) {
            return;
        }

        // エンティティによる破壊なら、HangingBreakByEntityEventで処理するので、
        // ここでは何もしない。
        if ( event instanceof HangingBreakByEntityEvent ) {
            return;
        }

        // 対象物のロックデータを取得する
        LockData ld = lockManager.getLockDataByHanging(hanging);

        // ロックデータが無い場合はイベントを無視する
        if ( ld == null ) {
            return;
        }

        switch (event.getCause()) {

        case ENTITY:
            // Entityにより破壊された場合。
            // HangingBreakByEntityEventの方で処理するので、ここでは何もしない
            break;

        case EXPLOSION:
            // 爆破により破壊された場合。
            // イベントをキャンセルして復元させる。
            // ただし、かけられていた壁も爆破で消滅していた場合は、
            // しばらくした後に（100ticks後くらい）同じイベントがPHYSICSで呼び出される。
            event.setCancelled(true);
            break;

        case OBSTRUCTION:
            // 額縁のある場所が、ブロックに塞がれた場合。
            // CB1.7.10-R0.1では、Hangingの設置方向によっては、
            // なぜかOBSTRUCTIONではなくPHYSICSになる（不具合？）。

            // hangingにかぶさる位置のブロックをAIRにし、イベントをキャンセルする
            Block obst = hanging.getLocation().getBlock();
            obst.setType(Material.AIR);

            event.setCancelled(true);
            break;

        case PHYSICS:
            // 額縁のかかっている壁のブロックが無くなったり、
            // 壁掛け物として不自然な状態になったりしたとき、
            // ワールドのPhysicsUpdate処理で剥がされた場合。
            // fall through
        case DEFAULT:
            // 破壊された原因が不明な場合。
            // 他のプラグインなどで、Hangingが強制除去された場合などに発生する。
            // default: のところでまとめて対応する。
            // fall through
        default:
            // 破壊原因が不明な場合。

            // PHYSICS、DEFAULTは、ここでまとめて処理する。

            // Hangingにかぶっているブロックがある場合は、AIRにして消滅させる。
            obst = hanging.getLocation().getBlock();
            obst.setType(Material.AIR);

            if ( config.getWallMode() == WallMode.EXTINCTION ) {
                // hangingエンティティを消去して、ドロップしないようにする。
                // イベントをキャンセルする。
                // ロック情報を削除する。
                lockManager.removeLockData(hanging);
                hanging.remove();
                event.setCancelled(true);

            } else if ( config.getWallMode() == WallMode.ITEM_DROP ) {
                // バニラ挙動と同様。つまり何もしない。
                // ロック情報の削除はする。
                lockManager.removeLockData(hanging);

            } else {
                // 設置されていたであろう壁の方向に石を作って、壁を復活させる。
                // イベントをキャンセルする。
                // ロック情報はそのままにする。
                Block wall = obst.getRelative(hanging.getAttachedFace());
                if ( wall.getType() == Material.AIR || wall.isLiquid() ) {
                    wall.setType(Material.STONE);
                }
                event.setCancelled(true);

            }

            break;
        }
    }

    /**
     * かべかけ物がエンティティに破壊された時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {

        Hanging hanging = event.getEntity();

        // 額縁でなければ無視する
        if ( !(hanging instanceof ItemFrame) ) {
            return;
        }

        // 事前コマンドが実行されている場合の処理
        if ( event.getRemover() instanceof Player ) {
            Player damager = (Player)event.getRemover();
            if ( processPrecommand(damager, hanging) ) {
                event.setCancelled(true);
                return;
            }
        }

        // ==== 以下、額縁に対する攻撃の保護処理 ====

        // 対象物のロックデータを取得する
        LockData ld = lockManager.getLockDataByHanging(hanging);

        // ロックデータが無い場合
        if ( ld == null ) {

            // 権限がなければ、操作を禁止する
            if ( event.getRemover() instanceof Player ) {
                Player remover = (Player)event.getRemover();
                if ( !remover.hasPermission(PERMISSION + ".break") ) {
                    remover.sendMessage(Messages.get("PermissionDeniedBreak"));
                    event.setCancelled(true);
                }
            }

            return; // ロックデータが無い場合は、ここで処理終了。
        }

        // 操作者取得
        Player remover = null;
        if ( event.getRemover() instanceof Player ) {
            remover = (Player)event.getRemover();
        } else if ( event.getRemover() instanceof Projectile ) {
            if ( ((Projectile)event.getRemover()).getShooter() instanceof Player ) {
                remover = (Player)((Projectile)event.getRemover()).getShooter();
            }
        }

        // 所有者でなくて、管理者でもなければ、操作を禁止する
        if ( remover == null ||
                (!ld.getOwnerUuid().equals(remover.getUniqueId()) &&
                 !remover.hasPermission(PERMISSION + ".admin") ) ) {
            event.setCancelled(true);
            if ( remover != null ) {
                remover.sendMessage(Messages.get("ItemFrameLocked"));
            }
            return;
        }

        // ロック情報を削除する
        lockManager.removeLockData(hanging);

        // メッセージを出す
        remover.sendMessage(Messages.get("LockRemoved"));
    }

    /**
     * プレイヤーがエンティティをクリックした時のイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {

        // クリック対象がItemFrameでなければ、イベントを無視する。
        if ( event.getRightClicked().getType() != EntityType.ITEM_FRAME ) {
            return;
        }

        // ロックデータ取得
        Hanging hanging = (Hanging)event.getRightClicked();
        LockData ld = lockManager.getLockDataByHanging(hanging);

        // ロック情報が無い場合は、権限を確認して、権限が無ければ操作をキャンセルする。
        if ( ld == null && !event.getPlayer().hasPermission(PERMISSION + ".interact") ) {
            event.getPlayer().sendMessage(Messages.get("PermissionDeniedInteract"));
            event.setCancelled(true);
            return;
        }

        // ロック情報がある場合は、所有者を確認して、所有者でなければメッセージを表示して操作をキャンセルする。
        if ( ld != null && !ld.getOwnerUuid().equals(event.getPlayer().getUniqueId()) &&
                !event.getPlayer().hasPermission(PERMISSION + ".admin") ) {
            event.getPlayer().sendMessage(Messages.get("ItemFrameLocked"));
            event.setCancelled(true);
            return;
        }
    }

    /**
     * エンティティがエンティティにダメージを与えた時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // ダメージ対象がItemFrameでなければ、イベントを無視する。
        if ( event.getEntity().getType() != EntityType.ITEM_FRAME ) {
            return;
        }

        // 事前コマンドが実行されている場合の処理
        if ( event.getDamager() instanceof Player ) {
            Player damager = (Player)event.getDamager();
            Hanging hanging = (Hanging)event.getEntity();
            if ( processPrecommand(damager, hanging) ) {
                event.setCancelled(true);
                return;
            }
        }

        // ==== 以下、額縁に対する攻撃の保護処理 ====

        // MEMO: このイベントハンドラは、
        //   額縁の中のアイテムを左クリックされた時、または、
        //   飛来物（矢や雪球など）による攻撃を受けた時に呼び出されるので、
        //   所有者を確認して、所有者でなければメッセージを表示して操作をキャンセルする。

        // ロックデータ取得
        Hanging hanging = (Hanging)event.getEntity();
        LockData ld = lockManager.getLockDataByHanging(hanging);

        // 攻撃者取得
        Player damager = null;
        if ( event.getDamager() instanceof Player ) {
            damager = (Player)event.getDamager();
        } else if ( event.getDamager() instanceof Projectile ) {
            if ( ((Projectile)event.getDamager()).getShooter() instanceof Player ) {
                damager = (Player)((Projectile)event.getDamager()).getShooter();
            }
        }

        if ( ld == null ) {
            // ロック情報が無い場合は、権限を確認して、権限が無ければ攻撃をキャンセルする。
            if ( !damager.hasPermission(PERMISSION + ".interact") ) {
                damager.sendMessage(Messages.get("PermissionDeniedInteract"));
                event.setCancelled(true);
                return;
            }

        } else if ( damager == null || (!ld.getOwnerUuid().equals(damager.getUniqueId()) &&
                !damager.hasPermission(PERMISSION + ".admin") ) ) {
            // ロック情報がある場合は、所有者を確認して、所有者でなければメッセージを表示して攻撃をキャンセルする。

            event.setCancelled(true);
            if ( damager != null ) {
                damager.sendMessage(Messages.get("ItemFrameLocked"));
            }
            return;
        }
    }

    /**
     * ブロックが置かれたときに呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBlockPlace(BlockPlaceEvent event) {

        Location location = event.getBlockPlaced().getLocation();

        // 指定された場所にItemFrameがないか確認する
        Hanging hanging = GakubuchiUtility.getFrameFromLocation(location);

        if ( hanging != null ) {

            // ロックデータ取得
            LockData ld = lockManager.getLockDataByHanging(hanging);

            // ロックデータが無いなら何もしない
            if ( ld == null ) {
                return;
            }

            // ブロック設置を禁止する（管理権限があっても、所有者でも、NGとする）。
            event.setBuild(false);
            event.setCancelled(true);
            if ( event.getPlayer() != null ) {
                event.getPlayer().sendMessage(Messages.get("BlockPlaceDenied"));
            }
            return;
        }
    }

    /**
     * ブロックが破壊されたときに呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBlockBreak(BlockBreakEvent event) {

        if ( checkBlockBreak(event.getBlock()) ) {
            if ( config.getWallMode() == WallMode.REGEN_STONE_NO_DROP ) {
                event.getBlock().setType(Material.AIR);
            } else {
                event.setCancelled(true);
            }
        }
    }

    /**
     * ブロックが燃えたときに呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBlockBurn(BlockBurnEvent event) {

        if ( checkBlockBreak(event.getBlock()) ) {
            if ( config.getWallMode() == WallMode.REGEN_STONE_NO_DROP ) {
                event.getBlock().setType(Material.AIR);
            } else {
                event.setCancelled(true);
            }
        }
    }

    /**
     * ブロックが消滅したときに呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onBlockFade(BlockFadeEvent event) {

        if ( checkBlockBreak(event.getBlock()) ) {
            if ( config.getWallMode() == WallMode.REGEN_STONE_NO_DROP ) {
                event.getBlock().setType(Material.AIR);
            } else {
                event.setCancelled(true);
            }
        }
    }

    private boolean checkBlockBreak(Block block) {

        // チェック対象のモードでなければ、何もしないで終了する
        if ( config.getWallMode() != WallMode.REGEN_STONE_NO_DROP
                && config.getWallMode() != WallMode.INVINCIBLE ) {
            return false;
        }

        // 対象のブロックに、ロックされた額縁が貼りついているかどうかを確認する。
        for ( ItemFrame frame : GakubuchiUtility.getAttachedFrameOnBlock(block) ) {
            if ( lockManager.getLockDataByHanging(frame) != null ) {
                return true;
            }
        }

        return false;
    }

    /**
     * 事前実行されたコマンドを処理する
     * @param player 実行したプレイヤー
     * @param hanging 実行対象の額縁
     * @return コマンド実行したかどうか
     */
    private boolean processPrecommand(Player player, Hanging hanging) {

        if ( player.hasMetadata(GakubuchiLockCommand.META_INFO_COMMAND) ) {

            if ( !player.hasMetadata(GakubuchiLockCommand.META_PERSIST_MODE) ) {
                player.removeMetadata(GakubuchiLockCommand.META_INFO_COMMAND, parent);
            }

            // ロックデータ取得
            LockData ld = lockManager.getLockDataByHanging(hanging);

            if ( ld == null ) {
                player.sendMessage(Messages.get("ItemFrameUnlocked"));
                return true;
            } else {
                String owner;
                if ( ld.getOwner() == null ) {
                    owner = Messages.get("UnknownUUID");
                } else {
                    owner = ld.getOwner().getName();
                }
                player.sendMessage(Messages.getMessageWithKeywords(
                        "InformationOwner",
                        new String[]{"%player", "%uuid"},
                        new String[]{owner, ld.getOwnerUuid().toString()}));

                if (ld.getDate() > 0) {
                    Date d = new Date();
                    d.setTime(ld.getDate());
                    String time = format.format(d);
                    player.sendMessage(Messages.getMessageWithKeywords(
                            "InformationTime", new String[] {"%time"}, new String[]{time}));
                }
                return true;
            }

        } else if ( player.hasMetadata(GakubuchiLockCommand.META_PRIVATE_COMMAND) ) {

            if ( !player.hasMetadata(GakubuchiLockCommand.META_PERSIST_MODE) ) {
                player.removeMetadata(GakubuchiLockCommand.META_PRIVATE_COMMAND, parent);
            }

            // ロックデータ取得
            LockData ld = lockManager.getLockDataByHanging(hanging);

            if ( ld == null ) {

                // 設置数制限を超える場合は、設置を許可しない。
                int locknum = lockManager.getPlayerLockNum(player.getUniqueId());
                int limit = lockManager.getPlayerStandLimit(player);
                if ( limit >= 0 && locknum >= limit ) {
                    player.sendMessage(Messages.get("ExceedLockLimit"));
                    return true;
                }

                // 新しいロックデータを登録する
                lockManager.addLockData(player.getUniqueId(), hanging);
                player.sendMessage(Messages.get("Locked"));
                return true;

            } else {
                player.sendMessage(Messages.get("ItemFrameAlreadyLocked"));
                return true;
            }

        } else if ( player.hasMetadata(GakubuchiLockCommand.META_REMOVE_COMMAND) ) {

            if ( !player.hasMetadata(GakubuchiLockCommand.META_PERSIST_MODE) ) {
                player.removeMetadata(GakubuchiLockCommand.META_REMOVE_COMMAND, parent);
            }

            // ロックデータ取得
            LockData ld = lockManager.getLockDataByHanging(hanging);

            if ( ld == null ) {
                player.sendMessage(Messages.get("ItemFrameUnlocked"));
                return true;

            } else {
                // Adminではなくて、かつ、クリックした人の額縁でないなら、操作を禁止する
                if ( !player.hasPermission(PERMISSION + ".admin") &&
                        ld.getOwner() != null &&
                        !ld.getOwner().getName().equals(player.getName()) ) {
                    player.sendMessage(Messages.get("ItemFrameNotOwner"));
                    return true;
                }

                // 新しいロックデータを登録する
                lockManager.removeLockData(hanging);
                player.sendMessage(Messages.get("LockRemoved"));
                return true;

            }
        }

        return false;
    }

    /**
     * ピストンが伸びたときのイベント
     * @param event
     */
    @EventHandler
    public void onBlockPistonExtended(BlockPistonExtendEvent event) {

        // ピストンが伸びた先に額縁があるなら、イベントをキャンセルする。
        Location extLoc = event.getBlock().getRelative(event.getDirection()).getLocation();
        if (  GakubuchiUtility.getFrameFromLocation(extLoc) != null ) {
            event.setCancelled(true);
            return;
        }
        for ( Block block : event.getBlocks() ) {

            // これから動くブロックに、ロックされた額縁が貼りついているなら、イベントをキャンセルする。
            for ( ItemFrame frame : GakubuchiUtility.getAttachedFrameOnBlock(block) ) {
                if ( lockManager.getLockDataByHanging(frame) != null ) {
                    event.setCancelled(true);
                    return;
                }
            }

            // これから動くブロックの移動先に、ロックされた額縁が貼りついているなら、イベントをキャンセルする。
            Location location = block.getRelative(event.getDirection()).getLocation();
            ItemFrame frame = GakubuchiUtility.getFrameFromLocation(location);
            if ( frame != null && lockManager.getLockDataByHanging(frame) != null ) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * ピストンが縮んだときのイベント
     * @param event
     */
    @EventHandler
    public void onBlockPistonRetract(BlockPistonRetractEvent event) {

        // 動いたブロックのところに額縁があるなら、イベントをキャンセルする。
        Block block = event.getBlock().getRelative(event.getDirection());
        for ( ItemFrame frame : GakubuchiUtility.getAttachedFrameOnBlock(block) ) {
            if ( lockManager.getLockDataByHanging(frame) != null ) {
                event.setCancelled(true);
                return;
            }
        }
    }
}
