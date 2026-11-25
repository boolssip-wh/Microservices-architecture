from __future__ import annotations

import json
import math
import random
import re
from dataclasses import dataclass, field
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont


ROOT = Path(__file__).resolve().parent
WIDTH = 1721
HEIGHT = 2435
FONT_DIR = Path(r"C:\Windows\Fonts")
CONTENT_POINTS = {
    "有理想": ("有理想", "理想信念", "树立理想", "听党话", "跟党走", "坚定信念"),
    "敢担当": ("敢担当", "敢于担当", "勇于担当", "责任担当", "挺膺担当"),
    "能吃苦": ("能吃苦", "吃苦耐劳", "艰苦奋斗", "不怕困难", "迎难而上"),
    "肯奋斗": ("肯奋斗", "不懈奋斗", "拼搏奋斗", "接续奋斗", "努力奋斗"),
}


@dataclass(frozen=True)
class AnswerSpec:
    student_no: str
    font_file: str
    font_size: int
    seed: int
    ink_alpha: int
    text: str
    smudge: str = "none"
    crossouts: tuple[str, ...] = field(default_factory=tuple)
    margin_top: int = 250
    line_gap: int = 26


ANSWERS = (
    AnswerSpec(
        student_no="2026100",
        font_file="STKAITI.TTF",
        font_size=58,
        seed=100,
        ink_alpha=238,
        margin_top=230,
        line_gap=22,
        smudge="left",
        text=(
            "新时代好青年首先要树立远大理想。我要把个人成长融入强国建设与民族复兴，"
            "坚定听党话、跟党走的信念，在专业学习中树立清晰目标。作为入党积极分子，"
            "我应当敢担当，主动承担班级服务和志愿工作任务，遇到问题不推诿，在集体需要时站出来。"
            "学习和实践不会总是一帆风顺，我要保持艰苦奋斗的作风，面对困难不退缩，"
            "在持续训练中提升本领。肯奋斗不是一句口号，而是每天完成学习计划、参与社会实践，"
            "再复盘改进。我会把个人理想落实到具体行动中，以新时代好青年的标准严格要求自己，"
            "在服务同学和社会中接受锻炼，争取早日成为合格的共产党员。"
        ),
    ),
    AnswerSpec(
        student_no="2026111",
        font_file="STXINGKA.TTF",
        font_size=55,
        seed=111,
        ink_alpha=226,
        margin_top=300,
        line_gap=30,
        smudge="right",
        crossouts=("主动承担班级",),
        text=(
            "新时代青年首先要有理想。我要把个人成长融入强国建设与民族复兴，"
            "在专业学习中树立清晰目标，坚定听党话、跟党走的信念。"
            "青年也应当敢担当。作为入党积极分子，我应主动承担班级服务和志愿工作，"
            "遇到问题不推诿，在集体需要时站出来。学习和实践不会总是一帆风顺，"
            "我要保持艰苦奋斗的作风，面对困难不退缩，坚持每天复盘，"
            "用实际行动争取早日入党。"
        ),
    ),
    AnswerSpec(
        student_no="2026999",
        font_file="STFANGSO.TTF",
        font_size=50,
        seed=999,
        ink_alpha=243,
        margin_top=245,
        line_gap=28,
        smudge="none",
        crossouts=("努力奋斗",),
        text=(
            "争做新时代好青年，心中必须要有理想。我会把个人选择同国家发展联系起来，"
            "珍惜学习机会，树立能够长期坚持的目标。成长过程中难免会遇到困难，"
            "我要能吃苦，不因一次挫折就放弃，在课程学习、实验训练和集体活动中磨炼意志。"
            "青春最可贵的是肯奋斗，我会认真完成每天的学习计划，主动参加社会实践，"
            "不断查找自己的不足并及时改正，以更严格的标准要求自己，"
            "争取用实际行动向党组织靠拢。"
        ),
    ),
    AnswerSpec(
        student_no="2026104",
        font_file="AdobeKaitiStd-Regular.otf",
        font_size=52,
        seed=104,
        ink_alpha=230,
        margin_top=350,
        line_gap=31,
        smudge="bottom",
        text=(
            "青年要敢担当，把责任放在心上。作为班级干部，我应当主动承担集体任务，"
            "遇到同学有困难时及时帮助，不回避矛盾。成长的过程不会轻松，"
            "我要能吃苦，坚持锻炼身体和意志，在实验失败、课程压力较大时也不退缩。"
            "要肯奋斗，就要把目标落实到每天的行动中。我会认真完成课程任务，"
            "参加志愿服务，定期复盘不足，用持续努力提升自己。"
        ),
    ),
    AnswerSpec(
        student_no="2026105",
        font_file="STLITI.TTF",
        font_size=54,
        seed=105,
        ink_alpha=218,
        margin_top=225,
        line_gap=34,
        smudge="left",
        crossouts=("认真完成课程任务",),
        text=(
            "我希望成为一名有理想的新时代青年。理想不能只停留在口头上，"
            "而要落实在专业学习和服务集体中。青年还应敢担当，面对急难任务主动站出来，"
            "不把责任推给别人。吃苦耐劳同样重要，学习和实践不会总是一帆风顺，"
            "我要在困难面前保持韧性，积极向优秀同学学习，"
            "不断纠正自己的不足。"
        ),
    ),
    AnswerSpec(
        student_no="2026106",
        font_file="FZSTK.TTF",
        font_size=46,
        seed=106,
        ink_alpha=239,
        margin_top=210,
        line_gap=25,
        smudge="right",
        text=(
            "新时代好青年要有坚定的理想信念，把个人成长放在国家发展的大背景中去思考。"
            "我会认真学习专业知识，树立清晰而长远的目标，不盲目跟风。"
            "在班级和学院需要时，我要敢担当，主动承担任务，做到遇事不推诿、关键时刻站得出来。"
            "同时，我深知肯奋斗需要长期坚持。我会把大目标拆成每天可以完成的小任务，"
            "按计划学习、参加实践、复盘改进，面对困难不轻易放弃，"
            "以踏实行动提升能力，在服务同学的过程中锻炼责任意识，"
            "努力以合格党员的标准要求自己。"
        ),
    ),
    AnswerSpec(
        student_no="2026107",
        font_file="simkai.ttf",
        font_size=57,
        seed=107,
        ink_alpha=222,
        margin_top=280,
        line_gap=27,
        smudge="none",
        crossouts=("社会实践",),
        text=(
            "敢担当不是抽象的口号，而要体现在具体行动中。作为入党积极分子，"
            "我应当主动承担班级工作，积极参加志愿服务，遇到老师和同学需要帮助时不袖手旁观。"
            "面对学习中的困难和生活中的压力，我要能吃苦、能坚持，不因为一时失败就降低标准。"
            "我也会保持肯奋斗的姿态，把每天的时间安排好，定期检查计划完成情况，"
            "从实践中发现不足，用一点一滴的进步证明自己。"
        ),
    ),
    AnswerSpec(
        student_no="2026108",
        font_file="NotoSerifSC-VF.ttf",
        font_size=48,
        seed=108,
        ink_alpha=235,
        margin_top=240,
        line_gap=29,
        smudge="bottom",
        text=(
            "我要成为有理想、能够服务社会的青年。为此，我会坚定学习方向，"
            "把个人发展与国家需要结合起来。学习中难免会遇到难题，我要能吃苦，"
            "主动向老师请教，坚持完成每一项训练，而不是绕开困难。"
            "实现目标离不开肯奋斗，我准备参加社会实践和志愿服务，"
            "主动承担集体交给我的工作，在实践中学习沟通与协作。"
            "我会经常总结自己的不足，及时改正，保持长期努力的习惯。"
        ),
    ),
    AnswerSpec(
        student_no="2026109",
        font_file="AdobeFangsongStd-Regular.otf",
        font_size=50,
        seed=109,
        ink_alpha=232,
        margin_top=235,
        line_gap=24,
        smudge="right",
        text=(
            "新时代好青年要有理想，也要把理想落实到具体行动中。我将把专业学习同国家发展需要结合起来，"
            "树立清晰目标，坚定听党话、跟党走的信念，努力提升解决实际问题的能力。"
            "青年必须敢担当。面对班级和学院交办的任务，我会主动承担、认真完成，"
            "遇到矛盾不逃避，在集体需要时站得出来。成长不会一路顺利，我要能吃苦，"
            "主动面对实验失败、课程压力和训练难题，在挫折中总结经验，"
            "而不是抱怨环境。肯奋斗需要长期坚持，我会把目标拆分成具体计划，"
            "每天检查完成情况，参加志愿服务和社会实践，在服务他人中认识责任。"
            "我也会虚心接受老师同学的批评，及时改正不足，用行动争取早日加入党组织。"
        ),
    ),
    AnswerSpec(
        student_no="2026110",
        font_file="STXINWEI.TTF",
        font_size=56,
        seed=110,
        ink_alpha=215,
        margin_top=390,
        line_gap=36,
        smudge="left",
        crossouts=("每天认真",),
        text=(
            "作为新时代大学生，我会按时完成课程学习，积极参加校园活动，"
            "认真听取老师和同学的意见，合理安排自己的时间。"
            "在集体生活中遵守纪律、团结同学，遇到问题主动沟通。"
            "今后我会继续提升专业能力，做好每一件小事，以积极态度面对学习生活。"
        ),
    ),
)


