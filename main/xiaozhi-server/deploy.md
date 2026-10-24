# xiaozhi-server 在 Ubuntu 服务器上的部署指南（源码 + uv + systemd）

本指南面向 **Ubuntu 服务器（20.04 / 22.04 / 24.04，x86_64 或 ARM64 均可）** 的源码部署场景：
不拉官方 Docker 镜像、直接运行仓库代码、用 `uv` 管理 Python 3.10 环境，并用 `systemd` 守护进程常驻。

> 相关文档：
> - 项目根目录 `start.md` —— uv 启动法（本地 / 1panel 通用）
> - `docs/Deployment.md` —— 官方 Docker / conda 部署法
> - `docs/docker-build.md` —— 改源码后本地编译镜像
>
> 本文件是 **Ubuntu 服务器** 的「源码 + systemd 常驻」专用手册。

---

## 一、架构与端口速览

`xiaozhi-server` 启动后同时监听两个端口：

| 端口 | 协议 | 用途 | 谁连它 |
|------|------|------|--------|
| `8000` | WebSocket | 对话服务（`ws://host:8000/xiaozhi/v1/`） | ESP32 设备 |
| `8003` | HTTP | OTA 升级接口、视觉分析接口（`/mcp/vision/explain`） | ESP32 设备 / 浏览器 |

两种运行模式（二选一）：

- **A. 仅 server（轻量）**：本地 `data/.config.yaml` 直接写 LLM/TTS/ASR 密钥，无需数据库。
- **B. 全模块（对接 manager-api）**：`data/.config.yaml` 只配 `manager-api.url` + `secret`，
  模型 / 密钥统一在智控台网页配置，需要 MySQL + Redis + manager-api + manager-web 已启动。

---

## 二、前置条件

### 2.1 系统依赖（必须，非 pip 包）

与官方 `Dockerfile-server-base` 一致，核心是 `ffmpeg` + `libopus`，并建议配置中文 locale：

```bash
sudo apt update
sudo apt install -y ffmpeg libopus0 locales

# 生成中文 UTF-8 locale（日志/配置大量中文，避免编码告警）
sudo locale-gen zh_CN.UTF-8
sudo update-locale LANG=zh_CN.UTF-8
```

### 2.2 Python 3.10 + uv

无需本机预装 Python 3.10，`uv` 会在建环境时自动下载。

```bash
# 安装 uv
curl -LsSf https://astral.sh/uv/install.sh | sh
# 让当前 shell 找到 uv（新开终端可省略）
source "$HOME/.local/bin/env" 2>/dev/null || source "$HOME/.cargo/env" 2>/dev/null
uv --version
```

### 2.3 语音识别模型（默认离线 ASR：SenseVoiceSmall）

默认语音识别走本地离线 `SenseVoiceSmall`，模型文件几百 MB，需单独下载：

```bash
cd <项目目录>/main/xiaozhi-server
mkdir -p models/SenseVoiceSmall
cd models/SenseVoiceSmall
# 阿里魔搭（推荐，服务器直接 wget）
wget -O model.pt https://modelscope.cn/models/iic/SenseVoiceSmall/resolve/master/model.pt
```

> 备用：百度网盘（提取码 `qvna`）。模型必须放在
> `main/xiaozhi-server/models/SenseVoiceSmall/model.pt`，否则启动报「找不到模型」。

---

## 三、获取源码 & 建环境

```bash
# 1. 拉取代码（此处为示例 fork 仓库，请替换为你的实际仓库地址）
git clone https://github.com/LeeDebug/xiaozhi-esp32-server
cd xiaozhi-esp32-server/main/xiaozhi-server

# 2. 用 uv 创建 Python 3.10 虚拟环境（自动下载 3.10）
uv venv --python 3.10

# 3. 安装依赖（国内镜像加速）
uv pip install -r requirements.txt -i https://mirrors.aliyun.com/pypi/simple/
```

> 依赖含 `torch==2.2.2` 等大包，安装较慢（5~15 分钟），属正常现象。
> ARM64 服务器同样适用，uv 会拉取对应架构的 wheel。

---

## 四、配置文件 `data/.config.yaml`

程序**强制要求** `data/.config.yaml` 存在，否则启动直接报错。
加载逻辑（`config/config_loader.py`）：

- 若 `data/.config.yaml` 中写了 `manager-api.url` → 走 **模式 B**，配置从 manager-api 拉取；
- 否则 → 把根目录 `config.yaml` 与 `data/.config.yaml` 合并（**模式 A**，`data/.config.yaml` 优先级更高）。

