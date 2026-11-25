---
name: onnx-inference-deployment
description: 接入已有 ONNX 模型并验证部署推理。用于核对模型输入输出、实现前处理和后处理、检查 ONNX Runtime 的 CPU 或 CUDA 执行后端，以及排查推理结果和性能偏差。
---

# ONNX 推理部署

从 [算法模块说明](../../AGENTS.md) 和 [ONNX 示例](../../starter_algorithm/algorithms/onnx_example/pipeline.py) 找到目标算法的入口。使用模型提供方交付的权重、输入输出说明和参考样例。

## 接入模型

1. 在 `models/README.md` 记录权重版本、来源、外部张量文件和目标运行环境。通过 `model.py` 指定模型路径、张量名称和会话加载方式。
2. 用 [公共加载器](../../starter_algorithm/onnx_runtime.py) 创建会话，读取 `session.get_inputs()` 和 `session.get_outputs()`，记录每个张量的名称、形状和类型。结合模型提供方的说明确认动态维度、输出含义和类别映射。缺少输出说明时，先找参考实现或已知输入输出样例。
3. 在 `preprocess.py` 对齐模型要求。图像模型重点核对颜色通道、NCHW/NHWC 布局、缩放填充和归一化，并保留还原坐标所需的缩放与偏移。在 `postprocess.py` 核对张量布局、类别、坐标单位和阈值。需要 NMS 或坐标反变换时，先确认哪些步骤已经包含在 ONNX 图中。
4. 在 `pipeline.py` 按前处理、会话推理、后处理组织代码。初始化 `Predictor` 时加载模型，后续请求复用会话。接入所选的 HTTP 或 Kafka 入口时，沿用同一个 `Predictor`。

## 验证目标环境

- **设备与依赖。** 记录环境里的 ONNX Runtime 版本、请求的 providers 和 `session.get_providers()` 的结果。CUDA 初始化异常时，对照官方兼容表核查目标环境的 CUDA、cuDNN 及共享库加载，再用同一个环境的解释器复查会话。需要确认具体算子的执行位置时，查看 ONNX Runtime profiling 输出。
- **输出正确性。** 先跑代表性小样本，对照参考输出和适合任务的容差。检测任务可以选一份有检出样本、一份无检出样本，并核对原图坐标和类别。结果有偏差时依次比对预处理张量、原始模型输出和后处理结果。
- **性能。** 在目标设备用实际输入尺寸和批大小测量。区分首次加载与预热后耗时，分开记录前处理、推理、后处理的延迟。条件允许时记录峰值内存或显存，并说明计入端到端延迟的阶段。

交付时附上权重版本、实测执行后端、小样本对比结果和已测输入范围。

## 上游参考

- [ONNX Runtime Python API](https://onnxruntime.ai/docs/api/python/api_summary.html)
- [CUDA Execution Provider 兼容表](https://onnxruntime.ai/docs/execution-providers/CUDA-ExecutionProvider.html)
- [ONNX Runtime Profiling](https://onnxruntime.ai/docs/performance/tune-performance/profiling-tools.html)
