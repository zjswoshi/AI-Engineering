# 项目内 OpenCodeReview

[`source/`](source/) 包含 [alibaba/open-code-review](https://github.com/alibaba/open-code-review/tree/f6379f71edba1e59dbc056e8f455df1f84396498) `v1.14.3` 的跟踪文件快照。提交为 `f6379f71edba1e59dbc056e8f455df1f84396498`。许可见 [`source/LICENSE`](source/LICENSE)。

Windows x64 从项目根目录执行 `./tools/ocr.ps1 --version`。首次运行把随附的官方发行版解包到本项目的 `.runtime-cache/`，并使用上游发布的 SHA-256 校验。

```text
5531c50080feec9bc2b966c83e1498a3b910ac23bd907651c0cfe4e7f7463631  opencodereview-windows-amd64.exe
```

校验值记录在 [v1.14.3 的 sha256sum.txt](https://github.com/alibaba/open-code-review/releases/download/v1.14.3/sha256sum.txt)。Windows 入口使用项目根目录的 `.opencodereview/rule.json`。模型配置和凭证使用 OpenCodeReview 的本机配置。

## 从源码构建

其他平台可以用符合 [`source/go.mod`](source/go.mod) 要求的 Go 工具链，从项目根目录运行以下命令。首次源码构建需要获取 Go 模块依赖。

```sh
cd tools/opencodereview/source
go build -buildvcs=false -ldflags '-X main.Version=v1.14.3 -X main.GitCommit=f6379f71' -o ../ocr ./cmd/opencodereview
cd ../../..
./tools/opencodereview/ocr --version
```

在 Windows 从源码构建时，可将输出文件名设为 `../ocr.exe`。