def font_for(spec: AnswerSpec) -> ImageFont.FreeTypeFont:
    path = FONT_DIR / spec.font_file
    if not path.exists():
        raise FileNotFoundError(f"Required Chinese font not found: {path}")
    return ImageFont.truetype(str(path), spec.font_size)


def make_paper(rng: random.Random) -> Image.Image:
    image = Image.new("RGB", (WIDTH, HEIGHT), (252, 251, 247))
    pixels = image.load()
    for y in range(HEIGHT):
        for x in range(WIDTH):
            noise = rng.randint(-4, 4)
            vignette = int(4 * math.hypot((x - WIDTH / 2) / WIDTH, (y - HEIGHT / 2) / HEIGHT))
            value = max(238, min(255, 250 + noise - vignette))
            pixels[x, y] = (value, value, max(235, value - 2))

    fibers = Image.new("RGBA", image.size, (0, 0, 0, 0))
    fiber_draw = ImageDraw.Draw(fibers)
    for _ in range(1500):
        x = rng.randrange(30, WIDTH - 30)
        y = rng.randrange(30, HEIGHT - 30)
        length = rng.randrange(2, 12)
        tint = rng.randrange(180, 220)
        fiber_draw.line((x, y, x + length, y + rng.choice((-1, 0, 0, 1))),
                        fill=(tint, tint, tint, rng.randrange(12, 28)), width=1)
    fibers = fibers.filter(ImageFilter.GaussianBlur(0.3))
    return Image.alpha_composite(image.convert("RGBA"), fibers)


