/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2016
 */
package org.bitbucket.ucchy.glr;

import java.util.ArrayList;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * プラグイン起動から一定時間後に、全ワールドの額縁を調べて、
 * 既に額縁が存在しなくなっているロックデータをクリーンアップするタスク。
 * @author ucchy
 */
public class LockDataCleanupTask extends BukkitRunnable {

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        // 全ての額縁の位置を取得する
        ArrayList<String> locs = new ArrayList<String>();

        for ( World world : Bukkit.getWorlds() ) {
            for ( ItemFrame frame : world.getEntitiesByClass(ItemFrame.class) ) {
                locs.add(getDescriptionFromLocation(frame.getLocation()));
            }
        }

        // 全てのロックデータを確認し、該当位置に額縁が既に無い場合は、ロックデータを削除する。
        LockDataManager manager = GakubuchiLockReloaded.getInstance().getLockDataManager();
        boolean isEnableLogging = GakubuchiLockReloaded.getInstance().getGLConfig().isCleanupTaskLog();
        Logger logger = GakubuchiLockReloaded.getInstance().getLogger();
        for ( LockData data : manager.getAllLockData() ) {

            String loc = getDescriptionFromLocation(data.getLocation());
            if ( !locs.contains(loc) ) {
                // 該当位置に額縁が既に無いので、ロックデータを削除する。
                manager.removeLockData(loc);
                if ( isEnableLogging ) {
                    logger.info("LockDataManager cleanup " + loc + ".");
                }
            }
        }
    }

    /**
     * Locationを文字列に変換する
     * @param location Location
     * @return 変換後の文字列
     */
    private static String getDescriptionFromLocation(Location location) {

        return String.format("%s_%d_%d_%d",
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ() );
    }
}
