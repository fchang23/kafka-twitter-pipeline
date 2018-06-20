`this is the code part`

&nbsp;&nbsp;&nbsp;&nbsp;It's very easy to make some words **bold** and other words *italic* with Markdown. You can even [link to Google!](http://google.com)Sometimes you want numbered lists:


# What dbus is going to do?
>dbus主要用于数据收集、数据源同步、实时流数据计算。可通过web管理界面进行简单灵活配置，以无侵入的方式对源端数据进行采集，采用高可用的流式计算框架（Storm），对公司各个IT系统在业务流程中产生的数据进行汇聚，经过转换处理后成为统一JSON的数据格式（UMS），提供给不同数据使用方订阅和消费，充当数仓平台、大数据分析平台、实时报表和实时营销等业务的数据源。

![system-architecture](https://bridata.github.io/DBus/img/more-system-architecture.png)

- 日志抓取模块：从RDBMS的slave库中读取增量日志，并实时同步到Kafka中；
- 增量转换模块：将增量数据实时转换为UMS数据，处理schema变更，脱敏等；
- 全量抽取程序：将全量数据从RDBMS备库拉取并转换为UMS数据；
- 日志算子处理模块：将来自不同抓取端的日志数据按照算子规则进行结构化处理；
- 心跳监控模块：对于RDBMS类源，定时向源端发送心跳数据，并在末端进行监控，发送预警通知；对于日志类，直接在末端监控预警。
- web管理模块：管理所有相关模块。DBusMgr为web服务的数据库。

## 功能组件

>dbus可应用于两类数据源，RDBMS类数据源和log类数据源。日志抽取、增量转换和全量拉取模块应用于RDBMS类数据源；各类log处理框架和算子处理模块应用于log类数据源。

### RDBMS数据源

基本流程：利用Canal获取MySQL binlog，用storm及时转存到Kafka topic中（protobuf format），之后再用storm根据不同的数据库或表解析成UMS格式的数据，供其他模块消费。UMS（Universial Message Schema）就是一个json的数据格式，自身带schema（自描述）。在初始同步时，支持将数据从MySQL中全量拉取，之后再进行增量同步。

#### 日志抽取模块（log extractor）

主要由canal和storm构成。canal用于抽取关系型数据库slave库的binlog，storm从canal server中读取数据，迅速将其publish到一个Kafka topic中存储。对binlog格式进行转换为protobuf格式。

#### 增量转换模块（dispatcher-appender）

![dispatcher-appender](https://bridata.github.io/DBus/img/more-system-architecture-1-2.png)

分发（dispatcher）

- 将来自数据源的日志按照不同的schema分发到不同topic上
  - 为了数据隔离（一般不同的shema对应不同的数据库）
  - 为了分离转换模块的计算压力，因为转换模块计算量比较大，可以部署多个（topology），每个schema一个提高效率。

转换（appender）

- 实时数据格式转换：Canal数据是protobuf格式，需要转换为我们约定的UMS格式，生成唯一标识符ums_id和ums_ts等；
- 捕获元数据版本变更：比如表加减列，字段变更等，维护版本信息，发出通知触发告警
- 实时数据脱敏：根据需要对指定列进行脱敏，例如替换为***，MD5加盐等。
- 响应拉全量事件：当收到拉全量请求时为了保证数据的相应顺序行，会暂停拉增量数据，等全量数据完成后，再继续。

#### 全量拉取模块（full-puller）



### log类数据源

#### 算子处理模块（log processor）



### web配置管理与zookeeper

web管理界面实现了可视化的定制服务。在web界面中，
- 可以添加新的data source；
- 可以为已存在的数据源添加新的schema或table，添加新的schema或table时，可以配置新的schema或table的schema属类和源topic和目标topic；
- 可以配置table内的数据进行脱敏（encode）；
- 可以启动或停止某个schema或table的增量转换，可以选择对某个table启动全量拉取；
- 可以对log类数据源进行定制处理规则；
- 可以发送控制信息用于重启dbus系统中的某个模块；
- 可以对zookeeper进行可视化管理，查看更新的数据，修改并更新数据；




# All in one 安装指南

Linux环境机器为centos7或Red Hat Enterprise Linux 7

## 安装MySQL

1. 下载mysql-5.7.19-1.el6.x86_64.rpm-bundle.tar [下载地址](https://dev.mysql.com/downloads/mysql/)

2. 解压后，按顺序执行下列命令
    ```sh
    rpm -ivh mysql-community-common-5.7.19-1.el6.x86_64.rpm --nodeps
    rpm -ivh mysql-community-libs-5.7.19-1.el6.x86_64.rpm --nodeps
    rpm -ivh mysql-community-client-5.7.19-1.el6.x86_64.rpm --nodeps
    rpm -ivh mysql-community-server-5.7.19-1.el6.x86_64.rpm --nodeps
    rpm -ivh mysql-community-devel-5.7.19-1.el6.x86_64.rpm --nodeps
    ```
    >在执行第一条命令之后，会出现与**mariadb-libs-1:5.5.44-2.el7.centos.x86_64**冲突的错误，此时，执行yum -y remove mariadb-libs-1:5.5.44-2.el7.centos.x86_64即可。

3. 在/etc/my.cnf添加canal相关配置
    ```sh
    log-bin=mysql-bin
    binlog-format=ROW
    server_id=1
    ```

4. 启动mysql
    ```sh
    service mysqld start
    ```
## 安装influxDB
    
1. 下载influxDB：[influxdb-1.1.0.x86_64](https://portal.influxdata.com/downloads)
2. 切换到root用户，在其目录安装。
    ```sh
    rpm -ivh influxdb-1.1.0.x86_64.rpm
    ```
3. 启动influxDB
    ```sh
    service influxdb start
    ```
4. influxDB初始化配置
    ```sh
    #登录influx
    influx

    #执行初始化脚本
    create database dbus_stat_db
    use dbus_stat_db
    CREATE USER "dbus" WITH PASSWORD 'dbus!@#123'
    ALTER RETENTION POLICY autogen ON dbus_stat_db DURATION 15d
    ```

## 安装dbus-allinone包
    
1. [下载dbus-allinone包](https://github.com/BriData/DBus/releases)

2. 在根目录在创建/app路径，将tar包解压在里面。
    ```sh
    mkdir /app
    cd /app
    tar zxvf dbus-allinone.tar.gz
    ```

3. 初始化数据库
    ```shell
    mysql -u root -p
    #第一个sql文件里面有中文乱码，需要删掉
    source /app/dbus-allinone/sql/1_init_database_user.sql
    source /app/dbus-allinone/sql/2_dbusmgr_tables.sql
    source /app/dbus-allinone/sql/3_dbus.sql
    source /app/dbus-allinone/sql/4_test.sql
    source /app/dbus-allinone/sql/5_canal.sql
    ```

4. 启动dbus服务
    ```shell
    cd /app/dbus-allinone
    ./start.sh
    ```

## 配置Grafana

1. 登陆至http://linux_host_address:3000/login

    ![image4.1](https://bridata.github.io/DBus/img/quick-start-5-1.png)

2. 修改Grafana数据源

    ![iamge4.2.1](https://bridata.github.io/DBus/img/quick-start-5-2.png)
    ![iamge4.2.2](https://bridata.github.io/DBus/img/quick-start-5-3.png)
    ![iamge4.2.3](https://bridata.github.io/DBus/img/quick-start-5-4.png)

## 验证RDBMS数据源抽取同步是否工作

1. 插入验证数据
    ```shell
    #登录测试用户
    mysql -utestschema -p     #testschema账户密码参考/app/dbus-allinone/sql/4_test.sql
    #执行测试脚本
    use testschema;
    INSERT INTO test_table (NAME, BIRTHDAY) VALUES ('ASFASFASF', '2018-01-25 16:11:20');
    INSERT INTO test_table (NAME, BIRTHDAY) VALUES ('QWEQWEQWE', '2018-01-25 16:11:20');
    INSERT INTO test_table (NAME, BIRTHDAY) VALUES ('QWEQWEQWE', '2018-01-25 16:11:20');
    INSERT INTO test_table (NAME, BIRTHDAY) VALUES ('QWEQWEQWE', '2018-01-25 16:11:20');
    INSERT INTO test_table (NAME, BIRTHDAY) VALUES ('QWEQWEQWE', '2018-01-25 16:11:20');
    ```

2. 查看DBus是否实时获取到数据

    ![iamge5.2.1](https://bridata.github.io/DBus/img/quick-start-6-2.png)
    ![iamge5.2.2](https://bridata.github.io/DBus/img/quick-start-6-3.png)
    ![iamge5.2.3](https://bridata.github.io/DBus/img/quick-start-6-4.png)



     








