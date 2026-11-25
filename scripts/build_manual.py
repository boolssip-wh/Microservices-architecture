from __future__ import annotations

from datetime import date
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_CELL_VERTICAL_ALIGNMENT, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_BREAK, WD_LINE_SPACING
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt, RGBColor


PROJECT_ROOT = Path(__file__).resolve().parent.parent
DELIVERY_ROOT = PROJECT_ROOT.parent
OUTPUT = DELIVERY_ROOT / "初代作品使用说明书.docx"
FIGURES = Path(
    r"F:\CodexData\.codex\visualizations\2026\11\09"
    r"\01a1be40-5f56-7cd3-a0c9-ad6f99472ca4"
)

INK = "17231F"
GREEN = "185C4A"
LIGHT_GREEN = "DCEFE6"
PALE_GREEN = "F2F8F5"
BORDER = "D9D9D9"
GRAY = "66736D"
WHITE = "FFFFFF"


def set_font(run, name: str, size: float, bold: bool = False, color: str = INK):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.color.rgb = RGBColor.from_string(color)
    run._element.get_or_add_rPr().rFonts.set(qn("w:ascii"), name)
    run._element.get_or_add_rPr().rFonts.set(qn("w:hAnsi"), name)
    run._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), name)


def set_cell_shading(cell, color: str):
    tc_pr = cell._tc.get_or_add_tcPr()
    shading = tc_pr.find(qn("w:shd"))
    if shading is None:
        shading = OxmlElement("w:shd")
        tc_pr.append(shading)
    shading.set(qn("w:fill"), color)


def remove_paragraph_borders(paragraph_or_style):
    p_pr = paragraph_or_style._element.get_or_add_pPr()
    for border in p_pr.findall(qn("w:pBdr")):
        p_pr.remove(border)


def set_cell_margins(cell, top=110, start=110, bottom=110, end=110):
    tc = cell._tc
    tc_pr = tc.get_or_add_tcPr()
    tc_mar = tc_pr.first_child_found_in("w:tcMar")
    if tc_mar is None:
        tc_mar = OxmlElement("w:tcMar")
        tc_pr.append(tc_mar)
    for edge, value in (("top", top), ("start", start), ("bottom", bottom), ("end", end)):
        node = tc_mar.find(qn(f"w:{edge}"))
        if node is None:
            node = OxmlElement(f"w:{edge}")
            tc_mar.append(node)
        node.set(qn("w:w"), str(value))
        node.set(qn("w:type"), "dxa")


def set_table_borders(table, color=BORDER, size="6"):
    tbl_pr = table._tbl.tblPr
    borders = tbl_pr.first_child_found_in("w:tblBorders")
    if borders is None:
        borders = OxmlElement("w:tblBorders")
        tbl_pr.append(borders)
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        element = borders.find(qn(f"w:{edge}"))
        if element is None:
            element = OxmlElement(f"w:{edge}")
            borders.append(element)
        element.set(qn("w:val"), "single")
        element.set(qn("w:sz"), size)
        element.set(qn("w:space"), "0")
        element.set(qn("w:color"), color)


def set_repeat_header(row):
    tr_pr = row._tr.get_or_add_trPr()
    repeat = OxmlElement("w:tblHeader")
    repeat.set(qn("w:val"), "true")
    tr_pr.append(repeat)


def set_cant_split(row):
    tr_pr = row._tr.get_or_add_trPr()
    cant_split = OxmlElement("w:cantSplit")
    cant_split.set(qn("w:val"), "true")
    tr_pr.append(cant_split)


def set_col_widths(table, widths: list[float]):
    for row in table.rows:
        for index, width in enumerate(widths):
            row.cells[index].width = Inches(width)


def cell_text(cell, text: str, bold=False, color=INK, size=9.5, align=WD_ALIGN_PARAGRAPH.LEFT):
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER
    set_cell_margins(cell)
    paragraph = cell.paragraphs[0]
    paragraph.alignment = align
    paragraph.paragraph_format.space_after = Pt(0)
    paragraph.paragraph_format.line_spacing = 1.15
    run = paragraph.add_run(str(text))
    set_font(run, "Microsoft YaHei", size, bold=bold, color=color)