### 4.1 模式 A：仅 server（最简）

把根目录 `config.yaml` 复制为 `data/.config.yaml`，再改关键信息：

```bash
cp config.yaml data/.config.yaml
```

最小可运行示例（编辑 `data/.config.yaml`）：

```yaml
server:
  ip: 0.0.0.0          # 监听所有网卡（服务器必须）
  port: 8000
  http_port: 8003
  websocket: ws://你的服务器IP或域名:8000/xiaozhi/v1/
prompt: |
  我是一个叫小智的台湾女孩，说话机车，声音好听……
selected_module:
  LLM: DoubaoLLM        # 选你用的 LLM 模块
  TTS: DoubaoTTS        # 选你用的 TTS 模块
LLM:
  DoubaoLLM:
    api_key: 你的密钥
TTS:
  DoubaoTTS:
    api_key: 你的密钥
```

> 提示：`config.yaml` 里已内置几十种 LLM/TTS/ASR 的示例配置，
> 直接保留你需要的那一项、删掉/留空其它的即可。默认 `LLM` 是 `ChatGLMLLM`，需去智谱注册免费 key。

### 4.2 模式 B：全模块（对接 manager-api）

前提：MySQL / Redis / manager-api(8002) / manager-web 已在运行（参考项目 manager 部分部署）。

```bash
cp config_from_api.yaml data/.config.yaml
```

编辑 `data/.config.yaml`：

```yaml
server:
  ip: 0.0.0.0
  port: 8000
  http_port: 8003
  # 公网/反代部署时务必改成真实可达地址，否则设备拿到的视觉接口地址不对
  vision_explain: http://你的IP或域名:8003/mcp/vision/explain
manager-api:
  url: http://127.0.0.1:8002/xiaozhi   # 同机用 localhost；跨机用 manager-api 的局域网 IP
  secret: <从智控台【参数管理】复制的 server.secret>
# 默认系统提示词模板文件
prompt_template: agent-base-prompt.txt
```

> `manager-api.secret` 必须与智控台里的 `server.secret` **完全一致**，否则 server 连不上后端。
> 每次从零部署 manager-api，这个 secret 都会重新生成，记得同步过来。

---

## 五、首次启动 & 验证

```bash
# 在项目根目录（main/xiaozhi-server）
uv run python app.py
```

看到以下日志即成功（记下里面的 ws / OTA 地址，烧 ESP32 固件要用）：

```
OTA接口是           http://<服务器IP>:8003/xiaozhi/ota/
Websocket地址是     ws://<服务器IP>:8000/xiaozhi/v1/
```

- `8000`：WebSocket 对话端口（ESP32 设备连这个）
- `8003`：HTTP 端口（OTA 接口、视觉分析接口）

> 源码部署时日志地址基本可信，但仍以服务器实际对外 IP/域名为准。

---

## 六、用 systemd 常驻运行（生产必做）

手动跑会随终端关闭而退出。下面把 `xiaozhi-server` 注册成系统服务，开机自启、崩溃自动重启。

### 6.1 创建服务文件

```bash
sudo tee /etc/systemd/system/xiaozhi-server.service > /dev/null <<'EOF'
[Unit]
Description=xiaozhi-esp32-server (xiaozhi-server)
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/xiaozhi-esp32-server/main/xiaozhi-server
ExecStart=/home/ubuntu/xiaozhi-esp32-server/main/xiaozhi-server/.venv/bin/python app.py
Restart=on-failure
RestartSec=5
# 中文环境，避免日志乱码
Environment=LANG=zh_CN.UTF-8
Environment=LC_ALL=zh_CN.UTF-8
Environment=PYTHONUNBUFFERED=1

[Install]
WantedBy=multi-user.target
EOF
```

> 把上面路径中的 `ubuntu` 和 `/home/ubuntu/...` 换成你的实际 **运行用户** 与 **项目绝对路径**。
> 运行用户需要对项目目录有读写权限（日志会写到 `tmp/`）。

### 6.2 启动 & 查状态

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now xiaozhi-server

# 查看状态
systemctl status xiaozhi-server --no-pager

