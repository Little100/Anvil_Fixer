# 创建一个新物品以修复铁砧(english down below)
- 前往config.yml中找到 Item: 部分
- 在 Item: 最下方添加
  - 请注意 物品可以随便设置 但是id必须是一个正确的命名id
  - 以下以金锭作为例子
  - 在config.yml中:
    ```yaml
      Item:
       # 原有或者其他物品
       Gold_Ingot(也可以是其他名称):
        id: minecraft:gold_ingot(必须是正确的命名id)
        enable: true
        chance: 0.5
        tips:
          name: "金锭"
        Fix_level:
          Max:
            level: 2
            chance: 0.5
          Min:
            level: 1
            chance: 0.5
    ```
- 意思是创建一个名为Gold_Ingot可以作为修复的物品
- 物品的id是minecraft:gold_ingot
- 物品为开启状态
- 物品的修复几率为0.5
  - 二次修复一级的几率为0.5
  - 二次修复二级的几率为0.5
- 物品的名字为金锭
- 物品的修复等级为1-2级

# create a new item to fix the iron pickaxe
- go to the config.yml and find the Item: section
- add the following code below the Item: section
  - please note that the item can be set to whatever you want but the id must be a valid naming id
  - for example, let's use gold ingot as an example
  - in the config.yml:
    ```yaml
      Item:
       # existing or other items
       Gold_Ingot(can be any name):
        id: minecraft:gold_ingot(must be a valid naming id)
        enable: true
        chance: 0.5
        tips:
          name: "Gold Ingot"
        Fix_level:
          Max:
            level: 2
            chance: 0.5
          Min:
            level: 1
            chance: 0.5
    ```
- the above code creates a new item named Gold_Ingot that can be used to fix the iron pickaxe
- the item's id is minecraft:gold_ingot
- the item is enabled
- the item's fix chance is 0.5
  - the chance of a second fix at level 2 is 0.5
  - the chance of a second fix at level 1 is 0.5
- the item's name is Gold Ingot
- the item's fix level is 1-2