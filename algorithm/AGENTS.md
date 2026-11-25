# Python 算法

本模块接收已交付的模型权重，负责部署和推理。一个 Python 工程容纳多个算法，推理代码放在 `starter_algorithm/algorithms/<算法名>/`，每个算法维护自己的处理流程、模型说明和输入输出约定。

| 文件 | 职责 |
| --- | --- |
| `preprocess.py` | 将业务输入转换为模型所需的形状、类型和数值范围。 |
| `model.py` | 定义模型路径、输入输出名及加载入口，调用公共的 `starter_algorithm/onnx_runtime.py` 创建并检查 ONNX 会话。 |
| `postprocess.py` | 将模型输出解码成算法返回值。 |
| `pipeline.py` | 串起前处理、推理和后处理。创建 `Predictor` 时加载模型，同一个实例供多次预测复用。 |
| `models/README.md` | 记录权重版本、来源及输入输出约定。部署时提供真实权重及相关外部张量文件。 |

## 接入一个 ONNX 模型

接入、部署或排查 ONNX 模型时，先读 [ONNX 推理部署 Skill](skills/onnx-inference-deployment/SKILL.md)。 选择 Triton Inference Server 或 Windows ML 时，查阅[算法 Skill 索引](skills/README.md)中的对应入口。

1. 复制 [`onnx_example/`](starter_algorithm/algorithms/onnx_example/pipeline.py) 为新的算法目录。先确定模型的输入输出名称、形状、类型和单位，再填写 `model.py`、前处理和后处理。
2. 权重放入该算法的 `models/`，也可通过 `Predictor(model_path=...)` 指定路径。该目录的权重文件通过模型存储或部署挂载提供，Git 保留模型说明。
3. 在 `algorithm/` 用 `uv sync --locked --extra onnx` 安装 CPU 推理依赖。在本机用小样本调用 `Predictor.predict()` 核对预处理和输出，再接入所选服务入口。模型部署可通过 `providers` 指定执行后端，基础镜像和推理依赖随所选硬件配置。

`http.py` 和 `worker.py` 是 FastAPI 与 Kafka 的传输参考，对应 `http` 和 `kafka` 可选依赖。现有 `engine.detect()` 连接 `algorithms/sample_demo/`，按 `contracts/detection.md` 返回可重复的样本结果。新业务在所选传输入口初始化并调用相应算法的 `Predictor`，按业务输入输出定义请求与结果契约。

- 推理验收保留原始样本，记录权重版本、样本、实际执行后端和对照结果。
- 使用 Hugging Face 模型评估时，读取 `skills/huggingface-community-evals/SKILL.md`，根据本机资源选后端、数据及基准。
- 添加依赖时更新 `pyproject.toml` 与 `uv.lock`。验证 ONNX 接入时记录实际会话的执行后端。

上游参考

- [Hugging Face Community Evals](https://github.com/huggingface/skills/tree/main/skills/huggingface-community-evals)
- [FastAPI 部署文档](https://fastapi.tiangolo.com/deployment/)