def add_table(doc, headers: list[str], rows: list[list[str]], widths: list[float]):
    table = doc.add_table(rows=1, cols=len(headers))
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_table_borders(table)
    header = table.rows[0]
    set_repeat_header(header)
    for index, title in enumerate(headers):
        set_cell_shading(header.cells[index], GREEN)
        cell_text(header.cells[index], title, bold=True, color=WHITE, size=9)
    for row_index, values in enumerate(rows):
        row = table.add_row()
        set_cant_split(row)
        fill = WHITE if row_index % 2 == 0 else PALE_GREEN
        for index, value in enumerate(values):
            set_cell_shading(row.cells[index], fill)
            align = WD_ALIGN_PARAGRAPH.CENTER if index > 1 else WD_ALIGN_PARAGRAPH.LEFT
            cell_text(row.cells[index], value, size=9, align=align)
    set_col_widths(table, widths)
    doc.add_paragraph().paragraph_format.space_after = Pt(0)
    return table


def add_body(doc, text: str, bold_lead: str | None = None):
    paragraph = doc.add_paragraph()
    paragraph.style = doc.styles["Body Text"]
    if bold_lead and text.startswith(bold_lead):
        first = paragraph.add_run(bold_lead)
        set_font(first, "宋体", 11, bold=True)
        remaining = paragraph.add_run(text[len(bold_lead):])
        set_font(remaining, "宋体", 11)
    else:
        run = paragraph.add_run(text)
        set_font(run, "宋体", 11)
    return paragraph


def add_bullet(doc, text: str):
    paragraph = doc.add_paragraph(style="List Bullet")
    paragraph.paragraph_format.left_indent = Inches(0.28)
    paragraph.paragraph_format.first_line_indent = Inches(-0.18)
    paragraph.paragraph_format.space_after = Pt(4)
    run = paragraph.add_run(text)
    set_font(run, "宋体", 10.5)


def add_heading(doc, text: str, level=1):
    paragraph = doc.add_paragraph(style=f"Heading {level}")
    paragraph.paragraph_format.keep_with_next = True
    run = paragraph.add_run(text)
    set_font(run, "Microsoft YaHei", 18 if level == 1 else 13.5, bold=True, color=INK)
    return paragraph


def add_figure(doc, path: Path, caption: str, width=6.15):
    if not path.exists():
        return
    paragraph = doc.add_paragraph()
    paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
    paragraph.paragraph_format.keep_with_next = True
    shape = paragraph.add_run().add_picture(str(path), width=Inches(width))
    doc_pr = shape._inline.docPr
    doc_pr.set("name", caption)
    doc_pr.set("descr", caption)
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.space_before = Pt(3)
    cap.paragraph_format.space_after = Pt(9)
    run = cap.add_run(caption)
    set_font(run, "Microsoft YaHei", 9, color=GRAY)


def add_code_block(doc, lines: list[str]):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(table, color=BORDER)
    cell = table.cell(0, 0)
    set_cell_shading(cell, "F5F7F5")
    set_cell_margins(cell, top=140, start=160, bottom=140, end=160)
    paragraph = cell.paragraphs[0]
    paragraph.paragraph_format.space_after = Pt(0)
    paragraph.paragraph_format.line_spacing = 1.25
    run = paragraph.add_run("\n".join(lines))
    set_font(run, "Consolas", 9.5, color="314038")
    doc.add_paragraph().paragraph_format.space_after = Pt(0)


def add_section_intro(doc, text: str):
    paragraph = add_body(doc, text)
    paragraph.paragraph_format.space_after = Pt(10)
    return paragraph


doc = Document()
doc.core_properties.title = "同标阅卷初代作品使用说明书"
doc.core_properties.subject = "同标阅卷微服务平台初代作品的部署、演示与验收说明"
doc.core_properties.author = "课程项目组"
section = doc.sections[0]
section.page_width = Inches(8.5)
section.page_height = Inches(11)
section.top_margin = Inches(0.72)
section.bottom_margin = Inches(0.7)
section.left_margin = Inches(0.78)
section.right_margin = Inches(0.78)
section.header_distance = Inches(0.35)
section.footer_distance = Inches(0.35)

styles = doc.styles
normal = styles["Normal"]
normal.font.name = "宋体"
normal.font.size = Pt(11)
normal.font.color.rgb = RGBColor.from_string(INK)
normal.paragraph_format.line_spacing = 1.45
normal.paragraph_format.space_after = Pt(6)
normal._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")

