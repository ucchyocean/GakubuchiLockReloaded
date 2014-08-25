/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

/**
 * GakubuchiLockのリスナークラス
 * @author ucchy
 */
public class GakubuchiPlayerListener implements Listener {

    private LockDataManager lockManager;
    private CompensationDataManager compManager;

    /**
     * コンストラクタ
     * @param parent
     */
    public GakubuchiPlayerListener(GakubuchiLockReloaded parent) {
        this.lockManager = parent.getLockDataManager();
        this.compManager = parent.getCompensationDataManager();
    }

    /**
     * かべかけ物が設置された時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHangingPlace(HangingPlaceEvent event) {

        Hanging hanging = event.getEntity();

        // 権限がなければ、操作を禁止する
        if ( !event.getPlayer().hasPermission("gakubuchilock.place") ) {
            event.getPlayer().sendMessage(ChatColor.RED + "You don't have a permission!!");
            event.setCancelled(true);
            return;
        }

        // 同じLocationを持つ既存のItemFrameまたはPaintingが存在しないか、確認する。
        // 本プラグイン導入後は、同じ位置へのHangingの設置を認めない。
        Location location = hanging.getLocation();
        if ( GakubuchiUtility.getHangingFromLocation(location) != null ) {
            event.setCancelled(true);
            return;
        }

        // 新しいロックデータを登録する
        lockManager.addLockData(event.getPlayer().getUniqueId(), hanging);

        String msg = String.format(
                ChatColor.DARK_GREEN + "Create a Private %s successfully.",
                event.getEntity().getType().name());
        event.getPlayer().sendMessage(msg);
    }

    /**
     * かべかけ物が破壊された時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHangingBreak(HangingBreakEvent event) {

        // エンティティによる破壊なら、HangingBreakByEntityEventで処理するので、
        // ここでは何もしない。
        if ( event instanceof HangingBreakByEntityEvent ) {
            return;
        }

        Hanging hanging = event.getEntity();

        // 対象物のロックデータを取得する
        LockData ld = lockManager.getLockDataByHanging(hanging);

        // ロックデータが無い場合
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
            // fall through
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

            // OBSTRUCTION、PHYSICS、DEFAULTは、ここでまとめて処理する。

            // 所有者がオンラインなら、メッセージを流す
            if ( ld.getOwner().isOnline() ) {
                String msg = String.format(
                        ChatColor.RED + "Your %s(%s,%d,%d,%d) was broken!",
                        hanging.getType().name(),
                        hanging.getLocation().getWorld().getName(),
                        hanging.getLocation().getBlockX(),
                        hanging.getLocation().getBlockY(),
                        hanging.getLocation().getBlockZ() );
                ld.getOwner().getPlayer().sendMessage(msg);
            }

            // 所有者にアイテムを補填する
            if ( hanging instanceof Painting ) {
                compManager.addItem(ld.getOwnerUuid(), new ItemStack(Material.PAINTING));
            } else if ( hanging instanceof ItemFrame ) {
                ItemFrame frame = (ItemFrame)hanging;
                compManager.addItem(ld.getOwnerUuid(), new ItemStack(Material.ITEM_FRAME));
                compManager.addItem(ld.getOwnerUuid(), frame.getItem());
            }

            // イベントはキャンセルしつつ、エンティティを削除して、
            // 何もドロップしないようにする。
            event.setCancelled(true);
            hanging.remove();

            // ロックデータを削除する
            lockManager.removeLockData(hanging);
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

        // 対象物のロックデータを取得する
        LockData ld = lockManager.getLockDataByHanging(hanging);

        // ロックデータが無い場合
        if ( ld == null ) {

            // 権限がなければ、操作を禁止する
            if ( event.getRemover() instanceof Player ) {
                Player remover = (Player)event.getRemover();
                if ( !remover.hasPermission("gakubuchilock.break") ) {
                    remover.sendMessage(ChatColor.RED + "You don't have a permission!!");
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
        if ( remover == null || !ld.getOwnerUuid().equals(remover.getUniqueId()) ||
                !remover.hasPermission("gakubuchilock.admin") ) {
            event.setCancelled(true);
            if ( remover != null ) {
                String msg = String.format(
                        ChatColor.RED + "This %s is locked with a magical spell.",
                        hanging.getType().name());
                remover.sendMessage(msg);
            }
            return;
        }

        // メッセージを出す
        String msg = String.format(
                ChatColor.RED + "%s unregistered.",
                hanging.getType().name());
        remover.sendMessage(msg);

        // ロック情報を削除する
        lockManager.removeLockData(hanging);
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

        // MEMO: このイベントハンドラは、
        //   額縁の中のアイテムを右クリックされた時に呼び出されるので、
        //   所有者を確認して、所有者でなければメッセージを表示して操作をキャンセルする。

        // ロックデータ取得
        Hanging hanging = (Hanging)event.getRightClicked();
        LockData ld = lockManager.getLockDataByHanging(hanging);

        // 所有者でなくて、管理者でもなければ、操作を禁止する
        if ( ld != null && !ld.getOwnerUuid().equals(event.getPlayer().getUniqueId()) ||
                !event.getPlayer().hasPermission("gakubuchilock.admin") ) {
            event.setCancelled(true);
            String msg = String.format(
                    ChatColor.RED + "This %s is locked with a magical spell.",
                    hanging.getType().name());
            event.getPlayer().sendMessage(msg);
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

        // MEMO: このイベントハンドラは、
        //   額縁の中のアイテムを左クリックされた時、または、
        //   飛来物（矢や雪球など）による攻撃を受けた時に呼び出されるので、
        //   所有者を確認して、所有者でなければメッセージを表示して操作をキャンセルする。

        // ロックデータ取得
        Hanging hanging = (Hanging)event.getEntity();
        LockData ld = lockManager.getLockDataByHanging(hanging);

        // ロックデータが無いなら何もしない
        if ( ld == null ) {
            return;
        }

        // 攻撃者取得
        Player damager = null;
        if ( event.getDamager() instanceof Player ) {
            damager = (Player)event.getDamager();
        } else if ( event.getDamager() instanceof Projectile ) {
            if ( ((Projectile)event.getDamager()).getShooter() instanceof Player ) {
                damager = (Player)((Projectile)event.getDamager()).getShooter();
            }
        }

        // 所有者でなくて、管理者でもなければ、操作を禁止する
        if ( damager == null || !ld.getOwnerUuid().equals(damager.getUniqueId()) ||
                !damager.hasPermission("gakubuchilock.admin") ) {
            event.setCancelled(true);
            if ( damager != null ) {
                String msg = String.format(
                        ChatColor.RED + "This %s is locked with a magical spell.",
                        hanging.getType().name());
                damager.sendMessage(msg);
            }
            return;
        }
    }

    /**
     * ブロックが置かれたときに呼び出されるイベント
     * @param event
     */
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {

        Location location = event.getBlockPlaced().getLocation();

        // 指定された場所にHangingがないか確認する
        Hanging hanging = GakubuchiUtility.getHangingFromLocation(location);
        if ( hanging != null ) {

            // ロックデータ取得
            LockData ld = lockManager.getLockDataByHanging(hanging);

            // ロックデータが無いなら何もしない
            if ( ld == null ) {
                return;
            }

            // 設置者取得、エンダーマンならnullになる
            Player player = event.getPlayer();

            // 所有者でなければ、ブロック設置を禁止する（管理権限があってもNGとする）。
            if ( player == null || !ld.getOwnerUuid().equals(player.getUniqueId()) ) {
                event.setBuild(false);
                event.setCancelled(true);
                if ( player != null ) {
                    String msg = String.format(
                            ChatColor.RED + "This %s is locked with a magical spell.",
                            hanging.getType().name());
                    player.sendMessage(msg);
                }
                return;
            }
        }
    }
}
