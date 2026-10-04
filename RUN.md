# 运行与构建

公开源码是受四条约束改进后的应用 V2。后端产品代码与 SQL 迁移保持现有实现，未为了公开发布改写业务。Maven artifact 的历史名称 `0.0.1-V1` 仍保留，不能据此混淆应用比较版本。

## 环境

- JDK 21，Maven Wrapper 自动取得 Maven 3.9.11；构建规则限制 JDK 21、Maven 3.9。
- MySQL 8（原课程实际检查使用本机 MySQL 8）。需要支持 Flyway 迁移、外键和 `GET_LOCK`。
- Node 24.21.0、npm 11.19.0，以 `frontend/package.json` 的 engines 为准。
- 可选：TeX Live 的 pdfLaTeX、ctex 和 Arphic 字体，用于重新编译报告。

## 1. 准备自己的数据库

建立空数据库 `rms`，使用 `utf8mb4`，为自己的应用账号授予该库迁移和读写权限。密码由运行者在本机设置，本仓库没有预置真实账号密码。Flyway 从 `backend/src/main/resources/db/migration/` 建表，Hibernate 随后校验模式；不要手工把开发数据库或实验数据复制到仓库。

复制根目录 `.env.example` 为本地 `.env`，填入数据库账号密码和首次管理员密码。首次管理员密码必须满足 `PasswordInputPolicy`，其具体字节域见后端源码；空值不会初始化。示例账号名和 `example.invalid` 邮箱只是占位值。

配置包括：

| 变量 | 作用 |
| --- | --- |
| RMS_DB_URL | MySQL JDBC URL，保留 UTC 时间处理 |
| RMS_DB_USERNAME / RMS_DB_PASSWORD | 自己的数据库凭据 |
| RMS_BOOTSTRAP_USERNAME / EMAIL / DISPLAY_NAME / PASSWORD | 空库首次创建管理员 |
| SERVER_ADDRESS / SERVER_PORT | 本地使用 127.0.0.1:18080 |
| SERVER_SERVLET_SESSION_COOKIE_SECURE | 本地 HTTP 用 false；HTTPS 部署用 true |

Spring Boot 读取**进程环境变量**，不会自动加载根 `.env`。Linux/macOS 可在根目录执行 `set -a; . ./.env; set +a` 后从同一终端启动后端。Windows PowerShell 可以逐项设置 `$env:RMS_DB_URL` 等变量，或用本地脚本读取 `.env`；不要把带密码的命令记录提交到仓库。

首次空库启动自动创建五个角色和管理员。非空库不会反复覆盖账号；其启用管理员和角色目录需要有效。后续用户及角色从管理员页面创建。首次初始化并不是开放注册或密码重置接口。

## 2. 启动后端

在设置好环境变量的终端：

```sh
cd backend
./mvnw clean package -DskipTests
java -jar target/rms-backend-0.0.1-V1.jar
```

Windows 使用 `mvnw.cmd clean package -DskipTests`。`-DskipTests` 仅用于先构建运行包，不表示测试已通过。后端路径统一为 `/api/v1`。

如果 Windows 的用户临时目录包含中文，而 JDK 的本地 socket 临时文件初始化失败，可为本次终端指定自己创建的 ASCII 临时目录，并在 JVM 参数中设置 `java.io.tmpdir` 与 `jdk.net.unixdomain.tmpdir`。这只调整运行环境，不改变业务配置。

## 3. 启动前端

另一个终端进入 `frontend`：

```sh
npm ci
npm run dev
```

打开 Vite 显示的地址（通常为 http://127.0.0.1:5173），用自己配置的首次管理员登录。Vite 的 `/api` 代理指向 http://127.0.0.1:18080；更换后端端口时需要相应配置开发代理。页面动作使用会话与 CSRF，不需要在浏览器保存 Bearer token。

## 4. 检查

前端：

```sh
npm ci
npm run build
npm run test -- --run
```

后端完整开发检查需要**可丢弃的独立 MySQL 测试库**及上述环境变量，然后运行 `./mvnw clean test`。初始化探针另读取 `RMS_BOOTSTRAP_PROBE_DB_URL`，也应是独立空测试库。部分测试创建用户和需求、模拟故障，不能用正在使用的工作数据库作为测试库。源文件中的硬编码字符串只属于本地合成测试数据，不是公开演示密码。

原课程 V1/V2 32 项比较包含 HTTP、MySQL、并发、故障和浏览器旅程。它不是一次 `mvn test` 的别名；当前公开源码为 V2，原私人运行证据不随公开树发布。[测试摘要](docs/experiment/test-summary.md)区分开发检查、代表场景和未运行范围。

## 5. 重新编译报告

见 [report/BUILD.md](report/BUILD.md)。正式编译使用 pdfLaTeX，不依赖 XeLaTeX。报告只有一张完整 ER 主图；A3 横向页保留原始矢量尺寸，其余正文为 A4。