# 跟踪日志
journalctl -u xiaozhi-server -f
```

常用运维命令：

```bash
sudo systemctl restart xiaozhi-server   # 改完配置后重启
sudo systemctl stop xiaozhi-server      # 停止
sudo journalctl -u xiaozhi-server -n 100 --no-pager  # 看最近 100 行
```

---

## 七、防火墙 & 对外暴露

### 7.1 放行端口

- 若用 **ufw**：
  ```bash
  sudo ufw allow 8000/tcp
  sudo ufw allow 8003/tcp
  ```
- 若用 **云厂商安全组 / 1panel 防火墙**：放行 `8000`、`8003` 入站 TCP。

### 7.2 公网 HTTPS / wss（推荐用 Nginx 反代）

ESP32 固件支持 `wss://` 与 `https://` OTA。若需公网加密访问，在服务器前置 Nginx：

```nginx
# /etc/nginx/sites-available/xiaozhi.conf
server {
    listen 443 ssl;
    server_name xiaozhi.yourdomain.com;

    ssl_certificate     /path/to/fullchain.pem;
    ssl_certificate_key /path/to/privkey.pem;

    # WebSocket 对话
    location /xiaozhi/v1/ {
        proxy_pass http://127.0.0.1:8000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 3600s;
    }

    # OTA / 视觉分析等 HTTP 接口
    location / {
        proxy_pass http://127.0.0.1:8003;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

配好后，ESP32 固件里的服务器地址填写 `wss://xiaozhi.yourdomain.com/xiaozhi/v1/`，
并把 `data/.config.yaml` 的 `server.websocket` / `vision_explain` 改为公网地址。

---

## 八、升级

```bash
cd xiaozhi-esp32-server
git pull
cd main/xiaozhi-server

# 重建环境并升级依赖
uv venv --python 3.10
uv pip install -r requirements.txt -i https://mirrors.aliyun.com/pypi/simple/

# 重启服务（若用 systemd）
sudo systemctl restart xiaozhi-server
```

> 升级前建议备份 `data/.config.yaml`：它是你的密钥与模式配置，升级不会动它，
> 但新版本 `config.yaml` 模板可能新增配置项，必要时手动对齐。

---

## 九、常见问题排查

| 现象 | 原因 / 解决 |
|------|-------------|
| `找不到data/.config.yaml文件` | 还没创建 `data/.config.yaml`，按第四节建好。 |
| `libopus` / `opus` 相关报错 | 系统缺原生 Opus 编解码库：`sudo apt install -y libopus0`（服务器版不需要 conda 的 opus.dll）。 |
| `libiconv.so.2: cannot open shared object file` | 个别精简系统缺失：`sudo apt install -y libiconv`（部分发行版需用 conda：`conda install libiconv`）。 |
| 找不到 `ffmpeg` | `sudo apt install -y ffmpeg`，并确认在 `PATH` 中。 |
| 启动报「找不到模型」 | 确认 `models/SenseVoiceSmall/model.pt` 存在且下载完整。 |
| 端口被占用 | 8000/8003 被占用：改 `data/.config.yaml` 的 `server.port` / `server.http_port`，或释放占用进程。 |
| 模式 B 连不上 manager-api | 检查 `manager-api.url` 可达、`manager-api.secret` 与智控台 `server.secret` 完全一致。 |
| 设备能连上但视觉接口 404 | `server.vision_explain` 没改成真实可达地址（Docker/公网下日志里的地址不可信）。 |
| 日志中文乱码 | 设好 `zh_CN.UTF-8` locale（见 2.1 / 6.1 的 Environment）。 |

---

## 十、架构说明（ARM 服务器）

本指南是**源码直接运行**，不受官方 Docker 镜像「仅 x86_64」限制：
ARM64 服务器（Oracle ARM、树莓派、飞腾等）按本文步骤原生跑即可，**无需像 `docs/docker-build.md` 那样自编译镜像**。
唯一注意点是 `torch` / `funasr` 等需要对应架构的 wheel，`uv` 会自动选取，安装耗时可能更长。

---

## 附：一键检查清单

- [ ] `ffmpeg` / `libopus0` 已装，`zh_CN.UTF-8` locale 已生成
- [ ] `uv venv --python 3.10` + `uv pip install -r requirements.txt` 成功
- [ ] `models/SenseVoiceSmall/model.pt` 已就位
- [ ] `data/.config.yaml` 已按模式 A / B 配好
- [ ] `uv run python app.py` 能看到 ws / OTA 地址
- [ ] systemd 服务已 `enable --now`，`journalctl` 无报错
- [ ] 防火墙 / 安全组放行 8000、8003
- [ ] （公网）Nginx 反代 + 固件地址改为 wss/https
