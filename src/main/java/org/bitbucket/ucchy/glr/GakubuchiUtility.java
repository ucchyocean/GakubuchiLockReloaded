/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Painting;

/**
 * 額縁や絵画を扱うユーティリティクラス
 * @author ucchy
 */
public class GakubuchiUtility {

    /**
     * 指定された地点にあるHangingを取得する
     * @param location 地点
     * @return Hanging、無かった場合はnull
     */
    public static Hanging getHangingFromLocation(Location location) {

        Painting painting = getPaintingFromLocation(location);
        if ( painting != null ) return painting;
        ItemFrame frame = getFrameFromLocation(location);
        return frame;
    }

    /**
     * 指定された地点にあるPaintingを取得する
     * @param location 地点
     * @return Painting、無かった場合はnull
     */
    public static Painting getPaintingFromLocation(Location location) {

        // TODO: Bukkit PR BUKKIT-3868 がマージされたら、
        // Location.getNearbyEntities が実行できるようになるので、実装しなおすこと。

        World world = location.getWorld();
        for ( Painting painting : world.getEntitiesByClass(Painting.class) ) {
            if ( isSameLocation(location, painting.getLocation()) ) {
                return painting;
            }
        }
        return null;
    }

    /**
     * 指定された地点にあるItemFrameを取得する
     * @param location 地点
     * @return ItemFrame、無かった場合はnull
     */
    public static ItemFrame getFrameFromLocation(Location location) {

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
     * 指定したブロックに貼りついているItemFrameを取得する
     * @param block ブロック
     * @return 貼りついているItemFrame
     */
    public static List<ItemFrame> getAttachedFrameOnBlock(Block block) {

        List<ItemFrame> list = new ArrayList<ItemFrame>();
        if ( block == null || block.isEmpty() || block.isLiquid() ) {
            return list;
        }

        for ( BlockFace direction : new BlockFace[]{
                BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST} ) {
            ItemFrame frame = getFrameFromLocation(block.getRelative(direction).getLocation());
            if ( frame != null && frame.getFacing() == direction ) {
                list.add(frame);
            }
        }

        return list;
    }

    /**
     * 2つのLocationが同じブロックかどうかを確認する
     * @param loc1
     * @param loc2
     * @return
     */
    private static boolean isSameLocation(Location loc1, Location loc2) {
        return loc1.getBlockX() == loc2.getBlockX() &&
                loc1.getBlockY() == loc2.getBlockY() &&
                loc1.getBlockZ() == loc2.getBlockZ();
    }
}
