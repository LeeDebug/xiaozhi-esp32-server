"""
TTS上报功能已集成到ConnectionHandler类中。

上报功能包括：
1. 每个连接对象拥有自己的上报队列和处理线程
2. 上报线程的生命周期与连接对象绑定
3. 使用ConnectionHandler.enqueue_tts_report方法进行上报

具体实现请参考core/connection.py中的相关代码。
"""

import time
import json
import opuslib_next
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from core.connection import ConnectionHandler

from config.manage_api_client import report as manage_report

TAG = __name__


async def report(conn: "ConnectionHandler", chat_type, text, audio_data, report_time, extra=None):
    """执行聊天记录上报操作

    Args:
        conn: 连接对象
        chat_type: 上报类型，1为用户(ASR/PCM)，2为智能体(TTS/Opus)，3为工具调用
        text: 合成文本
        audio_data: 音频数据（chat_type=1时为PCM格式，chat_type=2时为Opus格式）
        report_time: 上报时间
        extra: 本轮耗时快照（dict: roundId / metrics），仅末条 TTS 记录携带
    """
    try:
        if audio_data:

            if chat_type == 1:
                wav_data = pcm_to_wav(conn, audio_data)
            elif chat_type == 2:
                wav_data = opus_to_wav(conn, audio_data)
            else:
                wav_data = None
        else:
            wav_data = None
        # 执行异步上报
        await manage_report(
            mac_address=conn.device_id,
            session_id=conn.session_id,
            chat_type=chat_type,
            content=text,
            audio=wav_data,
            report_time=report_time,
            extra=extra,
        )
    except Exception as e:
        conn.logger.bind(tag=TAG).error(f"聊天记录上报失败: {e}")


def pcm_to_wav(conn: "ConnectionHandler", pcm_data):
    """将PCM数据转换为WAV格式的字节流

    Args:
        conn: 连接对象
        pcm_data: PCM音频数据（可能是列表或bytes）

    Returns:
        bytes: WAV格式的音频数据
    """
    try:
        # 处理可能是列表或bytes的PCM数据
        if isinstance(pcm_data, list):
            pcm_data_bytes = b"".join(pcm_data)
        else:
            pcm_data_bytes = pcm_data

        if not pcm_data_bytes:
            raise ValueError("没有有效的PCM数据")

        # 创建WAV文件头
        num_samples = len(pcm_data_bytes) // 2  # 16-bit samples

        # WAV文件头
        wav_header = bytearray()
        wav_header.extend(b"RIFF")  # ChunkID
        wav_header.extend((36 + len(pcm_data_bytes)).to_bytes(4, "little"))  # ChunkSize
        wav_header.extend(b"WAVE")  # Format
        wav_header.extend(b"fmt ")  # Subchunk1ID
        wav_header.extend((16).to_bytes(4, "little"))  # Subchunk1Size
        wav_header.extend((1).to_bytes(2, "little"))  # AudioFormat (PCM)
        wav_header.extend((1).to_bytes(2, "little"))  # NumChannels
        wav_header.extend((16000).to_bytes(4, "little"))  # SampleRate
        wav_header.extend((32000).to_bytes(4, "little"))  # ByteRate
        wav_header.extend((2).to_bytes(2, "little"))  # BlockAlign
        wav_header.extend((16).to_bytes(2, "little"))  # BitsPerSample
        wav_header.extend(b"data")  # Subchunk2ID
        wav_header.extend(len(pcm_data_bytes).to_bytes(4, "little"))  # Subchunk2Size

        # 返回完整的WAV数据
        return bytes(wav_header) + pcm_data_bytes
    except Exception as e:
        conn.logger.bind(tag=TAG).error(f"PCM转WAV失败: {e}", exc_info=True)
        raise


def opus_to_wav(conn: "ConnectionHandler", opus_data):
    """将Opus数据转换为WAV格式的字节流

    Args:
        conn: 连接对象
        opus_data: Opus音频数据（可能是列表或bytes）

    Returns:
        bytes: WAV格式的音频数据
    """
    decoder = None
    try:
        decoder = opuslib_next.Decoder(16000, 1)
        pcm_data = []

        if isinstance(opus_data, list):
            for opus_packet in opus_data:
                try:
                    pcm_frame = decoder.decode(opus_packet, 960)
                    pcm_data.append(pcm_frame)
                except opuslib_next.OpusError as e:
                    conn.logger.bind(tag=TAG).error(f"Opus解码错误: {e}", exc_info=True)
        elif isinstance(opus_data, bytes):
            pcm_frame = decoder.decode(opus_data, 960)
            pcm_data.append(pcm_frame)

        if not pcm_data:
            raise ValueError("没有有效的音频数据")

        pcm_data_bytes = b"".join(pcm_data)

        wav_header = bytearray()
        wav_header.extend(b"RIFF")
        wav_header.extend((36 + len(pcm_data_bytes)).to_bytes(4, "little"))
        wav_header.extend(b"WAVE")
        wav_header.extend(b"fmt ")
        wav_header.extend((16).to_bytes(4, "little"))
        wav_header.extend((1).to_bytes(2, "little"))
        wav_header.extend((1).to_bytes(2, "little"))
        wav_header.extend((16000).to_bytes(4, "little"))
        wav_header.extend((32000).to_bytes(4, "little"))
        wav_header.extend((2).to_bytes(2, "little"))
        wav_header.extend((16).to_bytes(2, "little"))
        wav_header.extend(b"data")
        wav_header.extend(len(pcm_data_bytes).to_bytes(4, "little"))

        return bytes(wav_header) + pcm_data_bytes
    finally:
        if decoder is not None:
            try:
                del decoder
            except Exception as e:
                conn.logger.bind(tag=TAG).debug(f"释放decoder资源时出错: {e}")


