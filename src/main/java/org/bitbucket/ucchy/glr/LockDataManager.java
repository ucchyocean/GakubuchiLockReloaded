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
import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Hanging;

/**
 * ロックデータマネージャ
 * @author ucchy
 */
public class LockDataManager {

    /** フラットデータファイルを格納するフォルダ */
    private File dataFolder;

    /** ロックデータの、プレイヤーUUIDをキーとしたマップ */
    private HashMap<UUID, ArrayList<LockData>> idMap;

    /** ロックデータの、HangingのUUIDをキーとしたマップ */
    private HashMap<UUID, LockData> hangingMap;

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

        // 全ワールドに存在する全てのHangingを取得
        HashMap<String, Collection<Hanging>> hangings =
                new HashMap<String, Collection<Hanging>>();
        for ( World world : Bukkit.getWorlds() ) {
            hangings.put(world.getName(), world.getEntitiesByClass(Hanging.class));
        }

        // 全てのデータをロード
        idMap = new HashMap<UUID, ArrayList<LockData>>();
        hangingMap = new HashMap<UUID, LockData>();

        for ( File file : files ) {

            // 後ろの4文字を削って拡張子を抜く
            String key = file.getName().substring(0, file.getName().length() - 4);

            // UUIDへ変換する
            if ( !isUUID(key) ) {
                continue;
            }
            UUID uuid = UUID.fromString(key);

            // データをロードする
            idMap.put(uuid, loadLockData(file, uuid, hangings));

            // Hangingマップにも展開する
            for ( LockData ld : idMap.get(uuid) ) {
                hangingMap.put(ld.getHanging().getUniqueId(), ld);
            }
        }
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

        ArrayList<LockData> datas = idMap.get(uuid);
        for ( LockData data : datas ) {
            String desc = getDescriptionFromLocation(data.getLocation());
            config.createSection(desc);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定されたHangingから、ロックデータを取得する
     * @param hanging Hanging
     * @return ロックデータ
     */
    public LockData getLockDataByHanging(Hanging hanging) {
        return hangingMap.get(hanging.getUniqueId());
    }

    /**
     * ロックデータを追加する
     * @param uuid オーナープレイヤー
     * @param hanging Hanging
     */
    public void addLockData(UUID uuid, Hanging hanging) {

        // 既にロックデータが存在する場合は、古いデータを削除する
        if ( hangingMap.containsKey(hanging.getUniqueId()) ) {
            removeLockData(hanging);
        }

        // オーナープレイヤーのデータが無いなら新規作成する
        if ( !idMap.containsKey(uuid) ) {
            idMap.put(uuid, new ArrayList<LockData>());
        }

        // ロックデータ追加
        LockData data = new LockData(uuid, hanging);
        idMap.get(uuid).add(data);
        hangingMap.put(hanging.getUniqueId(), data);

        // データを保存
        saveData(uuid);
    }

    /**
     * ロックデータを削除する
     * @param hanging 削除するHanging
     */
    public void removeLockData(Hanging hanging) {

        // 既にロックデータが無い場合は、何もしない
        if ( !hangingMap.containsKey(hanging.getUniqueId()) ) {
            return;
        }

        LockData ld = hangingMap.get(hanging.getUniqueId());

        // 削除を実行
        hangingMap.remove(hanging.getUniqueId());
        if ( idMap.containsKey(ld.getOwnerUuid()) ) {
            idMap.get(ld.getOwnerUuid()).remove(ld);
        }

        // データを保存
        saveData(ld.getOwnerUuid());
    }

    /**
     * プレイヤーファイルから、ロックデータをロードする
     * @param file プレイヤーファイル
     * @param uuid プレイヤーのUUID（あらかじめ取得したもの）
     * @param hangings 全ワールドのHanging（あらかじめ取得したもの）
     * @return ロードされたロックデータ
     */
    private ArrayList<LockData> loadLockData(File file, UUID uuid,
            HashMap<String, Collection<Hanging>> hangings) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ArrayList<LockData> data = new ArrayList<LockData>();

        for ( String key : config.getKeys(false) ) {

            Location location = getLocationFromDescription(key);
            if ( location == null ) {
                continue;
            }

            Hanging hanging = getHangingFromLocation(location, hangings);
            if ( hanging == null ) {
                continue;
            }

            data.add(new LockData(uuid, hanging));
        }

        return data;
    }

    /**
     * 指定された場所に存在するHangingを取得する
     * @param location 場所
     * @param hangings 全ワールドのHanging（あらかじめ取得したもの）
     * @return Hanging、指定した場所に存在しなければnull
     */
    private Hanging getHangingFromLocation(Location location,
            HashMap<String, Collection<Hanging>> hangings) {

        if ( !hangings.containsKey(location.getWorld().getName()) ) {
            return null;
        }

        for ( Hanging hanging : hangings.get(location.getWorld().getName()) ) {
            if ( location.getBlockX() == hanging.getLocation().getBlockX() &&
                    location.getBlockY() == hanging.getLocation().getBlockY() &&
                    location.getBlockZ() == hanging.getLocation().getBlockZ() ) {
                return hanging;
            }
        }
        return null;
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
        return source.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}");
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
