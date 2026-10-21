import sys
import os
import io

# Prevent Intel OpenMP conflict / WinError 1114
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

# Ensure PyTorch's bundled Intel OpenMP runtime (libiomp5md.dll) and dependencies
# are preloaded BEFORE any other library can load conflicting copies from C:\Windows\System32 or PATH.
import ctypes

def preload_torch_dlls():
    try:
        search_dirs = []
        try:
            import site
            us = site.getusersitepackages()
            if isinstance(us, str):
                search_dirs.append(us)
            elif isinstance(us, (list, tuple)):
                search_dirs.extend(us)
            search_dirs.extend(site.getsitepackages())
        except Exception:
            pass

        if hasattr(sys, "prefix"):
            search_dirs.append(os.path.join(sys.prefix, "Lib", "site-packages"))
        if hasattr(sys, "base_prefix"):
            search_dirs.append(os.path.join(sys.base_prefix, "Lib", "site-packages"))

        appdata = os.environ.get("APPDATA")
        if appdata:
            for pyver in ["Python312", "Python311", "Python310", "Python313"]:
                search_dirs.append(os.path.join(appdata, "Python", pyver, "site-packages"))

        localappdata = os.environ.get("LOCALAPPDATA")
        if localappdata:
            for pyver in ["Python312", "Python311", "Python310", "Python313"]:
                search_dirs.append(os.path.join(localappdata, "Programs", "Python", pyver, "Lib", "site-packages"))

        for prog in [r"C:\Program Files\Python312", r"C:\Program Files\Python311", r"C:\Program Files\Python310"]:
            search_dirs.append(os.path.join(prog, "Lib", "site-packages"))

        for base in search_dirs:
            if not base or not os.path.isdir(base):
                continue
            tlib = os.path.join(base, "torch", "lib")
            if os.path.isdir(tlib):
                if hasattr(os, "add_dll_directory"):
                    try:
                        os.add_dll_directory(tlib)
                    except Exception:
                        pass
                os.environ["PATH"] = tlib + os.pathsep + os.environ.get("PATH", "")

                # Crucial: Preload PyTorch's own libiomp5md.dll, uv.dll, c10.dll, and torch_cpu.dll
                # in order so Windows does not load incompatible versions from System32
                for dll_name in ["libiomp5md.dll", "uv.dll", "c10.dll", "torch_cpu.dll", "torch_global_deps.dll", "torch.dll"]:
                    dll_file = os.path.join(tlib, dll_name)
                    if os.path.exists(dll_file):
                        try:
                            ctypes.CDLL(dll_file)
                        except Exception:
                            pass
    except Exception:
        pass

preload_torch_dlls()

# Pre-import torch while DLL directories and preloads are in scope
try:
    import torch
except Exception:
    pass

# Ensure ffmpeg is in PATH on Windows
ffmpeg_candidates = [
    r"C:\Ai\ffmpeg",
    r"C:\ffmpeg\bin",
    r"C:\Program Files\ffmpeg\bin",
    os.path.join(os.environ.get("USERPROFILE", ""), "scoop", "shims"),
    os.path.join(os.environ.get("LOCALAPPDATA", ""), "Programs", "ffmpeg", "bin"),
]
for p in ffmpeg_candidates:
    if os.path.exists(p) and p not in os.environ.get("PATH", ""):
        os.environ["PATH"] = p + os.pathsep + os.environ.get("PATH", "")

# Ensure UTF-8 output on Windows
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8')

import json
import whisper

