/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2014
 */
package org.bitbucket.ucchy.glr;

import java.util.ArrayList;

import org.bukkit.inventory.ItemStack;

/**
 * 仕方なく消滅してしまった額縁を所有者に返すための、補償データ
 * @author ucchy
 */
public class CompensationData {

    ArrayList<ItemStack> items;

    /**
     * コンストラクタ
     */
    public CompensationData() {
        items = new ArrayList<ItemStack>();
    }

    /**
     * @return アイテム情報を返す
     */
    public ArrayList<ItemStack> getItems() {
        return items;
    }

    /**
     * アイテムを追加する
     * @param item アイテム
     */
    public void addItem(ItemStack item) {
        items.add(item);
    }
}
