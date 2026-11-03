# xiaozhi-server 启动指南（uv + Python 3.10）

本指南用 [`uv`](https://docs.astral.sh/uv/) 管理 Python 3.10 虚拟环境来启动 `main/xiaozhi-server`（核心对话服务）。
它等价于官方文档里用 conda 的方案，只是把环境管理器换成了 uv。适用于本地开发调试，
也适用于 1panel / Ubuntu 服务器（仅系统依赖的安装方式不同）。

---

## 一、前置条件

- **uv**：https://docs.astral.sh/uv/getting-started/installation/
  - Windows：`powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"`
  - macOS/Linux：`curl -LsSf https://astral.sh/uv/install.sh | sh`
- **Python 3.10**：uv 会在 `uv venv --python 3.10` 时自动下载，本机无需预装。
- **系统依赖**（不是 pip 包，必须单独装）：
  - `ffmpeg`：**必须**，音频处理/转码
  - `libopus`：语音编解码
  - `libiconv`：部分 Linux 缺失时报错
- **语音识别模型** `model.pt`：默认离线 ASR 用 `SenseVoiceSmall`（几百 MB）。

---

## 二、获取源码

```bash
git clone https://github.com/LeeDebug/xiaozhi-esp32-server
cd xiaozhi-esp32-server/main/xiaozhi-server
```

> 如果你已经在本机改着代码，直接进入你仓库里的 `main/xiaozhi-server` 目录即可，
> 不需要重新 clone。

---

## 三、用 uv 创建 3.10 环境并装依赖

```bash
# 1. 创建 Python 3.10 虚拟环境（uv 会自动拉取 3.10）
uv venv --python 3.10

.venv\Scripts\activate

# 2. 安装依赖（uv pip 直接装进 .venv，无需先 activate）
uv pip install -r requirements.txt
```

国内镜像加速（可选）：

```bash
uv pip install -r requirements.txt -i https://mirrors.aliyun.com/pypi/simple/
```

> 说明：仓库里同时有 `pyproject.toml` 和 `requirements.txt`。这里用 `requirements.txt`
> 最稳妥；如果你更习惯 `uv sync` 也可以，但 `.txt` 是官方文档指定的依赖来源。

---

## 四、下载语音识别模型

默认 ASR 用本地离线 `SenseVoiceSmall`，需把 `model.pt` 放到：

```
main/xiaozhi-server/models/SenseVoiceSmall/model.pt
```

下载（任选其一）：
- 阿里魔搭：`https://modelscope.cn/models/iic/SenseVoiceSmall/resolve/master/model.pt`
- 百度网盘：提取码 `qvna`

- **Windows**：用浏览器或下载工具下。
- **Linux 服务器**：直接 `wget`/`curl` 拉，别走网页上传（文件大）。

---

## 五、配置文件 `data/.config.yaml`

`xiaozhi-server` 优先读取 `data/.config.yaml`。根据你要跑的模式二选一：

### A. 只跑 server（最简，无数据库）

把仓库里的 `config.yaml` 复制到 `data/.config.yaml`，改成最小可运行配置：

```yaml
server:
  websocket: ws://你的IP或域名:8000/xiaozhi/v1/
prompt: |
  我是一个叫小智的台湾女孩，说话机车……
selected_module:
  LLM: DoubaoLLM
LLM:
  ChatGLMLLM:
    api_key: xxxxxx
```

### B. 全模块（对接 manager-api，需要 MySQL/Redis 已启动）

用仓库里的 `config_from_api.yaml` 作模板（复制为 `data/.config.yaml`），关键是加 `manager-api` 块：

```yaml
server:
  websocket: ws://你的IP或域名:8000/xiaozhi/v1/
manager-api:
  url: http://localhost:8002/xiaozhi
  secret: <从智控台复制的 server.secret>
```

> 全模块模式下，模型 / LLM / TTS 等密钥都在智控台网页里配置，`config.yaml` 不需要写。
> `manager-api.secret` 必须和智控台里 `server.secret` **完全一致**，否则 server 连不上后端。

---

## 六、启动

```bash
# 方式一：uv run（自动使用 .venv，无需手动 activate）
uv run python app.py

# 方式二：先激活再用 python
#   Windows:  .venv\Scripts\activate
#   Linux:    source .venv/bin/activate
#   然后:     python app.py
```

---

## 七、验证启动成功

看到类似日志即成功（注意里面给你的 ws / OTA 地址，后面烧 ESP32 固件要用）：

```
OTA接口是           http://192.168.x.x:8003/xiaozhi/ota/
Websocket地址是     ws://192.168.x.x:8000/xiaozhi/v1/
```

- `8000`：WebSocket 对话端口（ESP32 设备连这个）
- `8003`：HTTP 端口（OTA 接口、视觉分析接口）

> 注：Docker 部署时日志里打印的地址不一定是真实地址；源码部署时日志地址基本可信，
> 但仍以服务器实际对外 IP/域名为准。

---

## 八、常见问题

- **找不到 ffmpeg / libopus**（Windows 常见）：
  `opuslib_next` 只是 Python 封装，真正需要系统原生 **Opus 编解码库**（`opus.dll`）。
  - **Linux**：`apt install -y ffmpeg libopus0`，缺失 libiconv 再补。
  - **Windows**：uv/pip 不会装原生 DLL，需手动提供 `opus.dll`（64 位，与 Python 匹配）：
    1. 若有 Anaconda/Miniconda：`conda install libopus -y`，再把
       `<conda>/Library/bin/opus.dll` 复制到 `main/xiaozhi-server/.venv/Scripts/`；
    2. 若无 conda：从 Opus 官网（opus-codec.org/downloads）或 ffmpeg 共享版 `bin` 里取
       `opus.dll`，放到 `.venv/Scripts/` 或 `C:\Windows\System32/`；
    3. 改完 PATH 后**重开终端**再 `uv run python app.py`。
  - ffmpeg 同样需在 `PATH`：Windows 装 ffmpeg 共享版并把其 `bin` 加入 PATH。
- **libiconv.so.2 缺失（Linux）**：`apt install libiconv`（或装到 `/usr/lib`）。
- **模型路径不对**：确认 `models/SenseVoiceSmall/model.pt` 存在，否则启动报找不到模型。
- **端口被占**：8000/8003 被占用就改 `config.yaml` 里的端口或关掉占用进程。
- **全模块连不上 manager-api**：检查 `manager-api.secret` 是否与智控台 `server.secret` 一致。

---

## 九、1panel / Ubuntu 服务器差异

- **系统依赖**：`apt update && apt install -y ffmpeg libopus0`（libiconv 名称可能不同，缺失再补）。
- **模型**：`cd models/SenseVoiceSmall && wget <魔搭地址>`。
- **启动**：建议 `uv venv` + `uv run python app.py`，再用 systemd / supervisor 守护进程；
  或按项目 `docs/docker-build.md` 自己构建镜像，用 1panel「容器 → 编排」管理。
- **对外暴露**：1panel「防火墙」放行 8000 / 8003；如需 HTTPS / wss，用 1panel「网站（OpenResty）」
  做反向代理。
- **架构注意**：官方预构建镜像仅 x86_64；ARM 服务器（Oracle ARM / 树莓派）必须按
  `docs/docker-build.md` 自编译，不能直接拉官方镜像。