def transcribe(audio_path, model_name="base", output_file=None, language=None):
    if not os.path.exists(audio_path):
        return {"error": f"Audio file not found: {audio_path}"}

    # Normalize model name
    m_lower = (model_name or "base").strip().lower()
    if m_lower in ("large-v3-turbo", "turbo-large", "large_turbo"):
        resolved_model = "large-v3-turbo"
    elif m_lower in ("large", "large-v3"):
        resolved_model = "large-v3"
    elif m_lower in ("tiny", "default", ""):
        resolved_model = "base"
    else:
        resolved_model = m_lower

    # High-performance multi-threaded CPU execution
    device = "cpu"
    print(f"Loading Whisper model: {resolved_model} on CPU", flush=True)
    model = whisper.load_model(resolved_model, device=device)
    print(f"Model loaded. Starting transcription...", flush=True)

    kwargs = {"verbose": True, "fp16": False, "condition_on_previous_text": False, "word_timestamps": True}
    if language and language.lower() not in ("auto", "none", ""):
        lang_lower = language.lower()
        if lang_lower == "hinglish":
            kwargs["language"] = "hi"
        elif lang_lower == "benglish":
            kwargs["language"] = "bn"
        elif lang_lower == "tanglish":
            kwargs["language"] = "ta"
        elif lang_lower == "pinglish":
            kwargs["language"] = "pa"
        else:
            kwargs["language"] = lang_lower

    try:
        result = model.transcribe(audio_path, **kwargs)
    except Exception as e:
        err = str(e)
        # Whisper may throw when there are no vocals / silence
        if "model output" in err.lower() or "no speech" in err.lower() or "empty" in err.lower():
            return {"error": "No vocals detected in the audio. The track may be instrumental or the audio quality is too low for speech recognition."}
        raise

    segments = []
    for s in result.get("segments", []):
        # Skip segments flagged as no_speech by Whisper
        if s.get("no_speech_prob", 0) > 0.85:
            continue
        words = s.get("words", [])
        if words and (float(s["end"]) - float(s["start"]) > 5.0 or len(words) > 7):
            cur_line_words = []
            cur_start = None
            prev_end = float(s["start"])
            for w in words:
                w_text = w.get("word", "").strip()
                if not w_text:
                    continue
                w_start = round(float(w.get("start", prev_end)), 2)
                w_end = round(float(w.get("end", w_start)), 2)
                if cur_start is None:
                    cur_start = w_start
                gap = w_start - prev_end
                cur_line_words.append(w_text)
                prev_end = w_end

                is_punct = any(w_text.endswith(p) for p in (".", "?", "!", "|", "\n"))
                is_comma = (w_text.endswith(",") or w_text.endswith("，")) and len(" ".join(cur_line_words)) >= 20
                is_gap = gap >= 0.85 and len(cur_line_words) > 1
                is_len = len(" ".join(cur_line_words)) >= 45

                if is_punct or is_comma or is_gap or is_len:
                    line_text = " ".join(cur_line_words).strip()
                    if line_text:
                        segments.append({"start": cur_start, "end": prev_end, "text": line_text})
                    cur_line_words = []
                    cur_start = None
            if cur_line_words:
                line_text = " ".join(cur_line_words).strip()
                if line_text:
                    segments.append({"start": cur_start if cur_start is not None else round(float(s["start"]), 2), "end": prev_end, "text": line_text})
        else:
            start = round(float(s["start"]), 2)
            end = round(float(s["end"]), 2)
            text = s["text"].strip()
            if text:
                segments.append({
                    "start": start,
                    "end": end,
                    "text": text
                })

    plain_text = result.get("text", "").strip()

    if not plain_text and not segments:
        return {"error": "No vocals detected. The track may be instrumental or speech recognition failed on this audio."}

    data = {
        "text": plain_text,
        "segments": segments,
        "language": result.get("language", "en")
    }

    if output_file:
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)

    return data

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Missing audio file path argument"}))
        sys.exit(1)
        
    audio_path = sys.argv[1]
    model_name = sys.argv[2] if len(sys.argv) > 2 else "base"
    out_file = sys.argv[3] if len(sys.argv) > 3 and sys.argv[3] != "none" else None
    language = sys.argv[4] if len(sys.argv) > 4 and sys.argv[4] != "auto" else None
    
    try:
        data = transcribe(audio_path, model_name, out_file, language)
        # If the script returned an error dict, write it and exit with error code
        if "error" in data:
            err_msg = data["error"]
            if out_file:
                try:
                    with open(out_file, "w", encoding="utf-8") as f:
                        json.dump({"error": err_msg}, f)
                except Exception:
                    pass
            print(json.dumps({"error": err_msg}))
            sys.exit(1)
        print("---TRANSCRIPTION_START---")
        print(json.dumps(data, ensure_ascii=False))
        print("---TRANSCRIPTION_END---")
    except Exception as e:
        import traceback
        traceback.print_exc(file=sys.stderr)
        err_str = str(e)
        # Translate Whisper internal errors into friendly messages
        if "model output" in err_str.lower():
            err_msg = "No vocals detected in this audio. The track may be instrumental."
        elif "ffmpeg" in err_str.lower() or "FileNotFoundError" in err_str:
            err_msg = "ffmpeg not found. Ensure ffmpeg is installed and in PATH."
        else:
            err_msg = f"{type(e).__name__}: {err_str}"
        if out_file:
            try:
                with open(out_file, "w", encoding="utf-8") as f:
                    json.dump({"error": err_msg}, f)
            except Exception:
                pass
        print(json.dumps({"error": err_msg}))
        sys.exit(1)
