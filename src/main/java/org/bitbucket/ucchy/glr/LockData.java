/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

/**
 * ロックデータ
 * @author ucchy
 */
public class LockData {

    /** ロック所有者のUUID */
    private UUID uuid;

    /** ロック対象の位置 */
    private Location location;

    /** ロック日時 */
    private long time;

    /**
     * コンストラクタ
     * @param uuid ロック所有者のUUID
     * @param location ロック対象の位置
     * @param time ロック日時
     */
    public LockData(UUID uuid, Location location, long time) {
        this.uuid = uuid;
        this.location = location;
        this.time = time;
    }

    /**
     * @return uuid
     */
    public UUID getOwnerUuid() {
        return uuid;
    }

    /**
     * 所有プレイヤーを返す
     * @return 所有プレイヤー
     */
    public OfflinePlayer getOwner() {
        return Bukkit.getOfflinePlayer(uuid);
    }

    /**
     * ロック対象の場所を返す
     * @return 場所
     */
    public Location getLocation() {
        return location;
    }

    /**
     * ロック日時を返す
     * @return ロック日時
     */
    public long getDate() {
        return time;
    }

    /**
     * 文字列表現を返す。デバッグ用
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return String.format("LockData{uuid=%s,location=%s}", uuid, location);
    }
}
