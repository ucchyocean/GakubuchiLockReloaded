/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

/**
 * ロックデータマネージャ
 * @author ucchy
 */
public class LockDataManager {

    /** フラットデータファイルを格納するフォルダ */
    private File dataFolder;

    /** ロックデータの、プレイヤーUUIDをキーとしたマップ */
    private HashMap<UUID, ArrayList<LockData>> idMap;

    /** ロックデータの、Locationをキーとしたマップ */
    private HashMap<String, LockData> locationMap;

    /**
     * コンストラクタ
     * @param dataFolder フラットデータファイルを格納するフォルダ
     */
    public LockDataManager(File dataFolder) {

        this.dataFolder = dataFolder;

        // データフォルダがまだ存在しないなら、ここで作成する
        if ( !dataFolder.exists() ) {
            dataFolder.mkdirs();
        }

        // データのロード
        reloadData();

        // ロードしたデータをセーブ（nullだったHangingをデータに反映するため）
        saveAllData();
    }

    /**
     * データを再読込する
     */
    public void reloadData() {

        // データフォルダに格納されているymlファイルのリストを取得
        File[] files = dataFolder.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".yml");
            }
        });

        // 全てのデータをロード
        idMap = new HashMap<UUID, ArrayList<LockData>>();
        locationMap = new HashMap<String, LockData>();

        for ( File file : files ) {

            // 後ろの4文字を削って拡張子を抜く
            String key = file.getName().substring(0, file.getName().length() - 4);

            // UUIDへ変換する
            if ( !isUUID(key) ) {
                continue;
            }
            UUID uuid = UUID.fromString(key);

            // データをロードする
            idMap.put(uuid, loadLockData(file, uuid));

            // Locationマップにも展開する
            for ( LockData ld : idMap.get(uuid) ) {
                locationMap.put(getDescriptionFromLocation(ld.getLocation()), ld);
            }
        }
    }

    /**
     * プレイヤーファイルから、ロックデータをロードする
     * @param file プレイヤーファイル
     * @param uuid プレイヤーのUUID
     * @return ロードされたロックデータ
     */
    private ArrayList<LockData> loadLockData(File file, UUID uuid) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ArrayList<LockData> data = new ArrayList<LockData>();

        for ( String key : config.getKeys(false) ) {

            if ( key.equals("name") ) continue;

            Location location = getLocationFromDescription(key);
            if ( location == null ) {
                continue;
            }

            long time = config.getLong(key, -1);

            data.add(new LockData(uuid, location, time));
        }

        return data;
    }

    /**
     * 全てのデータを保存する
     */
    public void saveAllData() {
        for ( UUID uuid : idMap.keySet() ) {
            saveData(uuid);
        }
    }

    /**
     * 指定したオーナープレイヤーのデータを保存する
     * @param uuid オーナープレイヤー
     */
    public void saveData(UUID uuid) {

        File file = new File(dataFolder, uuid.toString() + ".yml");

        YamlConfiguration config = new YamlConfiguration();

        String name;
        if ( Bukkit.getOfflinePlayer(uuid) != null ) {
            name = Bukkit.getOfflinePlayer(uuid).getName();
        } else {
            name = Messages.get("UnknownUUID");
        }
        config.set("name", name);

        ArrayList<LockData> datas = idMap.get(uuid);
        for ( LockData data : datas ) {
            String desc = getDescriptionFromLocation(data.getLocation());
            config.set(desc, data.getDate());
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定されたItemFrameから、ロックデータを取得する
     * @param frame 額縁
     * @return ロックデータ
     */
    public LockData getLockDataByFrame(ItemFrame frame) {
        if ( frame == null ) return null;
        return locationMap.get(getDescriptionFromLocation(frame.getLocation()));
    }

    /**
     * 全てのロックデータを取得する
     * @return 全てのロックデータ
     */
    public List<LockData> getAllLockData() {
        return new ArrayList<LockData>(locationMap.values());
    }

    /**
     * ロックデータを追加する
     * @param uuid オーナープレイヤー
     * @param frame 額縁
     */
    public void addLockData(UUID uuid, ItemFrame frame) {

        if ( uuid == null || frame == null ) return;

        // 既にロックデータが存在する場合は、古いデータを削除する
        String locDesc = getDescriptionFromLocation(frame.getLocation());
        if ( locationMap.containsKey(locDesc) ) {
            removeLockData(frame);
        }

        // オーナープレイヤーのデータが無いなら新規作成する
        if ( !idMap.containsKey(uuid) ) {
            idMap.put(uuid, new ArrayList<LockData>());
        }

        // ロックデータ追加
        LockData data = new LockData(uuid, frame.getLocation(), System.currentTimeMillis());
        idMap.get(uuid).add(data);
        locationMap.put(locDesc, data);

        // データを保存
        saveData(uuid);
    }

    /**
     * ロックデータを削除する
     * @param frame 削除する額縁
     */
    public void removeLockData(ItemFrame frame) {

        if ( frame == null ) return;

        // 既にロックデータが無い場合は、何もしない
        String locDesc = getDescriptionFromLocation(frame.getLocation());
        if ( !locationMap.containsKey(locDesc) ) {
            return;
        }

        LockData ld = locationMap.get(locDesc);

        // 削除を実行
        locationMap.remove(locDesc);
        if ( idMap.containsKey(ld.getOwnerUuid()) ) {
            idMap.get(ld.getOwnerUuid()).remove(ld);
        }

        // データを保存
        saveData(ld.getOwnerUuid());
    }

    /**
     * ロックデータを削除する
     * @param locationDescription 削除する額縁の位置情報文字列
     */
    public void removeLockData(String locationDescription) {

        if ( locationDescription == null ) return;

        // 既にロックデータが無い場合は、何もしない
        if ( !locationMap.containsKey(locationDescription) ) {
            return;
        }

        LockData ld = locationMap.get(locationDescription);

        // 削除を実行
        locationMap.remove(locationDescription);
        if ( idMap.containsKey(ld.getOwnerUuid()) ) {
            idMap.get(ld.getOwnerUuid()).remove(ld);
        }

        // データを保存
        saveData(ld.getOwnerUuid());
    }

    /**
     * 指定したプレイヤーのロック数を返す
     * @param uuid プレイヤー
     * @return ロック数
     */
    public int getPlayerLockNum(UUID uuid) {

        if ( idMap.containsKey(uuid) ) {
            return idMap.get(uuid).size();
        }
        return 0;
    }

    /**
     * 指定されたプレイヤーの設置上限数を返す
     * @param player プレイヤー
     * @return 設置上限数(ただし、-1は無限大を示す)
     */
    public int getPlayerStandLimit(Player player) {

        if ( player.hasPermission(GakubuchiLockReloaded.PERMISSION_INFINITE_PLACE) ) {
            return -1;
        }

        GakubuchiLockConfig config = GakubuchiLockReloaded.getInstance().getGLConfig();
        int limit = config.getItemFrameLimit();

        if ( limit <= -1 ) {
            return -1;
        }

        if ( GakubuchiLockReloaded.getInstance().getPex() != null ) {
            return GakubuchiLockReloaded.getInstance().getPex().getPlayerIntegerOptionValue(
                    player, "itemFrameLimit", limit);
        }

        return limit;
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

    /**
     * 文字列をLocationに変換する
     * @param description 文字列
     * @return Location、変換に失敗した場合はnull
     */
    private static Location getLocationFromDescription(String description) {

        String[] temp = description.split("_");
        if ( temp.length < 4 ) {
            return null;
        }

        int offset = temp.length - 4;
        String temp_x = temp[offset + 1];
        String temp_y = temp[offset + 2];
        String temp_z = temp[offset + 3];
        if ( !isDigit(temp_x) || !isDigit(temp_y) || !isDigit(temp_z) ) {
            return null;
        }
        int x = Integer.parseInt(temp_x);
        int y = Integer.parseInt(temp_y);
        int z = Integer.parseInt(temp_z);

        String suffix = temp_x + "_" + temp_y + "_" + temp_z;
        String wname = description.substring(0, description.lastIndexOf(suffix) - 1);
        World world = Bukkit.getWorld(wname);
        if ( world == null ) {
            return null;
        }

        return new Location(world, x, y, z);
    }

    /**
     * 文字列がUUIDかどうかを判定する
     * @param source 文字列
     * @return UUIDかどうか
     */
    private static boolean isUUID(String source) {
        return source.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    /**
     * 文字列が整数値に変換可能かどうかを判定する
     * @param source 変換対象の文字列
     * @return 整数に変換可能かどうか
     */
    private static boolean isDigit(String source) {
        return source.matches("^-?[0-9]{1,9}$");
    }
}
