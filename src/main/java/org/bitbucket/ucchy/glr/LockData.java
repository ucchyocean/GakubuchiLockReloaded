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
import org.bukkit.entity.Hanging;

/**
 * ロックデータ
 * @author ucchy
 */
public class LockData {

    /** ロック所有者のUUID */
    private UUID uuid;

    /** ロック対象のHanging */
    private Hanging hanging;

    /**
     * コンストラクタ
     * @param uuid ロック所有者のUUID
     * @param hanging ロック対象のHanging
     */
    public LockData(UUID uuid, Hanging hanging) {
        this.uuid = uuid;
        this.hanging = hanging;
    }

    /**
     * @return uuid
     */
    public UUID getOwnerUuid() {
        return uuid;
    }

    /**
     * @return hanging
     */
    public Hanging getHanging() {
        return hanging;
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
        return hanging.getLocation();
    }
}
