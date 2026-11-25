# 算法 Skill

先读[算法模块说明](../AGENTS.md)，再按任务选择 Skill。

- **ONNX 推理部署**用于接入现成权重、对齐前后处理、检查 CPU/CUDA 和验证推理。见 [ONNX Inference Deployment](onnx-inference-deployment/SKILL.md)。
- **NVIDIA Triton ONNX Model Repository** 用于 Triton Inference Server 的 ONNX 模型仓库、`config.pbtxt` 和部署排查。见 [本地 Skill](triton-onnx-model-repository/SKILL.md)。
- **Microsoft use-winml-cli** 用于 Windows ML 的模型准备、执行后端兼容性检查和推理性能评估。见 [本地 Skill](use-winml-cli/SKILL.md)。
- **Hugging Face Community Evals**用于在本机评估 Hugging Face Hub 模型。见 [Community Evals](huggingface-community-evals/SKILL.md)。

Community Evals 固定上游为 [huggingface/skills](https://github.com/huggingface/skills/tree/d67b12054923df5bbfa07c5ff1331412deebdf4f/skills/huggingface-community-evals) 的 `d67b120`。工具安装和评测环境视实际任务而定。

## 部署 Skill 的上游版本

| Skill | 上游快照 | 上游许可证 |
| --- | --- | --- |
| `triton-onnx-model-repository` | [NVIDIA/skills `a79ed3d`](https://github.com/NVIDIA/skills/tree/a79ed3dca393fd894ddfb907f4b5e70a4098ec80/skills/triton-onnx-model-repository) | [Apache 2.0](licenses/nvidia-skills/LICENSE-APACHE)、[CC BY 4.0](licenses/nvidia-skills/LICENSE-CC-BY-4.0) |
| `use-winml-cli` | [microsoft/winml-cli `5335672`](https://github.com/microsoft/winml-cli/tree/53356729ba848e6ec77513b3d628eefdcae591c8/skills/use-winml-cli) | [MIT](licenses/winml-cli/LICENSE.txt) |