def build_round_metrics(conn: "ConnectionHandler"):
    """生成一轮问答的耗时快照，仅在每轮最后一句 TTS 上报时调用。

    startedAt 是 time.perf_counter() 的单调时钟值，只用于进程内求差，
    必须在这里剔除，绝不能把单调时钟值写进上报 JSON 或数据库。
    埋点缺失（如历史会话、异常轮次）时返回 None，前端会隐藏面板。
    """
    raw = getattr(conn, "chat_round_metrics", None)
    if not raw or not raw.get("roundId"):
        return None
    started_at = raw.get("startedAt")
    # roundId 单独作为上报字段，不再重复塞进 metrics
    metrics = {
        key: value for key, value in raw.items()
        if key not in ("startedAt", "roundId")
    }
    if started_at is not None:
        metrics["e2eTotalMs"] = int((time.perf_counter() - started_at) * 1000)
    return {"roundId": raw["roundId"], "metrics": metrics}


def enqueue_tts_report(conn: "ConnectionHandler", text, opus_data, is_last=False):
    """将TTS数据加入上报队列

    Args:
        conn: 连接对象
        text: 合成文本
        opus_data: opus音频数据
        is_last: 是否为本轮最后一句；为 True 时附带本轮耗时快照
    """
    if not conn.read_config_from_api or conn.need_bind or not conn.report_tts_enable:
        return
    if conn.chat_history_conf == 0:
        return
    try:
        # 耗时只在最后一句携带，避免同一轮的 metrics 被重复写入多条记录
        extra = build_round_metrics(conn) if is_last else None
        if is_last:
            # 自检日志：耗时面板不显示时，这一行可直接区分「Python 没生成快照」
            # 与「生成了但 Java 没收到」。hasSnapshot=False 说明 chat_round_metrics
            # 未初始化，或 tts/base.py / 本文件仍是旧版（旧版不传 is_last）。
            conn.logger.bind(tag=TAG).info(
                f"本轮耗时快照自检: hasSnapshot={extra is not None}, "
                f"roundId={extra.get('roundId') if extra else None}"
            )
        # 使用连接对象的队列，传入文本和二进制数据而非文件路径
        if conn.chat_history_conf == 2:
            conn.report_queue.put((2, text, opus_data, int(time.time() * 1000), extra))
            conn.logger.bind(tag=TAG).debug(
                f"TTS数据已加入上报队列: {conn.device_id}, 音频大小: {len(opus_data)} "
            )
        else:
            conn.report_queue.put((2, text, None, int(time.time() * 1000), extra))
            conn.logger.bind(tag=TAG).debug(
                f"TTS数据已加入上报队列: {conn.device_id}, 不上报音频"
            )
    except Exception as e:
        conn.logger.bind(tag=TAG).error(f"加入TTS上报队列失败: {text}, {e}")


def enqueue_tool_report(conn: "ConnectionHandler", tool_name: str, tool_input: dict, tool_result: str = None, report_tool_call: bool = True):
    """将工具调用数据加入上报队列

    Args:
        conn: 连接对象
        tool_name: 工具名称
        tool_input: 工具输入参数
        tool_result: 工具执行结果（可选）
        report_tool_call: 是否上报工具调用本身，默认True；仅上报结果时设为False
    """
    if not conn.read_config_from_api or conn.need_bind:
        return
    if conn.chat_history_conf == 0:
        return

    try:
        timestamp = int(time.time() * 1000)

        # 构建工具调用内容
        if report_tool_call:
            tool_text = json.dumps(
                [
                    {
                        "type": "tool",
                        "text": f"{tool_name}({json.dumps(tool_input, ensure_ascii=False)})",
                    }
                ]
            )
            conn.report_queue.put((3, tool_text, None, timestamp, None))

        # 构建工具结果内容
        if tool_result:
            result_display = f'{{"result":"{str(tool_result)}"}}'
            result_content = json.dumps([{"type": "tool_result", "text": result_display}], ensure_ascii=False)
            conn.report_queue.put((3, result_content, None, timestamp + 1, None))
    except Exception as e:
        conn.logger.bind(tag=TAG).error(f"加入工具上报队列失败: {e}")


def enqueue_asr_report(conn: "ConnectionHandler", text, opus_data):
    """将ASR数据加入上报队列

    Args:
        conn: 连接对象
        text: 合成文本
        opus_data: opus音频数据
    """
    if not conn.read_config_from_api or conn.need_bind or not conn.report_asr_enable:
        return
    if conn.chat_history_conf == 0:
        return
    try:
        # 使用连接对象的队列，传入文本和二进制数据而非文件路径
        if conn.chat_history_conf == 2:
            conn.report_queue.put((1, text, opus_data, int(time.time() * 1000), None))
            conn.logger.bind(tag=TAG).debug(
                f"ASR数据已加入上报队列: {conn.device_id}, 音频大小: {len(opus_data)} "
            )
        else:
            conn.report_queue.put((1, text, None, int(time.time() * 1000), None))
            conn.logger.bind(tag=TAG).debug(
                f"ASR数据已加入上报队列: {conn.device_id}, 不上报音频"
            )
    except Exception as e:
        conn.logger.bind(tag=TAG).debug(f"加入ASR上报队列失败: {text}, {e}")