def wrap_handwriting(text: str, font: ImageFont.FreeTypeFont, max_width: int) -> list[str]:
    lines: list[str] = []
    current = ""
    for char in text:
        candidate = current + char
        if current and font.getlength(candidate) > max_width:
            lines.append(current)
            current = char
        else:
            current = candidate
    if current:
        lines.append(current)
    return lines


def draw_handwriting(image: Image.Image, spec: AnswerSpec) -> None:
    rng = random.Random(spec.seed)
    font = font_for(spec)
    margin_left = 205 + rng.randrange(-8, 9)
    margin_right = 185
    y = spec.margin_top
    max_width = WIDTH - margin_left - margin_right

    for line in wrap_handwriting(spec.text, font, max_width):
        x = margin_left + rng.randrange(0, 18)
        baseline_jitter = rng.randrange(-3, 4)
        for char in line:
            if not char.strip():
                x += max(8, font.size // 3)
                continue
            glyph_width = max(font.size, int(font.getlength(char)))
            glyph_height = font.size + 26
            glyph = Image.new("RGBA", (glyph_width + 20, glyph_height + 20), (0, 0, 0, 0))
            glyph_draw = ImageDraw.Draw(glyph)
            alpha = max(180, min(250, spec.ink_alpha + rng.randrange(-12, 13)))
            glyph_draw.text((10, 4), char, font=font, fill=(18, 18, 17, alpha))
            angle = rng.uniform(-1.4, 1.4)
            if rng.random() < 0.18:
                angle += rng.choice((-1, 1)) * rng.uniform(1.2, 2.4)
            glyph = glyph.rotate(angle, resample=Image.Resampling.BICUBIC, expand=False)
            px = x + rng.randrange(-2, 3)
            py = int(y + baseline_jitter + rng.randrange(-2, 3))
            image.alpha_composite(glyph, (max(0, px - 2), max(0, py - 2)))
            x += glyph_width + rng.randrange(-1, 3)
        y += spec.font_size + spec.line_gap + rng.randrange(-2, 3)
        if y > HEIGHT - 220:
            break

    if y > HEIGHT - 300:
        raise ValueError(f"{spec.student_no} content exceeds one page")


def draw_crossouts(image: Image.Image, spec: AnswerSpec) -> None:
    if not spec.crossouts:
        return
    rng = random.Random(spec.seed + 500)
    draw = ImageDraw.Draw(image, "RGBA")
    for index, phrase in enumerate(spec.crossouts):
        x = 300 + rng.randrange(0, 520)
        y = 620 + index * 440 + rng.randrange(-40, 40)
        width = int(spec.font_size * max(2.2, len(phrase) * 0.85))
        for stroke in range(2):
            points = []
            for point in range(7):
                points.append((
                    x + point * width / 6,
                    y + rng.randrange(-9, 10) + stroke * 5,
                ))
            draw.line(points, fill=(20, 20, 19, rng.randrange(150, 210)), width=3)


def draw_smudge(image: Image.Image, spec: AnswerSpec) -> None:
    if spec.smudge == "none":
        return
    rng = random.Random(spec.seed + 900)
    layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    if spec.smudge == "left":
        center = (110, 1160)
    elif spec.smudge == "right":
        center = (WIDTH - 95, 1040)
    else:
        center = (900, HEIGHT - 125)
    for _ in range(24):
        x = center[0] + rng.randrange(-42, 43)
        y = center[1] + rng.randrange(-70, 71)
        radius = rng.randrange(8, 28)
        draw.ellipse((x - radius, y - radius // 2, x + radius, y + radius // 2),
                     fill=(110, 108, 102, rng.randrange(3, 11)))
    layer = layer.filter(ImageFilter.GaussianBlur(8))
    image.alpha_composite(layer)


def normalized_char_count(text: str) -> int:
    return len(re.sub(r"\s+", "", text))


def expected_hits(text: str) -> list[str]:
    return [
        point
        for point, variants in CONTENT_POINTS.items()
        if any(variant in text for variant in variants)
    ]


def calculate_expected_score(text: str, max_score: float = 20, base_score: float = 10) -> float:
    hits = expected_hits(text)
    point_score = round((max_score - base_score) * 0.9 / 4, 2)
    coverage_score = round((max_score - base_score) * 0.1, 2)
    score = base_score + point_score * len(hits)
    if normalized_char_count(text) >= 240:
        score += coverage_score
    return round(score, 2)


def render(spec: AnswerSpec) -> dict[str, object]:
    image = make_paper(random.Random(spec.seed + 7))
    draw_handwriting(image, spec)
    draw_crossouts(image, spec)
    draw_smudge(image, spec)

    # Keep the result a photograph-like scan rather than a perfectly crisp digital document.
    image = image.filter(ImageFilter.GaussianBlur(0.28))
    out = ROOT / f"{spec.student_no}.jpg"
    image.convert("RGB").save(out, "JPEG", quality=92, subsampling=0, optimize=True)

    return {
        "studentNo": spec.student_no,
        "file": out.name,
        "font": spec.font_file,
        "characters": normalized_char_count(spec.text),
        "contentHits": expected_hits(spec.text),
        "contentHitCount": len(expected_hits(spec.text)),
        "lengthCoveragePassed": normalized_char_count(spec.text) >= 240,
        "expectedSuggestedScore": calculate_expected_score(spec.text),
        "answerText": spec.text,
    }


def main() -> None:
    if len(ANSWERS) != 10:
        raise ValueError("The demo set must contain exactly ten answer sheets")
    results = [render(spec) for spec in ANSWERS]
    counts = {
        "fourPoint": sum(item["contentHitCount"] == 4 for item in results),
        "threePoint": sum(item["contentHitCount"] == 3 for item in results),
        "zeroPoint": sum(item["contentHitCount"] == 0 for item in results),
    }
    manifest = {
        "size": [WIDTH, HEIGHT],
        "orientation": "portrait",
        "paper": "A4-style white scanned paper",
        "distribution": {
            "fourPoint": counts["fourPoint"] / len(results),
            "threePoint": counts["threePoint"] / len(results),
            "zeroPoint": counts["zeroPoint"] / len(results),
        },
        "answers": results,
    }
    (ROOT / "答卷清单.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    print(json.dumps(manifest["distribution"], ensure_ascii=False))
    for item in results:
        print(
            f"{item['file']}: {item['contentHitCount']}/4 hits, "
            f"{item['characters']} chars, expected {item['expectedSuggestedScore']}"
        )


if __name__ == "__main__":
    main()