body_style = styles["Body Text"]
body_style.font.name = "宋体"
body_style.font.size = Pt(11)
body_style.font.color.rgb = RGBColor.from_string(INK)
body_style.paragraph_format.line_spacing = 1.5
body_style.paragraph_format.space_after = Pt(8)

for name, size, before, after in (("Heading 1", 18, 14, 8), ("Heading 2", 13.5, 12, 6)):
    style = styles[name]
    style.font.name = "Microsoft YaHei"
    style.font.size = Pt(size)
    style.font.bold = True
    style.font.color.rgb = RGBColor.from_string(INK)
    style.paragraph_format.space_before = Pt(before)
    style.paragraph_format.space_after = Pt(after)
    style.paragraph_format.keep_with_next = True

title_style = styles["Title"]
title_style.font.name = "Microsoft YaHei"
title_style.font.size = Pt(29)
title_style.font.bold = True
title_style.font.color.rgb = RGBColor.from_string(INK)
title_style.paragraph_format.space_before = Pt(0)
title_style.paragraph_format.space_after = Pt(7)

subtitle_style = styles["Subtitle"]
subtitle_style.font.name = "Microsoft YaHei"
subtitle_style.font.size = Pt(14)
subtitle_style.font.color.rgb = RGBColor.from_string(GREEN)
subtitle_style.font.italic = False
subtitle_style.paragraph_format.space_after = Pt(26)
remove_paragraph_borders(title_style)
remove_paragraph_borders(subtitle_style)

footer = section.footer
footer.paragraphs[0].alignment = WD_ALIGN_PARAGRAPH.CENTER
footer_run = footer.paragraphs[0].add_run("同标阅卷初代作品使用说明书")
set_font(footer_run, "Microsoft YaHei", 8.5, color=GRAY)

# Cover
for _ in range(4):
    doc.add_paragraph()
cover_kicker = doc.add_paragraph()
cover_kicker.alignment = WD_ALIGN_PARAGRAPH.LEFT
run = cover_kicker.add_run("TONGBIAO GRADING")
set_font(run, "Georgia", 10, bold=True, color=GREEN)

title = doc.add_paragraph(style="Title")
title.paragraph_format.space_before = Pt(18)
title.paragraph_format.space_after = Pt(7)
remove_paragraph_borders(title)
run = title.add_run("同标阅卷初代作品使用说明书")
set_font(run, "Microsoft YaHei", 29, bold=True, color=INK)

subtitle = doc.add_paragraph(style="Subtitle")
subtitle.paragraph_format.space_after = Pt(26)
run = subtitle.add_run("初代作品 1.0")
set_font(run, "Microsoft YaHei", 14, color=GREEN)

scope = doc.add_paragraph()
scope.paragraph_format.space_after = Pt(28)
scope.paragraph_format.line_spacing = 1.75
run = scope.add_run(
    "本说明书用于部署、启动和演示“同标阅卷”微服务平台初代作品，"
    "并说明文本答卷从规则冻结、证据初筛到教师复核、统计导出的完整操作路径。"
)
set_font(run, "宋体", 11.5, color="3E4E47")

meta = [
    ["作品名称", "同标阅卷微服务平台"],
    ["作品版本", "初代作品 1.0"],
    ["源码目录", r"1101作业\smartgrade-platform"],
    ["技术栈", "Java 17、Spring Boot 3.2、Gateway、OpenFeign、Vue 3"],
    ["编制日期", date(2026, 11, 10).strftime("%Y-%m-%d")],
    ["适用对象", "课程验收教师、项目演示人员、后续开发人员"],
]
meta_table = doc.add_table(rows=0, cols=2)
meta_table.alignment = WD_TABLE_ALIGNMENT.LEFT
set_table_borders(meta_table)
for index, pair in enumerate(meta):
    row = meta_table.add_row()
    set_cant_split(row)
    set_cell_shading(row.cells[0], LIGHT_GREEN)
    set_cell_shading(row.cells[1], WHITE if index % 2 == 0 else PALE_GREEN)
    cell_text(row.cells[0], pair[0], bold=True, size=9.5, color=GREEN)
    cell_text(row.cells[1], pair[1], size=9.5)
set_col_widths(meta_table, [1.25, 4.85])

