/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.util.ArrayList;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
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

/**
 * GakubuchiLockのリスナークラス
 * @author ucchy
 */
public class GakubuchiPlayerListener implements Listener {

    private LockDataManager manager;

    /**
     * コンストラクタ
     * @param parent
     */
    public GakubuchiPlayerListener(GakubuchiLockReloaded parent) {
        this.manager = parent.getLockDataManager();
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
            event.setCancelled(true);
            event.getPlayer().sendMessage(ChatColor.RED + "You don't have a permission!!");
            return;
        }

        // TODO 同じ箇所に、既存のItemFrameまたはPaintingが存在しないか、確認する
        // 本プラグイン導入後は、同じ位置へのHangingの設置を認めない。

        // 新しいロックデータを登録する
        manager.addLockData(event.getPlayer().getUniqueId(), hanging);

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
        LockData ld = manager.getLockDataByHanging(hanging);

        // ロックデータが無いなら何もしない
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

            // イベントはキャンセルしつつ、エンティティを削除して、
            // 何もドロップしないようにする。
            event.setCancelled(true);
            hanging.remove();

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

            // ロックデータを削除する
            manager.removeLockData(hanging);

            // TODO: 所有者にアイテムを補填する
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

        // TODO debug
        if ( hanging.getType() == EntityType.PAINTING ) {
            Painting painting = (Painting)hanging;
            System.out.println(String.format("(%d, %d, %d) - (%d, %d)",
                    painting.getLocation().getBlockX(),
                    painting.getLocation().getBlockY(),
                    painting.getLocation().getBlockZ(),
                    painting.getArt().getBlockWidth(),
                    painting.getArt().getBlockHeight()
                    ));
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
        manager.removeLockData(hanging);
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

        // 指定された場所にItemFrameがないか確認する
        ItemFrame frame = getFrameFromLocation(location);
        if ( frame != null ) {

            // ロックデータ取得
            LockData ld = manager.getLockDataByHanging(frame);

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
                            frame.getType().name());
                    player.sendMessage(msg);
                }
                return;
            }
        }

        // 周囲にPaintingがないか確認する
        for ( Painting painting : getNearbyPainting(location, 3.0) ) {

            // ロックデータ取得
            LockData ld = manager.getLockDataByHanging(painting);

            // ロックデータが無いなら何もしない
            if ( ld == null ) {
                return;
            }

            // TODO 設置位置がPaintingにかぶっているかどうか確認する
            // Paintingの設置範囲が正確に取得できないので、なかなか難しい・・・

//            // 設置者取得、エンダーマンならnullになる
//            Player player = event.getPlayer();
//
//            // 所有者でなければ、ブロック設置を禁止する（管理権限があってもNGとする）。
//            if ( player == null || !ld.getOwnerUuid().equals(player.getUniqueId()) ) {
//                event.setBuild(false);
//                event.setCancelled(true);
//                if ( player != null ) {
//                    String msg = String.format(
//                            ChatColor.RED + "This %s is locked with a magical spell.",
//                            painting.getType().name());
//                    player.sendMessage(msg);
//                }
//                return;
//            }
        }
    }

    /**
     * 指定された地点の周囲にあるPaintingを取得する
     * @param location 基点
     * @param range 基点から検索する距離
     * @return 検索されたHanging
     */
    private ArrayList<Painting> getNearbyPainting(Location location, double range) {

        // TODO: Bukkit PR BUKKIT-3868 がマージされたら、
        // Location.getNearbyEntities が実行できるようになるので、実装しなおすこと。

        // NOTE: Locationクラスのdistanceは、呼び出すたびに平方根が計算されるため、
        // 繰り返し呼び出すとコストがかかる。
        // そのため、比較対象のrangeをあらかじめ二乗しておき、distanceSquareと比較する。

        ArrayList<Painting> paintings = new ArrayList<Painting>();
        World world = location.getWorld();
        double rangeSqr = range * range;
        for ( Painting painting : world.getEntitiesByClass(Painting.class) ) {
            if ( location.distanceSquared(painting.getLocation()) < rangeSqr ) {
                paintings.add(painting);
            }
        }
        return paintings;
    }

    /**
     * 指定された地点にあるItemFrameを取得する
     * @param location 地点
     * @return ItemFrame、無かった場合はnull
     */
    private ItemFrame getFrameFromLocation(Location location) {

        // TODO: Bukkit PR BUKKIT-3868 がマージされたら、
        // Location.getNearbyEntities が実行できるようになるので、実装しなおすこと。

        World world = location.getWorld();
        for ( ItemFrame itemframe : world.getEntitiesByClass(ItemFrame.class) ) {
            if ( isSameLocation(location, itemframe.getLocation()) ) {
                return itemframe;
            }
        }
        return null;
    }

    /**
     * 2つのLocationが同じブロックかどうかを確認する
     * @param loc1
     * @param loc2
     * @return
     */
    private boolean isSameLocation(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }
}
