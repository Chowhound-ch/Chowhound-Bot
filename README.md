# Chowhound-Bot
基于Mirai的qq机器人

## 介绍
### 模块
1. [Chowhound-Bot-Parent]() 父模块，包含所有子模块的依赖
2. [Chowhound-Framework](Chowhound-Framework)Chowhound-Bot 公共模块，包含所有子模块的公共类和方法
3. [Chowhound-Bot](Chowhound-Bot) Chowhound-Bot 主模块，`启动类、配置文件`在这里
4. 数据模块`(任选其一)`, 在
    - 3.1 `默认`[Chowhound-Data-Json](Chowhound-Data-Json) Chowhound-Bot 基于`Json`数据模块，对数据进行操作，数据存储在本地
    - 3.2 [Chowhound-Data-Mongo](Chowhound-Data-Mongo) Chowhound-Bot 基于`Mongo`数据模块，对数据进行操作
    - 3.3 [Chowhound-Data-MySql](Chowhound-Data-MySql) Chowhound-Bot 基于`MySql`数据模块，对数据进行操作