doc.add_page_break()

# Contents
add_heading(doc, "目录", 1)
contents = [
    "1 文档说明",
    "2 作品概览",
    "3 系统架构与服务边界",
    "4 环境准备与启动",
    "5 完整演示流程",
    "6 功能说明",
    "7 接口、数据和错误约定",
    "8 常见问题排查",
    "9 验收结果",
    "10 当前边界与后续迭代",
    "11 交付清单",
]
for item in contents:
    paragraph = doc.add_paragraph()
    paragraph.paragraph_format.space_after = Pt(8)
    run = paragraph.add_run(item)
    set_font(run, "Microsoft YaHei", 11.5, color=INK)

add_heading(doc, "文档控制", 2)
add_table(
    doc,
    ["版本", "日期", "说明"],
    [["1.0", "2026-11-10", "初代作品源码、运行方式、演示流程和验收结果"]],
    [0.9, 1.25, 4.0],
)

doc.add_page_break()

# 1
add_heading(doc, "1 文档说明", 1)
add_section_intro(
    doc,
    "本说明书对应初代作品 1.0 的可运行源码与本地验收环境。读者可按“快速启动”直接进入系统，"
    "也可根据“完整演示流程”逐步完成一次阅卷闭环。文档同时记录当前能力边界，便于后续迭代时判断哪些模块可继续复用。"
)
add_heading(doc, "1.1 读者与目标", 2)
add_body(doc, "验收教师可以通过本文核对工程结构、启动结果和业务闭环；演示人员可以按步骤完成一轮主观题阅卷；开发人员可以从服务边界、接口约定和已知边界继续迭代。")
add_heading(doc, "1.2 阅读方式", 2)
add_body(doc, "首次运行先阅读第 4 章，再按第 5 章完成演示。排障时直接查阅第 8 章。评估当前作品完成度时，重点阅读第 2、3、9、10 章。")

# 2
add_heading(doc, "2 作品概览", 1)
add_section_intro(
    doc,
    "初代作品把主观题阅卷拆成七个领域服务和一个统一网关。系统生成可解释建议分与评语草稿，但不替教师作最终决定。"
)
add_heading(doc, "2.1 已实现闭环", 2)
flow_table = add_table(
    doc,
    ["阶段", "用户动作", "系统结果"],
    [
        ["规则", "录入评分细则", "解析要点、权重和分值，并冻结版本与 checksum"],
        ["导入", "上传 TXT 答卷", "识别重复、空白和损坏文件，生成学生编号"],
        ["初筛", "等待后台处理", "匹配关键词或同义表达，返回句级证据与字符偏移"],
        ["复核", "核对证据、生成评语、确认分数", "写入最终分、成绩快照、评语和审计记录"],
        ["统计", "查看漏点并导出", "显示平均分、要点未命中率，生成 CSV 或 XLSX"],
        ["通知", "查看站内消息", "记录完成、失败和复核确认结果"],
    ],
    [1.0, 2.1, 3.2],
)
add_body(doc, "上述流程已在 2026-11-09 的本机环境通过真实 HTTP 请求完整验证，不是静态页面演示。")

add_heading(doc, "2.2 核心设计", 2)
add_bullet(doc, "可解释初筛：每条命中证据保留原文句子、字符起止位置、匹配类型和置信度。")
add_bullet(doc, "版本冻结：评分规则确认后生成不可变快照，后续复核可以追溯使用的规则版本。")
add_bullet(doc, "人工兜底：最终分和最终评语必须由教师确认，机器结果只作为建议。")
add_bullet(doc, "统一边界：外部访问经 Gateway 进入；统一响应、异常和 traceId 约束所有普通接口。")
add_bullet(doc, "按域拆库：每个领域服务拥有独立 schema，业务客户端不能跨服务读取 Repository 或数据库。")

add_heading(doc, "2.3 演示账号", 2)
add_table(
    doc,
    ["账号", "密码", "角色", "用途"],
    [
        ["teacher1", "Teach2026", "教师", "创建批次、冻结规则、复核和导出"],
        ["assistant1", "Teach2026", "助教", "保留的助教演示账号"],
        ["admin", "Teach2026", "管理员", "保留的管理员演示账号"],
    ],
    [1.2, 1.0, 1.0, 3.0],
)

