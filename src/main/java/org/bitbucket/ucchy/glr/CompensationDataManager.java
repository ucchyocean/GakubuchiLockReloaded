/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

/**
 * 仕方なく消滅してしまった額縁を所有者に返すための、補償データの管理クラス
 * @author ucchy
 */
public class CompensationDataManager {

    /** フラットデータファイルを格納するフォルダ */
    private File dataFolder;

    /** 補償データの、プレイヤーUUIDをキーとしたマップ */
    private HashMap<UUID, CompensationData> idMap;

    /**
     * コンストラクタ
     * @param dataFolder フラットデータファイルを格納するフォルダ
     */
    public CompensationDataManager(File dataFolder) {

        this.dataFolder = dataFolder;

        // データフォルダがまだ存在しないなら、ここで作成する
        if ( !dataFolder.exists() ) {
            dataFolder.mkdirs();
        }

        // データのロード
        reloadData();
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
        idMap = new HashMap<UUID, CompensationData>();

        for ( File file : files ) {

            // 後ろの4文字を削って拡張子を抜く
            String key = file.getName().substring(0, file.getName().length() - 4);

            // UUIDへ変換する
            if ( !isUUID(key) ) {
                continue;
            }
            UUID uuid = UUID.fromString(key);

            // データをロードする
            idMap.put(uuid, loadCompensationData(file));
        }
    }

    /**
     * 補償データをファイルからロードする
     * @param file 対象ファイル
     * @return ロードされたデータ
     */
    private CompensationData loadCompensationData(File file) {

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        CompensationData data = new CompensationData();

        for ( String key : config.getKeys(false) ) {
            ItemStack item = config.getItemStack(key);
            data.addItem(item);
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

        CompensationData data = idMap.get(uuid);
        int counter = 0;
        for ( ItemStack item : data.getItems() ) {
            String key = "item" + counter;
            config.set(key, item);
            counter++;
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 文字列がUUIDかどうかを判定する
     * @param source 文字列
     * @return UUIDかどうか
     */
    private static boolean isUUID(String source) {
        return source.matches("[0-9A-F]{8}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{4}-[0-9A-F]{12}");
    }
}
