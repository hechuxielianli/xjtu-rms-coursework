# pdfLaTeX 编译

源文件为 UTF-8，使用与作业 1 相同的 `\documentclass[UTF8,a4paper,12pt]{ctexart}`，左右 2.7 cm、上下 2.5 cm、1.25 倍行距。Windows 本机的 ctex 自动选择系统中文字体：SimSun 用于正文、表格和页眉，SimHei 用于主标题和章节标题；英文与数学保持普通 LaTeX 字体。实际编译后的字体均已嵌入，未使用 Arphic 字体或自定义中文字体命令覆盖。

在本目录执行两遍，引用变化时再执行一遍：

```sh
pdflatex -interaction=nonstopmode -halt-on-error -file-line-error RMS_软件系统分析与设计作业2.tex
pdflatex -interaction=nonstopmode -halt-on-error -file-line-error RMS_软件系统分析与设计作业2.tex
```

实际工具版本：`pdfTeX 3.141592653-2.6-1.40.29 (TeX Live 2026)`，kpathsea 6.4.2。没有使用 fontspec、xeCJK 或 unicode-math，也不需要 XeLaTeX。

图形从 `../docs/design/` 和 `../docs/screenshots/` 读取。正文核心 ER 页使用 `pdfpages` 保留 A3 横向矢量尺寸，其余正文为 A4。核心图保留全部实体、主外键和关键业务字段，重复的人员外键用 `FK·U` 表示；完整字段级 `complete-er.*` 仅作为仓库补充材料，不嵌入正式报告。正式 PDF 为 `RMS_软件系统分析与设计作业2.pdf`。

图 5、图 6 使用原始展示截图的矩形裁剪，去除外围和无关留白，未拼接或修改页面内容。完整原始编译日志和裁剪核对记录留在本地私人检查记录中，不公开含本机路径的日志。最终检查包括未定义引用、缺字、溢出、字体嵌入、截图像素核对和逐页渲染；字体核验命令为 `pdffonts RMS_软件系统分析与设计作业2.pdf`。