# 3
add_heading(doc, "3 系统架构与服务边界", 1)
add_section_intro(
    doc,
    "服务拆分围绕“规则、答卷、初筛、复核、统计、通知”六个业务域展开。Gateway 只负责入口和身份传递，不保存业务数据。"
)
add_table(
    doc,
    ["编号", "服务", "端口", "主要职责"],
    [
        ["S00", "smartgrade-gateway", "8080", "统一路由、JWT 校验、身份头和 traceId 透传"],
        ["C01", "smartgrade-user-service", "8081", "身份与教师偏好服务"],
        ["C02", "smartgrade-rubric-service", "8082", "评分规则服务"],
        ["C03", "smartgrade-answer-service", "8083", "答卷批次服务"],
        ["C04", "smartgrade-grading-service", "8084", "可解释初筛服务"],
        ["C05", "smartgrade-review-service", "8085", "复核定分服务"],
        ["C06", "smartgrade-report-service", "8086", "统计报告服务"],
        ["C07", "smartgrade-notification-service", "8087", "任务通知服务"],
    ],
    [0.55, 2.15, 0.7, 3.1],
)

add_heading(doc, "3.1 业务调用关系", 2)
add_body(
    doc,
    "答卷服务完成导入后，通过事件触发后台编排：读取当前冻结规则，调用初筛服务完成 OCR 适配和文本匹配，再调用复核服务写入待复核结果。复核服务通过用户 Client 读取评语偏好，并在确认分数后通过通知 Client 写入提醒。"
)
add_body(
    doc,
    "外部请求只进入 Gateway。领域服务端口用于本地调试，正式部署时不应暴露给浏览器。所有 Feign Client 明确连接超时、读取超时，并透传 X-Trace-Id。"
)

add_heading(doc, "3.2 数据边界", 2)
add_body(doc, "用户、规则、答卷、初筛任务、复核结果、导出记录和通知分别保存在独立 schema。跨服务协作只通过 HTTP Client 或事件，不允许直接访问其他服务的 Repository。复核服务的最终定分在本地短事务内同时写入复核记录、成绩快照、评语草稿和审计日志。")

# 4
add_heading(doc, "4 环境准备与启动", 1)
add_section_intro(
    doc,
    "当前开发机已经安装 JDK 17 和 Maven。二者放在 D 盘独立目录，启动脚本只在当前 PowerShell 进程中设置环境，不修改系统级 PATH。"
)
add_heading(doc, "4.1 已验证环境", 2)
add_table(
    doc,
    ["项目", "版本或位置"],
    [
        ["JDK 17", r"D:\SmartGradeTools\jdk17；OpenJDK 17.0.21 LTS"],
        ["Maven", r"D:\SmartGradeTools\maven；Apache Maven 3.9.9"],
        ["Node.js", r"F:\soft\node.exe；用于 Vue 3 前端"],
        ["浏览器", "Microsoft Edge；访问 http://127.0.0.1:5173"],
    ],
    [1.8, 4.8],
)

add_heading(doc, "4.2 一键启动", 2)
add_body(doc, "进入源码目录，双击 start.cmd。脚本会构建全部模块、启动 8 个后端进程和前端开发服务器，等待所有健康端点返回 UP 后自动打开浏览器。")
add_code_block(
    doc,
    [
        r"cd E:\西南大学\班级\课：微服务与分布式架构\1101作业\smartgrade-platform",
        r".\start.cmd",
    ],
)
add_body(doc, "启动过程大约需要 1 至 2 分钟。第一次构建依赖较多，后续启动会使用项目内 Maven 仓库，速度更快。")

add_heading(doc, "4.3 停止服务", 2)
add_body(doc, "双击 stop.cmd，或执行 PowerShell 命令。停止脚本会核对 PID、进程名和启动时间，避免 PID 被系统复用后误停其他程序。")
add_code_block(doc, [r".\stop.cmd"])

add_heading(doc, "4.4 单独构建和测试", 2)
add_code_block(
    doc,
    [
        r".\scripts\mvn17.cmd clean verify",
        r"cd .\smartgrade-web",
        r"npm install",
        r"npm run build",
    ],
)
add_body(doc, "默认模式使用内存 H2，每个服务独立建库并通过 Flyway 执行基线脚本。服务停止后数据会清空，适合可重复课堂演示。")

