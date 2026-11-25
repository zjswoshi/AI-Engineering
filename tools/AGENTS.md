# 工具

`smoke.ps1` 检查正在运行的服务，传入 `-Frontend` 时同时验证首页和同源健康接口。

`ocr.ps1` 从项目根目录运行随附的 OpenCodeReview，源码、版本和可移植运行方式见 `opencodereview/README.md`。`opencodereview/source/` 保存上游快照，项目规则位于根目录 `.opencodereview/rule.json`。

工具应可从任意工作目录调用。出错返回非零状态。清理时尊重项目和数据卷范围。
