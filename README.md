GakubuchiLockReloaded
=====================

Gakubuchi Lock Reloaded

額縁を個人単位で保護して、中のアイテムを守るためのプラグインです。

使用方法
--------
pluginsフォルダに、jarファイルを入れてください。

コンフィグ
----------
plugins/GakubuchiLockReloaded/config.yml に生成されます。
<pre>
# GakubuchiLockReloaded v0.0.3
# @author     ucchy
# @license    LGPLv3
# @copyright  Copyright ucchy 2014

# 1人あたりのItemFrameの設置数上限です。-1で無限大に設定することができます。
itemFrameLimit: 50
</pre>

コマンド
--------
コマンドは、/gl reload 以外は、全てのプレイヤーが実行できます（/gl reload は、OPのみです）。
- `/ginfo` または `/gl info` - 額縁のロック情報を参照します。コマンドを打った後に、額縁をパンチしてください。
- `/gprivate` または `/gl private` - 額縁を新規ロックします。コマンドを打った後に、額縁をパンチしてください。
- `/gremove` または `/gl remove` - 額縁のロック情報を削除します。コマンドを打った後に、額縁をパンチしてください。
- `/glimits` または `/gl limits` - 自分の額縁ロック数と、ロック制限数を参照します。
- `/gl reload` - データ（config.ymlとロック情報）を再読み込みします。

パーミッション
--------------
特に書いていないものは、全員が保持している権限です。
- `gakubuchilock.command` - コマンドの使用権限
- `gakubuchilock.command.info` - infoコマンドの使用権限
- `gakubuchilock.command.limits` - limitsコマンドの使用権限
- `gakubuchilock.command.private` - privateコマンドの使用権限
- `gakubuchilock.command.remove` - removeコマンドの使用権限
- `gakubuchilock.command.reload` - reloadコマンドの使用権限（デフォルトでOPが保持する権限です。）
- `gakubuchilock.command.*` - 全てのコマンドの使用権限

- `gakubuchilock.entity.place` - 額縁を新規に設置できる権限
- `gakubuchilock.entity.break` - ロックされていない額縁を、剥がすことができる権限
- `gakubuchilock.entity.admin` - ロックされた額縁を、所有者でなくても操作できる権限（デフォルトでOPが保持する権限です。）
- `gakubuchilock.entity.*` - 全てのエンティティの操作権限

- `gakubuchilock.*` - GakubuchiLockの全てのパーミッション

ライセンス
----------
LGPLv3を適用します。<br/>
ソースコードを流用する場合は、流用先にもLGPLv3を適用してください。

ダウンロード
------------
https://github.com/ucchyocean/VoteLogger/blob/master/release/VoteLogger.zip?raw=true