# 5
add_heading(doc, "5 完整演示流程", 1)
add_section_intro(
    doc,
    "本章按照验收路径给出实际操作。建议使用 samples 目录中的三份文本答卷，学生编号会直接从文件名生成。"
)

add_heading(doc, "5.1 登录", 2)
add_body(doc, "打开 http://127.0.0.1:5173，输入 teacher1 和 Teach2026，点击“进入工作台”。登录成功后，身份与教师偏好服务签发 JWT，后续普通接口均通过 Authorization 请求头访问。")
add_figure(doc, FIGURES / "login.png", "图 1 同标阅卷登录页")

add_heading(doc, "5.2 创建批次并冻结评分规则", 2)
steps = [
    "在“阅卷批次”区域展开“新建阅卷批次”，填写考试名称、题号、题目标题和满分。",
    "在“评分规则”文本框中按“要点名称：关键词1|关键词2”的格式录入细则。",
    "点击“创建并解析”。系统会生成可编辑的要点、关键词、权重和分值。",
    "确认权重合计为 100.00，然后点击“保存并冻结”。",
    "记录页面显示的规则版本和状态。冻结后，复核结果会绑定该版本，便于追溯。",
]
for text in steps:
    paragraph = doc.add_paragraph(style="List Number")
    paragraph.paragraph_format.left_indent = Inches(0.3)
    paragraph.paragraph_format.space_after = Pt(4)
    run = paragraph.add_run(text)
    set_font(run, "宋体", 10.5)

add_heading(doc, "5.3 导入答卷并等待初筛", 2)
add_body(doc, "在“导入答卷”区域选择 samples 目录中的 2026001.txt、2026002.txt、2026003.txt。点击“接收并开始初筛”。系统先完成文件接收和校验，再在后台调用规则、初筛和复核服务。")
add_body(doc, "进度面板中的“处理中”归零、“待复核”出现数量后，表示异步流程完成。初代作品的默认演示只对 UTF-8 文本执行真实规则匹配。")
add_figure(doc, FIGURES / "workspace-final.png", "图 2 阅卷工作台和规则状态")

add_heading(doc, "5.4 复核证据并确认最终分", 2)
add_body(doc, "进入“复核与定分”。左侧选择学生答卷，右侧查看原文、命中要点、证据句、字符区间和置信度。点击“生成评语草稿”，系统会读取当前教师的评语偏好并生成两条候选评语。")
add_body(doc, "教师修改最终分、复核说明和最终评语后，点击“确认定分并写入快照”。系统写入复核记录、成绩快照和审计编号。")

add_heading(doc, "5.4.1 复核检查项", 2)
add_table(
    doc,
    ["检查项", "通过标准"],
    [
        ["证据对应", "证据句确实出现在原答卷中，并对应题目要求的评分要点"],
        ["分值与要点", "建议分与命中要点一致，漏选或误判已由教师修正"],
        ["评语表达", "评语符合教师偏好，未包含教师设置的禁用表达"],
        ["最终确认", "最终分处于满分范围内，最终评语已由教师主动确认"],
    ],
    [1.6, 4.9],
)
add_figure(doc, FIGURES / "results.png", "图 3 证据化复核和教师定分")

add_heading(doc, "5.5 查看统计与导出", 2)
add_body(doc, "进入“统计与导出”，可查看已定分答卷平均分和每个评分要点的未命中数量、未命中率。选择 Excel XLSX 或 CSV 后生成文件，再点击下载。")
add_figure(doc, FIGURES / "reports.png", "图 4 平均分、漏点统计和导出")

