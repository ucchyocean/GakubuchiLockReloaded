/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import org.bukkit.ChatColor;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

/**
 * GakubuchiLockのリスナークラス
 * @author ucchy
 */
public class GakubuchiPlayerListener implements Listener {

    private LockDataManager manager;

    /**
     * コンストラクタ
     * @param manager
     */
    public GakubuchiPlayerListener(LockDataManager manager) {
        this.manager = manager;
    }

    /**
     * かべかけ物が設置された時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onHangingPlace(HangingPlaceEvent event) {

        Hanging hanging = event.getEntity();

        // TODO: 設置して良いかどうかを権限確認する

        // 権限がなければ、操作を禁止する
//        event.setCancelled(true);
//        event.getPlayer().sendMessage(ChatColor.RED + "You don't have a permission!!");


        // 新しいロックデータを登録する
        manager.addLockData(event.getPlayer().getUniqueId(), event.getEntity());

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

        // TODO debug
        System.out.println(event.getEventName() + " -- " +
                event.getEntity() + " = " + event.getCause());

        Hanging hanging = event.getEntity();

        // 対象物のロックデータを取得する
        LockData ld = manager.getLockDataByHanging(hanging);

        switch (event.getCause()) {

        case DEFAULT:
            break;

        case ENTITY:
            // HangingBreakByEntityEventの方で処理するので、ここでは何もしない
            break;

        case EXPLOSION:
            break;

        case OBSTRUCTION:
            break;

        case PHYSICS:
            break;

        default:
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
        LockData ld = manager.getLockDataByHanging(hanging);

        // ロックデータが無いなら何もしない
        if ( ld == null ) {
            return;
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

        // 所有者でなければ、操作を禁止する
        if ( remover == null || !ld.getUuid().equals(remover.getUniqueId()) ) {
            event.setCancelled(true);
            if ( remover != null ) {
                String msg = String.format(
                        ChatColor.RED + "This %s is locked with a magical spell.",
                        hanging.getType().name());
                remover.sendMessage(msg);
            }
        }
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
        LockData ld = manager.getLockDataByHanging(hanging);

        // 所有者でなければ、操作を禁止する
        if ( ld != null && !ld.getUuid().equals(event.getPlayer().getUniqueId()) ) {
            event.setCancelled(true);
            String msg = String.format(
                    ChatColor.RED + "This %s is locked with a magical spell.",
                    hanging.getType().name());
            event.getPlayer().sendMessage(msg);
        }
    }

    /**
     * エンティティがエンティティにダメージを与えた時に呼び出されるイベント
     * @param event
     */
    @EventHandler(priority=EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {

        // クリック対象がItemFrameでなければ、イベントを無視する。
        if ( event.getEntity().getType() != EntityType.ITEM_FRAME ) {
            return;
        }

        // MEMO: このイベントハンドラは、
        //   額縁の中のアイテムを左クリックされた時、または、
        //   飛来物（矢や雪球など）による攻撃を受けた時に呼び出されるので、
        //   所有者を確認して、所有者でなければメッセージを表示して操作をキャンセルする。

        // ロックデータ取得
        Hanging hanging = (Hanging)event.getEntity();
        LockData ld = manager.getLockDataByHanging(hanging);

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

        // 所有者でなければ、操作を禁止する
        if ( damager == null || !ld.getUuid().equals(damager.getUniqueId()) ) {
            event.setCancelled(true);
            if ( damager != null ) {
                String msg = String.format(
                        ChatColor.RED + "This %s is locked with a magical spell.",
                        hanging.getType().name());
                damager.sendMessage(msg);
            }
        }
    }
}