# 6
add_heading(doc, "6 功能说明", 1)
add_heading(doc, "6.1 用户与评语偏好", 2)
add_body(doc, "用户服务提供登录、当前用户信息和教师评语偏好。偏好包括语气、长度、开头方式、常用表达和禁用表达。评语草稿生成时会读取偏好；禁用表达先从模板文本中移除，教师仍需确认最终文字。")
add_heading(doc, "6.2 评分规则与冻结版本", 2)
add_body(doc, "规则服务将细则解析为可编辑要点。每个要点保存类型、关键词、同义变体、权重和分值。冻结时校验权重合计，生成版本快照和 SHA-256 checksum。历史版本可按 rubricId 和 version 查询。")
add_heading(doc, "6.3 答卷批次与异步初筛", 2)
add_body(doc, "答卷服务按文件名生成学生编号，在同一导入任务内识别重复编号、空白文本和损坏文件。原文件保存在项目 .data/objects，元数据写入答卷 schema。导入提交后发布本地事件，后台编排调用下游服务。")
add_heading(doc, "6.4 可解释匹配", 2)
add_body(doc, "初筛服务先应用 OCR 适配器。文本答卷直接使用原文；图片和 PDF 当前返回明确失败原因。匹配阶段对关键词和同义表达做 NFKC 规范化，返回匹配类型、证据句、起止偏移和置信度。")
add_heading(doc, "6.5 复核定分与留痕", 2)
add_body(doc, "复核服务按教师隔离结果。最终分必须在 0 到满分之间。确认后，同一短事务写入 review_record、score_snapshot、comment_draft 和 audit_log。通知失败不会回滚教师已确认的分数。")
add_heading(doc, "6.6 统计与导出", 2)
add_body(doc, "报告服务调用复核服务内部汇总接口，计算已定分答卷的平均分和每个要点的未命中率。CSV 使用 UTF-8 BOM，Excel 使用 XLSX 工作簿；导出记录保留有效期和审计编号。")
add_heading(doc, "6.7 通知与 SSE", 2)
add_body(doc, "通知服务支持站内通知列表、已读操作和 text/event-stream 事件流。初筛完成、失败和复核确认会写入通知。SSE 事件包含类型、ID 和数据正文。")

# 7
add_heading(doc, "7 接口、数据和错误约定", 1)
add_heading(doc, "7.1 统一响应", 2)
add_body(doc, "普通 JSON 接口统一返回 code、message、data、traceId 和 timestamp。成功时 code 为 0；参数错误、未登录、越权、业务冲突和系统异常使用不同错误码段。")
add_code_block(
    doc,
    [
        "{",
        '  "code": 0,',
        '  "message": "ok",',
        '  "data": { "batchId": "B...", "status": "WAITING_REVIEW" },',
        '  "traceId": "602d2feb258a419abd3cfe68922a60a5",',
        '  "timestamp": "2026-11-09T12:12:16+08:00"',
        "}",
    ],
)

add_heading(doc, "7.2 主要接口分组", 2)
add_table(
    doc,
    ["前缀", "服务", "代表性操作"],
    [
        ["/api/auth", "用户服务", "登录、读取当前用户"],
        ["/api/teachers/me/comment-profile", "用户服务", "读取和更新教师评语偏好"],
        ["/api/batches", "答卷服务", "创建批次、导入答卷、查看进度和答卷"],
        ["/api/rubrics", "规则服务", "创建、解析、保存要点、冻结和查询版本"],
        ["/api/results", "复核服务", "查询结果、生成评语、确认最终分和状态"],
        ["/api/reports", "报告服务", "统计、生成导出和下载"],
        ["/api/notifications", "通知服务", "列表、已读和 SSE"],
    ],
    [2.0, 1.25, 3.1],
)

add_heading(doc, "7.3 数据表", 2)
add_body(doc, "初代作品保留数据库设计中的核心实体：teacher、teacher_comment_profile、batch、import_job、answer、rubric、rubric_point、point_variant、rubric_version_snapshot、grading_task、grading_result、hit_evidence、review_record、comment_draft、score_snapshot、export_record、notification 和 audit_log。每个服务的 V1 迁移脚本位于 smartgrade-*-service/src/main/resources/db/migration。")

# 8
add_heading(doc, "8 常见问题排查", 1)
add_heading(doc, "8.1 端口被占用", 2)
add_body(doc, "先执行 stop.cmd。若问题仍在，在 PowerShell 中执行 Get-NetTCPConnection -State Listen | Where-Object LocalPort -ge 8080 查看占用进程。不要直接结束未知 Java 或 Node 进程。")
add_heading(doc, "8.2 Java 启动后立即退出", 2)
add_body(doc, "检查 logs 目录中对应服务的 .log 和 .err.log。当前 Windows 环境需要参数 -Djdk.net.unixdomain.tmpdir，启动脚本已自动传入。若手工运行 JAR，也应复制该参数并将目录指向可写路径。")
add_heading(doc, "8.3 npm 提示 EPERM", 2)
add_body(doc, "说明 npm 缓存目录不可写。将 npm_config_cache 指向项目内 .npm-cache 后重试。本作品的前端安装已在项目内使用该方式完成。")
add_heading(doc, "8.4 初筛结果为 0 分", 2)
add_body(doc, "先核对冻结规则中的关键词是否与答卷文本一致。初筛只按规则匹配，不进行语义推理。教师应在复核页面查看证据并人工修正最终分。")
add_heading(doc, "8.5 上传图片或 PDF 后失败", 2)
add_body(doc, "这是当前能力边界。初代作品尚未配置真实 OCR 引擎，系统不会伪造识别结果。课堂演示请上传 samples 中的 TXT 文件；接入 OCR 后可在初筛服务替换适配器。")
add_heading(doc, "8.6 重启后数据消失", 2)
add_body(doc, "默认演示模式使用内存 H2。需要持久化时启用 docker-compose-infra.yml 中的 MySQL 和各服务的 infra profile。")

# 9
add_heading(doc, "9 验收结果", 1)
add_section_intro(doc, "以下结果来自本机 2026-11-09 的实际构建、单元测试和 HTTP 闭环验证。")
add_table(
    doc,
    ["验收项", "结果", "证据"],
    [
        ["Maven 聚合构建", "通过", "11 个 reactor 模块全部 SUCCESS"],
        ["自动化测试", "通过", "字段校验、证据匹配、双 Client 编排测试均为 0 失败"],
        ["8 个健康端点", "通过", "8080 至 8087 均返回 UP"],
        ["三份答卷闭环", "通过", "3 份进入待复核，无失败"],
        ["教师定分", "通过", "最终分 18、8、17，均生成快照和审计编号"],
        ["漏点统计", "通过", "平均分 14.33，三个要点未命中率均 33.33%"],
        ["XLSX 导出", "通过", "生成 COMPLETED 记录，下载文件 3560 字节"],
        ["前端视觉检查", "通过", "登录、工作台、复核、报告无控制台错误和版面溢出"],
    ],
    [1.8, 1.0, 3.9],
)

# 10
add_heading(doc, "10 当前边界与后续迭代", 1)
add_body(doc, "初代作品优先保证领域边界、可解释结果和教师确认闭环。以下能力已在源码或基础设施配置中预留，但不应在课程演示中描述为已完全上线。")
add_bullet(doc, "真实 OCR：当前没有图片文字识别引擎，图片和 PDF 会返回未配置原因。")
add_bullet(doc, "持久化：默认 H2 为内存模式；正式模式需要 Docker 中的 MySQL。")
add_bullet(doc, "消息总线：默认使用本地异步事件；Kafka 已在编排文件中提供，消费者适配尚未切换。")
add_bullet(doc, "对象存储：默认使用本地文件适配器；MinIO 已在编排文件中提供。")
add_bullet(doc, "评语生成：当前使用教师偏好和命中证据的模板生成，后续可接入大模型。")
add_bullet(doc, "分布式事务：跨服务长流程使用状态与幂等思路；Seata 的短事务适配可作为后续增强。")
add_body(doc, "建议下一阶段先接入真实 OCR 和持久化，再增加 Kafka 消费者与 MinIO 适配器，最后补充 Sentinel 限流规则、Seata 短事务和 SkyWalking 链路追踪。")

# 11
add_heading(doc, "11 交付清单", 1)
add_table(
    doc,
    ["交付内容", "位置"],
    [
        ["初代作品源码", r"1101作业\smartgrade-platform"],
        ["启动脚本", r"1101作业\smartgrade-platform\start.cmd"],
        ["停止脚本", r"1101作业\smartgrade-platform\stop.cmd"],
        ["项目说明", r"1101作业\smartgrade-platform\README.md"],
        ["示例答卷", r"1101作业\smartgrade-platform\samples"],
        ["Docker 基础设施", r"1101作业\smartgrade-platform\deploy\docker-compose-infra.yml"],
        ["使用说明书", r"1101作业\初代作品使用说明书.docx"],
    ],
    [1.6, 5.1],
)
add_body(doc, "源码包的交付不包含项目内 Maven 缓存、npm 缓存、编译产物和运行日志。首次构建会按 scripts/maven-settings.xml 使用阿里云镜像下载依赖。")

doc.save(OUTPUT)
print(OUTPUT)
