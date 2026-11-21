# 设计稿规格提取表（自动生成，勿手改）

> 由 `tools/pen.py` 从 `design/desing.pen` 直接解析。
> 设计画布：**468 × 860**，宽度属性多为 `fill_container`（流式）。
> 每一行都是代码数值的唯一出处；本文件里没有的值，代码里不允许出现。

## 设计变量

| token | 值 |
| --- | --- |
| `ink` | `#1A1A1A` |
| `ink2` | `#4B5563` |
| `ink3` | `#6B7280` |
| `line` | `#E5E7EB` |
| `board` | `#F3F4F6` |
| `card` | `#FFFFFF` |
| `brand` | `#0F766E` |
| `fcn` | `Inter` |

---

## P01-a　欢迎与隐私

> 首屏价值说明 → 隐私三件事 → 开始。隐私说明必须先于相机授权出现。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 1 | frame | 系统图标 | None×None | gap=4 |  |
| 2 | rectangle | 信号 | 3×7 | cornerRadius=1 fill=#0F1A24 |  |
| 2 | rectangle | 信号 | 3×10 | cornerRadius=1 fill=#0F1A24 |  |
| 2 | rectangle | 信号 | 3×13 | cornerRadius=1 fill=#0F1A24 |  |
| 2 | rectangle | 电池 | 22×11 | cornerRadius=3 stroke=#0F1A24 |  |
| 0 | frame | 内容区 | fill_container×fill_container | layout=vertical padding=26 |  |
| 1 | frame | 品牌行 | fill_container×None | gap=8 |  |
| 2 | ellipse | 点 | 9×9 | fill=#047857 |  |
| 2 | text | 品牌 | None×None | fontSize=10 fontWeight=600 fill=#047857 | HER RHYTHM　她的节律 |
| 1 | frame | 空隙1 | fill_container×44 |  |  |
| 1 | text | 主标题 | fill_container×None | fontSize=37 fontWeight=700 fill=#0F1A24 | 今天，我们一起 练习照顾 自己的身体。 |
| 1 | frame | 空隙2 | fill_container×18 |  |  |
| 1 | text | 副文案 | fill_container×None | fontSize=16 fontWeight=normal fill=#5C6B7A | 每天 3 分钟，跟着语音节律练习，屏幕会实时显示你的心率与呼吸。 |
| 1 | frame | 空隙3 | fill_container×32 |  |  |
| 1 | frame | 隐私卡 | fill_container×None | layout=vertical gap=16 padding=20 cornerRadius=16 fill=#F1F5F9 |  |
| 2 | text | 隐私标题 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 关于摄像头，三件事先讲清楚 |
| 2 | frame | 隐私行 画面只在手机本地处理 | fill_container×None | gap=12 |  |
| 3 | frame | 图标底 | 32×32 | layout=vertical cornerRadius=9 fill=#FFFFFF |  |
| 4 | icon | 图标 | 16×16 | fill=#047857 icon=shield-check |  |
| 3 | text | 文字 | fill_container×None | fontSize=15 fontWeight=600 fill=#1E293B | 画面只在手机本地处理 |
| 2 | frame | 隐私行 不录像，不留任何画面 | fill_container×None | gap=12 |  |
| 3 | frame | 图标底 | 32×32 | layout=vertical cornerRadius=9 fill=#FFFFFF |  |
| 4 | icon | 图标 | 16×16 | fill=#047857 icon=video-off |  |
| 3 | text | 文字 | fill_container×None | fontSize=15 fontWeight=600 fill=#1E293B | 不录像，不留任何画面 |
| 2 | frame | 隐私行 不上传，原始帧不出手机 | fill_container×None | gap=12 |  |
| 3 | frame | 图标底 | 32×32 | layout=vertical cornerRadius=9 fill=#FFFFFF |  |
| 4 | icon | 图标 | 16×16 | fill=#047857 icon=cloud-off |  |
| 3 | text | 文字 | fill_container×None | fontSize=15 fontWeight=600 fill=#1E293B | 不上传，原始帧不出手机 |
| 1 | frame | 空隙4 | fill_container×fill_container |  |  |
| 1 | frame | 主按钮 | fill_container×56 | gap=8 cornerRadius=28 fill=#0F1A24 |  |
| 2 | icon | 图标 | 17×17 | fill=#FFFFFF icon=arrow-right |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 我知道了，继续 |
| 1 | text | 脚注 | fill_container×None | fontSize=12 fontWeight=normal fill=#94A3B8 | 下一步需要开启相机权限，也可以稍后在设置里开启 |

---

## P01-b 安全筛查

> 3 题单选，不可预选；第 2 题选「是」会插入就医提示并常驻训练页。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航行 | fill_container×None | padding=[2, 22, 10, 22] |  |
| 1 | icon | 返回 | 22×22 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 进度 | None×None | fontSize=12 fontWeight=600 fill=#94A3B8 | 1 / 3 |
| 0 | frame | 进度条 | fill_container×4 | padding=[0, 22] |  |
| 1 | frame | 轨 | fill_container×4 | cornerRadius=2 fill=#E2E8F0 |  |
| 2 | rectangle | 进度 | 130×4 | cornerRadius=2 fill=#0F766E |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical padding=[26, 22, 22, 22] |  |
| 1 | text | 题干 | fill_container×None | fontSize=23 fontWeight=700 fill=#0F1A24 | 是否有医生或专业治疗师 指导过你做过盆底训练？ |
| 1 | frame | 空隙 | fill_container×8 |  |  |
| 1 | text | 辅助 | fill_container×None | fontSize=13 fontWeight=normal fill=#94A3B8 | 这会影响我们给你的建议强度，可以如实回答。 |
| 1 | frame | 空隙2 | fill_container×22 |  |  |
| 1 | frame | 选项 是，有专业指导 | fill_container×64 | gap=12 padding=[0, 18] cornerRadius=14 fill=#E7F6EC stroke=#047857 |  |
| 2 | ellipse | 选中 | 22×22 | fill=#047857 |  |
| 2 | text | 文字 | fill_container×None | fontSize=16 fontWeight=700 fill=#065F46 | 是，有专业指导 |
| 1 | frame | 选项 否，没有指导过 | fill_container×64 | gap=12 padding=[0, 18] cornerRadius=14 fill=#FFFFFF stroke=#CBD5E1 |  |
| 2 | ellipse | 未选 | 22×22 | stroke=#94A3B8 |  |
| 2 | text | 文字 | fill_container×None | fontSize=16 fontWeight=600 fill=#1E293B | 否，没有指导过 |
| 1 | frame | 选项 不确定 | fill_container×64 | gap=12 padding=[0, 18] cornerRadius=14 fill=#FFFFFF stroke=#CBD5E1 |  |
| 2 | ellipse | 未选 | 22×22 | stroke=#94A3B8 |  |
| 2 | text | 文字 | fill_container×None | fontSize=16 fontWeight=600 fill=#1E293B | 不确定 |
| 1 | frame | 空隙3 | fill_container×fill_container |  |  |
| 1 | frame | 主按钮 | fill_container×56 | cornerRadius=28 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 下一题 |
| 1 | text | 脚注 | fill_container×None | fontSize=12 fontWeight=normal fill=#94A3B8 | 选完后自动进入下一题，可随时返回修改 |

---

## P02-a　首页 · 未完成

> 今日训练卡为主行动；摄像头未就绪时在状态行先说明，不等点开始才报错。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=18 padding=[10, 22] |  |
| 1 | text | 问候 | fill_container×None | fontSize=16 fontWeight=normal fill=#5C6B7A | 早上好 |
| 1 | text | 主标题 | fill_container×None | fontSize=30 fontWeight=700 fill=#0F1A24 | 今天练一次吧 |
| 1 | frame | 今日训练卡 | fill_container×None | layout=vertical gap=14 padding=20 cornerRadius=20 fill=#0F1A24 |  |
| 2 | text | 卡标题 | None×None | fontSize=12 fontWeight=600 fill=#5EEAD4 | 今日训练 |
| 2 | text | 卡主文 | fill_container×None | fontSize=22 fontWeight=700 fill=#FFFFFF | 8 次 · 约 3 分钟 |
| 2 | text | 卡副文 | fill_container×None | fontSize=12 fontWeight=normal fill=#8FA3B8 | 准备 5s / 收缩 5s / 保持 5s / 放松 5s |
| 2 | frame | 卡按钮 | fill_container×52 | gap=8 cornerRadius=26 fill=#5EEAD4 |  |
| 3 | icon | 图标 | 17×17 | fill=#062530 icon=play |  |
| 3 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#062530 | 开始今天的训练 |
| 1 | frame | 状态行 | fill_container×None | gap=10 padding=[12, 14] cornerRadius=12 fill=#FEF3C7 |  |
| 2 | icon | 图标 | 16×16 | fill=#B45309 icon=video-off |  |
| 2 | text | 字 | fill_container×None | fontSize=13 fontWeight=600 fill=#B45309 | 训练前需开启摄像头 |
| 2 | text | 行动 | None×None | fontSize=13 fontWeight=700 fill=#B45309 | 去开启 |
| 1 | frame | 本周打卡 | fill_container×None | layout=vertical gap=12 padding=18 cornerRadius=16 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 行头 | fill_container×None |  |  |
| 3 | text | 标题 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 本周打卡 |
| 3 | text | 天数 | None×None | fontSize=12 fontWeight=600 fill=#0F766E | 3 / 7 天 |
| 2 | frame | 星期点 | fill_container×None |  |  |
| 3 | frame | 日 一 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#047857 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 一 |
| 3 | frame | 日 二 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#047857 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 二 |
| 3 | frame | 日 三 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#FFFFFF stroke=#CBD5E1 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 三 |
| 3 | frame | 日 四 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#047857 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 四 |
| 3 | frame | 日 五 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#FFFFFF stroke=#CBD5E1 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 五 |
| 3 | frame | 日 六 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#FFFFFF stroke=#CBD5E1 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 六 |
| 3 | frame | 日 日 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#FFFFFF stroke=#CBD5E1 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 日 |
| 1 | text | 上次训练 | fill_container×None | fontSize=13 fontWeight=normal fill=#64748B | 上次训练：昨天 · 3 分钟 |

---

## P02-b　首页 · 已完成

> 完成后卡内按钮降为次按钮，文案改为「可再练一次」，不隐藏入口。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=18 padding=[10, 22] |  |
| 1 | text | 问候 | fill_container×None | fontSize=16 fontWeight=normal fill=#5C6B7A | 晚上好 |
| 1 | text | 主标题 | fill_container×None | fontSize=30 fontWeight=700 fill=#0F1A24 | 今天已完成 |
| 1 | frame | 已完成卡 | fill_container×None | layout=vertical gap=14 padding=20 cornerRadius=20 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 完成行 | fill_container×None | gap=10 |  |
| 3 | ellipse | 勾底 | 28×28 | fill=#047857 |  |
| 3 | text | 文案 | fill_container×None | fontSize=16 fontWeight=700 fill=#0F1A24 | 今天已完成，可再练一次 |
| 2 | text | 数据 | fill_container×None | fontSize=13 fontWeight=normal fill=#64748B | 8 次　·　3 分 12 秒　·　平均心率 71 |
| 2 | frame | 次按钮 | fill_container×52 | gap=8 cornerRadius=26 stroke=#CBD5E1 |  |
| 3 | icon | 图标 | 16×16 | fill=#0F1A24 icon=rotate-ccw |  |
| 3 | text | 字 | None×None | fontSize=17 fontWeight=600 fill=#0F1A24 | 再练一次 |
| 1 | frame | 就绪行 | fill_container×None | gap=10 padding=[12, 14] cornerRadius=12 fill=#E7F6EC |  |
| 2 | icon | 图标 | 16×16 | fill=#047857 icon=video |  |
| 2 | text | 字 | fill_container×None | fontSize=13 fontWeight=600 fill=#047857 | 摄像头已就绪 |
| 1 | frame | 本周打卡 | fill_container×None | layout=vertical gap=12 padding=18 cornerRadius=16 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 行头 | fill_container×None |  |  |
| 3 | text | 标题 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 本周打卡 |
| 3 | text | 天数 | None×None | fontSize=12 fontWeight=600 fill=#0F766E | 4 / 7 天 |
| 2 | frame | 星期点 | fill_container×None |  |  |
| 3 | frame | 日 一 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#047857 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 一 |
| 3 | frame | 日 二 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#047857 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 二 |
| 3 | frame | 日 三 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#FFFFFF stroke=#CBD5E1 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 三 |
| 3 | frame | 日 四 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#047857 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 四 |
| 3 | frame | 日 五 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#FFFFFF stroke=#CBD5E1 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 五 |
| 3 | frame | 日 六 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#FFFFFF stroke=#CBD5E1 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 六 |
| 3 | frame | 日 日 | None×None | layout=vertical gap=6 |  |
| 4 | ellipse | 点 | 26×26 | fill=#047857 |  |
| 4 | text | 文字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 日 |

---

## P03-a　校准 · 建立信号中

> 进度环 + 秒数递增，每 1s 更新。开始按钮此时置灰，下方写明原因。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 视频区 | fill_container×430 | layout=none fill=#1B2A38 |  |
| 1 | rectangle | 真人预览 · 镜像 | 468×430 | fill={'type': 'image', 'enabled': True, 'url': 'generated.png', 'mode': 'cover', 'transform': [-1.25, 0, 0, 1.25, 1.12, -0.04]} |  |
| 1 | rectangle | 底部渐变 | 468×100 | fill={'type': 'gradient', 'gradientType': 'linear', 'enabled': True, 'rotation': 180, 'size': {'height': 1}, 'colors': [{'color': '#0F1A2400', 'position': 0}, {'color': '#0F1A24D9', 'position': 1}]} |  |
| 1 | frame | 人脸框 | 232×288 | layout=none cornerRadius=120 stroke=#FFFFFFB3 |  |
| 1 | frame | 相机角标 | 132×30 | gap=6 padding=[0, 11] cornerRadius=15 fill=#0F1A24B3 |  |
| 2 | ellipse | 点 | 7×7 | fill=#5EEAD4 |  |
| 2 | text | 字 | None×None | fontSize=11 fontWeight=600 fill=#FFFFFF | 摄像头工作中 |
| 1 | frame | 镜像标 | 86×30 | gap=5 padding=[0, 11] cornerRadius=15 fill=#0F1A24B3 |  |
| 2 | icon | 图标 | 12×12 | fill=#FFFFFF icon=flip-horizontal |  |
| 2 | text | 字 | None×None | fontSize=11 fontWeight=600 fill=#FFFFFF | 镜像 |
| 1 | text | 状态字 | 468×None | fontSize=15 fontWeight=700 fill=#FFFFFF | 正在建立信号 |
| 0 | frame | 下区 | fill_container×fill_container | layout=vertical gap=16 padding=22 |  |
| 1 | frame | 进度环 | fill_container×120 | layout=none |  |
| 2 | frame | 环 | 120×120 | layout=none |  |
| 3 | ellipse | 底环 | 120×120 | fill=#E2E8F0 |  |
| 3 | ellipse | 进度 | 120×120 | fill=#0F766E |  |
| 3 | frame | 中心 | 120×120 | layout=vertical |  |
| 4 | text | 秒 | None×None | fontSize=26 fontWeight=600 fill=#0F1A24 | 12s |
| 1 | text | 说明 | fill_container×None | fontSize=15 fontWeight=600 fill=#0F1A24 | 正在建立信号 12s / 约 30s |
| 1 | text | 提示 | fill_container×None | fontSize=13 fontWeight=normal fill=#64748B | 请保持不动，看向摄像头，正常呼吸 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 禁用按钮 | fill_container×56 | layout=vertical cornerRadius=28 fill=#E2E8F0 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#94A3B8 | 开始训练 |
| 1 | text | 原因 | fill_container×None | fontSize=12 fontWeight=normal fill=#B45309 | 信号建立中，约 30 秒后可开始 |

---

## P03-b　校准 · 人脸未对准

> 图像上罩暗层降低干扰，用琥珀框与大字提示，不报错音。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 视频区 | fill_container×430 | layout=none fill=#1B2A38 |  |
| 1 | rectangle | 真人预览 · 镜像 | 468×430 | fill={'type': 'image', 'enabled': True, 'url': 'generated.png', 'mode': 'cover', 'transform': [-1.25, 0, 0, 1.25, 1.12, -0.04]} |  |
| 1 | rectangle | 底部渐变 | 468×100 | fill={'type': 'gradient', 'gradientType': 'linear', 'enabled': True, 'rotation': 180, 'size': {'height': 1}, 'colors': [{'color': '#0F1A2400', 'position': 0}, {'color': '#0F1A24D9', 'position': 1}]} |  |
| 1 | frame | 人脸框 | 232×288 | layout=none cornerRadius=120 stroke=#F59E0B |  |
| 1 | rectangle | 暗罩 | 468×430 | fill=#0F1A2426 |  |
| 1 | frame | 相机角标 | 132×30 | gap=6 padding=[0, 11] cornerRadius=15 fill=#0F1A24B3 |  |
| 2 | ellipse | 点 | 7×7 | fill=#F59E0B |  |
| 2 | text | 字 | None×None | fontSize=11 fontWeight=600 fill=#FFFFFF | 摄像头工作中 |
| 1 | frame | 镜像标 | 86×30 | gap=5 padding=[0, 11] cornerRadius=15 fill=#0F1A24B3 |  |
| 2 | icon | 图标 | 12×12 | fill=#FFFFFF icon=flip-horizontal |  |
| 2 | text | 字 | None×None | fontSize=11 fontWeight=600 fill=#FFFFFF | 镜像 |
| 1 | text | 状态字 | 468×None | fontSize=15 fontWeight=700 fill=#FBBF24 | 请把脸放进框内 |
| 0 | frame | 下区 | fill_container×fill_container | layout=vertical gap=16 padding=22 |  |
| 1 | text | 主提示 | fill_container×None | fontSize=28 fontWeight=700 fill=#0F1A24 | 请把脸放进框内 |
| 1 | text | 辅助提示 | fill_container×None | fontSize=15 fontWeight=normal fill=#64748B | 手机放稳，脸正对镜头，光线从前面来 |
| 1 | frame | 三条提示 | fill_container×None | layout=vertical gap=10 padding=16 cornerRadius=14 fill=#F1F5F9 |  |
| 2 | frame | 提示 面向光源，不要背对窗 | fill_container×None | gap=10 |  |
| 3 | icon | 图标 | 15×15 | fill=#0F766E icon=sun |  |
| 3 | text | 字 | fill_container×None | fontSize=13 fontWeight=normal fill=#334155 | 面向光源，不要背对窗 |
| 2 | frame | 提示 脸占画面 20%–60% 最合适 | fill_container×None | gap=10 |  |
| 3 | icon | 图标 | 15×15 | fill=#0F766E icon=move-horizontal |  |
| 3 | text | 字 | fill_container×None | fontSize=13 fontWeight=normal fill=#334155 | 脸占画面 20%–60% 最合适 |
| 2 | frame | 提示 手机立在桌上，与脸齐高 | fill_container×None | gap=10 |  |
| 3 | icon | 图标 | 15×15 | fill=#0F766E icon=smartphone |  |
| 3 | text | 字 | fill_container×None | fontSize=13 fontWeight=normal fill=#334155 | 手机立在桌上，与脸齐高 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 禁用按钮 | fill_container×56 | layout=vertical cornerRadius=28 fill=#E2E8F0 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#94A3B8 | 开始训练 |
| 1 | text | 原因 | fill_container×None | fontSize=12 fontWeight=normal fill=#B45309 | 未检测到人脸，请先对准镜头 |

---

## P03-c　校准 · 就绪可开始

> 双条件（SDK 成功 + 人脸稳定≥1s）均满足，按钮才亮并变成强调色。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 视频区 | fill_container×430 | layout=none fill=#1B2A38 |  |
| 1 | rectangle | 真人预览 · 镜像 | 468×430 | fill={'type': 'image', 'enabled': True, 'url': 'generated.png', 'mode': 'cover', 'transform': [-1.25, 0, 0, 1.25, 1.12, -0.04]} |  |
| 1 | rectangle | 底部渐变 | 468×100 | fill={'type': 'gradient', 'gradientType': 'linear', 'enabled': True, 'rotation': 180, 'size': {'height': 1}, 'colors': [{'color': '#0F1A2400', 'position': 0}, {'color': '#0F1A24D9', 'position': 1}]} |  |
| 1 | frame | 人脸框 | 232×288 | layout=none cornerRadius=120 stroke=#5EEAD4 |  |
| 1 | frame | 相机角标 | 132×30 | gap=6 padding=[0, 11] cornerRadius=15 fill=#0F1A24B3 |  |
| 2 | ellipse | 点 | 7×7 | fill=#5EEAD4 |  |
| 2 | text | 字 | None×None | fontSize=11 fontWeight=600 fill=#FFFFFF | 摄像头工作中 |
| 1 | frame | 镜像标 | 86×30 | gap=5 padding=[0, 11] cornerRadius=15 fill=#0F1A24B3 |  |
| 2 | icon | 图标 | 12×12 | fill=#FFFFFF icon=flip-horizontal |  |
| 2 | text | 字 | None×None | fontSize=11 fontWeight=600 fill=#FFFFFF | 镜像 |
| 1 | text | 状态字 | 468×None | fontSize=15 fontWeight=700 fill=#5EEAD4 | 已对准　人脸占画面 38% |
| 0 | frame | 下区 | fill_container×fill_container | layout=vertical gap=16 padding=22 |  |
| 1 | text | 主提示 | fill_container×None | fontSize=28 fontWeight=700 fill=#0F1A24 | 可以开始了 |
| 1 | text | 辅助提示 | fill_container×None | fontSize=15 fontWeight=normal fill=#64748B | 信号已建立，现在开始能记录到完整数据 |
| 1 | frame | 设备诊断 | fill_container×None | layout=vertical gap=8 padding=[14, 16] cornerRadius=12 fill=#F8FAFC stroke=#E2E8F0 |  |
| 2 | frame | 诊断头 | fill_container×None |  |  |
| 3 | text | 标题 | None×None | fontSize=12 fontWeight=700 fill=#475569 | 设备诊断 |
| 3 | icon | 箭头 | 15×15 | fill=#94A3B8 icon=chevron-down |  |
| 2 | frame | 诊断 SDK 版本 | fill_container×None |  |  |
| 3 | text | k | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | SDK 版本 |
| 3 | text | v | None×None | fontSize=11 fontWeight=600 fill=#334155 | v1.5.0 |
| 2 | frame | 诊断 初始化 | fill_container×None |  |  |
| 3 | text | k | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 初始化 |
| 3 | text | v | None×None | fontSize=11 fontWeight=600 fill=#334155 | 成功 |
| 2 | frame | 诊断 License 剩余 | fill_container×None |  |  |
| 3 | text | k | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | License 剩余 |
| 3 | text | v | None×None | fontSize=11 fontWeight=600 fill=#334155 | 28 天 |
| 2 | frame | 诊断 当前 SQI | fill_container×None |  |  |
| 3 | text | k | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 当前 SQI |
| 3 | text | v | None×None | fontSize=11 fontWeight=600 fill=#334155 | 0.82 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 主按钮 | fill_container×56 | gap=8 cornerRadius=28 fill=#0F1A24 |  |
| 2 | icon | 图标 | 17×17 | fill=#FFFFFF icon=play |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 开始训练 |

---

## P04-a　训练中 · 运行

> 前 30s 只显示「信号建立中」；波形仅在 hasEnoughWindow 后绘。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 视频区 | fill_container×300 | layout=none fill=#1B2A38 |  |
| 1 | rectangle | 真人预览 · 镜像 | 468×300 | fill={'type': 'image', 'enabled': True, 'url': 'generated.png', 'mode': 'cover', 'transform': [-1.24, 0, 0, 1.24, 1.12, -0.16]} |  |
| 1 | rectangle | 底部渐变 | 468×100 | fill={'type': 'gradient', 'gradientType': 'linear', 'enabled': True, 'rotation': 180, 'size': {'height': 1}, 'colors': [{'color': '#0F1A2400', 'position': 0}, {'color': '#0F1A24E6', 'position': 1}]} |  |
| 1 | frame | 人脸框 | 184×228 | layout=none cornerRadius=96 stroke=#5EEAD4 |  |
| 1 | frame | 相机角标 | 130×28 | gap=6 padding=[0, 10] cornerRadius=14 fill=#0F1A24B3 |  |
| 2 | ellipse | 点 | 6×6 | fill=#5EEAD4 |  |
| 2 | text | 字 | None×None | fontSize=10 fontWeight=600 fill=#FFFFFF | 摄像头工作中 |
| 1 | text | 对准字 | 468×None | fontSize=12 fontWeight=600 fill=#FFFFFF | 已对准　肩颈放松 |
| 0 | frame | 阶段时间轴 | fill_container×None | layout=vertical padding=[14, 20] fill=#0F1A24 |  |
| 1 | text | 标题 | fill_container×None | fontSize=10 fontWeight=600 fill=#5E7386 | 阶段时间轴　PHASE TIMELINE |
| 1 | frame | 步骤行 | fill_container×None | padding=[10, 0, 0, 0] |  |
| 2 | frame | 步骤 准备 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 18×18 | fill=#047857 |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#C6D5E2 | 准备 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 收缩 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 18×18 | fill=#047857 |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#C6D5E2 | 收缩 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 保持 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 22×22 | fill=#5EEAD4 |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=700 fill=#5EEAD4 | 保持 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5EEAD4 | 00:05 |
| 2 | frame | 步骤 放松 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 16×16 | fill=#1B2A38 stroke=#3A4D5E |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#6E8598 | 放松 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 第 4 次 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 16×16 | fill=#1B2A38 stroke=#3A4D5E |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#6E8598 | 第 4 次 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | — |
| 0 | frame | 下半 | fill_container×fill_container | layout=vertical padding=[14, 20, 16, 20] |  |
| 1 | frame | 当前阶段头 | fill_container×None |  |  |
| 2 | frame | 左 | None×None | layout=vertical gap=3 |  |
| 3 | text | 标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 当前阶段　CURRENT |
| 3 | frame | 行 | None×None | gap=10 |  |
| 4 | text | 阶段 | None×None | fontSize=32 fontWeight=700 fill=#0F1A24 | 保持 |
| 3 | text | 说明 | None×None | fontSize=14 fontWeight=normal fill=#5C6B7A | 保持住，自然呼吸 |
| 2 | frame | 右 | None×None | gap=6 |  |
| 3 | text | 秒 | None×None | fontSize=34 fontWeight=600 fill=#0F1A24 | 00:03 |
| 3 | text | 总 | None×None | fontSize=12 fontWeight=normal fill=#94A3B8 | / 00:05 |
| 1 | frame | 间距 | fill_container×12 |  |  |
| 1 | frame | 进度轨 | fill_container×4 | cornerRadius=2 fill=#E2E8F0 |  |
| 2 | rectangle | 进度 | 170×4 | cornerRadius=2 fill=#0F766E |  |
| 1 | frame | 间距2 | fill_container×14 |  |  |
| 1 | frame | 波形卡 HR　心率　bpm | fill_container×104 | layout=none cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | text | 通道标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | HR　心率　bpm |
| 2 | text | 值 | None×None | fontSize=28 fontWeight=600 fill=#E11D48 | 72 |
| 2 | path | 曲线 | 404×64 | fill=#00000000 stroke=#E11D48 |  |
| 1 | frame | 间距3 | fill_container×10 |  |  |
| 1 | frame | 波形卡 RR　呼吸率　次/分 | fill_container×104 | layout=none cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | text | 通道标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | RR　呼吸率　次/分 |
| 2 | text | 值 | None×None | fontSize=28 fontWeight=600 fill=#2563EB | 16 |
| 2 | path | 曲线 | 404×64 | fill=#00000000 stroke=#2563EB |  |
| 1 | frame | 间距3 | fill_container×10 |  |  |
| 1 | text | 免责 | fill_container×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 曲线与数值反映身体状态，不代表盆底肌收缩质量 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 按钮行 | fill_container×None | gap=10 |  |
| 2 | frame | 暂停按钮 | fill_container×50 | gap=7 cornerRadius=25 stroke=#CBD5E1 |  |
| 3 | icon | 图标 | 15×15 | fill=#0F1A24 icon=pause |  |
| 3 | text | 字 | None×None | fontSize=16 fontWeight=600 fill=#0F1A24 | 暂停 |
| 2 | frame | 结束按钮 | fill_container×50 | gap=7 cornerRadius=25 fill=#0F1A24 |  |
| 3 | icon | 图标 | 13×13 | fill=#FFFFFF icon=square |  |
| 3 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 结束 |

---

## P04-b　训练中 · 暂停

> 冻结秒数与波形，波形不再右移；相机继续投喂以保持窗口。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 视频区 | fill_container×300 | layout=none fill=#1B2A38 |  |
| 1 | rectangle | 真人预览 · 镜像 | 468×300 | fill={'type': 'image', 'enabled': True, 'url': 'generated.png', 'mode': 'cover', 'transform': [-1.24, 0, 0, 1.24, 1.12, -0.16]} |  |
| 1 | rectangle | 底部渐变 | 468×100 | fill={'type': 'gradient', 'gradientType': 'linear', 'enabled': True, 'rotation': 180, 'size': {'height': 1}, 'colors': [{'color': '#0F1A2400', 'position': 0}, {'color': '#0F1A24E6', 'position': 1}]} |  |
| 1 | frame | 人脸框 | 184×228 | layout=none cornerRadius=96 stroke=#FFFFFF66 |  |
| 1 | frame | 相机角标 | 130×28 | gap=6 padding=[0, 10] cornerRadius=14 fill=#0F1A24B3 |  |
| 2 | ellipse | 点 | 6×6 | fill=#94A3B8 |  |
| 2 | text | 字 | None×None | fontSize=10 fontWeight=600 fill=#FFFFFF | 感知已暂停 |
| 1 | text | 对准字 | 468×None | fontSize=12 fontWeight=600 fill=#FFFFFF | 已对准　肩颈放松 |
| 0 | frame | 阶段时间轴 | fill_container×None | layout=vertical padding=[14, 20] fill=#0F1A24 |  |
| 1 | text | 标题 | fill_container×None | fontSize=10 fontWeight=600 fill=#5E7386 | 阶段时间轴　PHASE TIMELINE |
| 1 | frame | 步骤行 | fill_container×None | padding=[10, 0, 0, 0] |  |
| 2 | frame | 步骤 准备 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 18×18 | fill=#047857 |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#C6D5E2 | 准备 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 收缩 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 18×18 | fill=#047857 |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#C6D5E2 | 收缩 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 保持 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 22×22 | fill=#5EEAD4 |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=700 fill=#5EEAD4 | 保持 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5EEAD4 | 00:05 |
| 2 | frame | 步骤 放松 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 16×16 | fill=#1B2A38 stroke=#3A4D5E |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#6E8598 | 放松 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 第 4 次 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 16×16 | fill=#1B2A38 stroke=#3A4D5E |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#6E8598 | 第 4 次 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | — |
| 0 | frame | 下半 | fill_container×fill_container | layout=vertical padding=[14, 20, 16, 20] |  |
| 1 | frame | 当前阶段头 | fill_container×None |  |  |
| 2 | frame | 左 | None×None | layout=vertical gap=3 |  |
| 3 | text | 标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 当前阶段　CURRENT |
| 3 | text | 阶段 | None×None | fontSize=32 fontWeight=700 fill=#0F1A24 | 已暂停 |
| 3 | text | 说明 | None×None | fontSize=14 fontWeight=normal fill=#5C6B7A | 点击继续回到当前阶段 |
| 2 | frame | 右 | None×None | gap=6 |  |
| 3 | text | 秒 | None×None | fontSize=34 fontWeight=600 fill=#0F1A24 | 00:03 |
| 3 | text | 总 | None×None | fontSize=12 fontWeight=normal fill=#94A3B8 | / 00:05 |
| 1 | frame | 间距 | fill_container×12 |  |  |
| 1 | frame | 进度轨 | fill_container×4 | cornerRadius=2 fill=#E2E8F0 |  |
| 2 | rectangle | 进度 | 170×4 | cornerRadius=2 fill=#0F766E |  |
| 1 | frame | 间距2 | fill_container×14 |  |  |
| 1 | frame | 波形卡 HR　心率　bpm | fill_container×104 | layout=none cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | text | 通道标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | HR　心率　bpm |
| 2 | text | 值 | None×None | fontSize=28 fontWeight=600 fill=#E11D48 | 72 |
| 2 | path | 曲线 | 404×64 | fill=#00000000 stroke=#E11D48 |  |
| 1 | frame | 间距3 | fill_container×10 |  |  |
| 1 | frame | 波形卡 RR　呼吸率　次/分 | fill_container×104 | layout=none cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | text | 通道标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | RR　呼吸率　次/分 |
| 2 | text | 值 | None×None | fontSize=28 fontWeight=600 fill=#2563EB | 16 |
| 2 | path | 曲线 | 404×64 | fill=#00000000 stroke=#2563EB |  |
| 1 | frame | 间距3 | fill_container×10 |  |  |
| 1 | text | 免责 | fill_container×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 曲线与数值反映身体状态，不代表盆底肌收缩质量 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 按钮行 | fill_container×None | gap=10 |  |
| 2 | frame | 暂停按钮 | fill_container×50 | gap=7 cornerRadius=25 stroke=#CBD5E1 |  |
| 3 | icon | 图标 | 15×15 | fill=#0F1A24 icon=play |  |
| 3 | text | 字 | None×None | fontSize=16 fontWeight=600 fill=#0F1A24 | 继续 |
| 2 | frame | 结束按钮 | fill_container×50 | gap=7 cornerRadius=25 fill=#0F1A24 |  |
| 3 | icon | 图标 | 13×13 | fill=#FFFFFF icon=square |  |
| 3 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 结束 |

---

## P04-c　训练中 · 建信号

> 前 30s 指标区只显示建立中，不出现任何数字或曲线。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 视频区 | fill_container×300 | layout=none fill=#1B2A38 |  |
| 1 | rectangle | 真人预览 · 镜像 | 468×300 | fill={'type': 'image', 'enabled': True, 'url': 'generated.png', 'mode': 'cover', 'transform': [-1.24, 0, 0, 1.24, 1.12, -0.16]} |  |
| 1 | rectangle | 底部渐变 | 468×100 | fill={'type': 'gradient', 'gradientType': 'linear', 'enabled': True, 'rotation': 180, 'size': {'height': 1}, 'colors': [{'color': '#0F1A2400', 'position': 0}, {'color': '#0F1A24E6', 'position': 1}]} |  |
| 1 | frame | 人脸框 | 184×228 | layout=none cornerRadius=96 stroke=#5EEAD4 |  |
| 1 | frame | 相机角标 | 130×28 | gap=6 padding=[0, 10] cornerRadius=14 fill=#0F1A24B3 |  |
| 2 | ellipse | 点 | 6×6 | fill=#5EEAD4 |  |
| 2 | text | 字 | None×None | fontSize=10 fontWeight=600 fill=#FFFFFF | 摄像头工作中 |
| 1 | text | 对准字 | 468×None | fontSize=12 fontWeight=600 fill=#FFFFFF | 已对准　肩颈放松 |
| 0 | frame | 阶段时间轴 | fill_container×None | layout=vertical padding=[14, 20] fill=#0F1A24 |  |
| 1 | text | 标题 | fill_container×None | fontSize=10 fontWeight=600 fill=#5E7386 | 阶段时间轴　PHASE TIMELINE |
| 1 | frame | 步骤行 | fill_container×None | padding=[10, 0, 0, 0] |  |
| 2 | frame | 步骤 准备 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 22×22 | fill=#5EEAD4 |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=700 fill=#5EEAD4 | 准备 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5EEAD4 | 00:05 |
| 2 | frame | 步骤 收缩 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 16×16 | fill=#1B2A38 stroke=#3A4D5E |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#6E8598 | 收缩 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 保持 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 16×16 | fill=#1B2A38 stroke=#3A4D5E |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#6E8598 | 保持 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 放松 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 16×16 | fill=#1B2A38 stroke=#3A4D5E |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#6E8598 | 放松 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | 00:05 |
| 2 | frame | 步骤 第 4 次 | fill_container×None | layout=vertical gap=5 |  |
| 3 | ellipse | 点 | 16×16 | fill=#1B2A38 stroke=#3A4D5E |  |
| 3 | text | 名 | None×None | fontSize=11 fontWeight=600 fill=#6E8598 | 第 4 次 |
| 3 | text | 时长 | None×None | fontSize=9 fontWeight=normal fill=#5F7285 | — |
| 0 | frame | 下半 | fill_container×fill_container | layout=vertical padding=[14, 20, 16, 20] |  |
| 1 | frame | 当前阶段头 | fill_container×None |  |  |
| 2 | frame | 左 | None×None | layout=vertical gap=3 |  |
| 3 | text | 标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 当前阶段　CURRENT |
| 3 | text | 阶段 | None×None | fontSize=32 fontWeight=700 fill=#0F1A24 | 准备 |
| 3 | text | 说明 | None×None | fontSize=14 fontWeight=normal fill=#5C6B7A | 坐稳，放松肩膀和腹部 |
| 2 | frame | 右 | None×None | gap=6 |  |
| 3 | text | 秒 | None×None | fontSize=34 fontWeight=600 fill=#0F1A24 | 00:03 |
| 3 | text | 总 | None×None | fontSize=12 fontWeight=normal fill=#94A3B8 | / 00:05 |
| 1 | frame | 间距 | fill_container×12 |  |  |
| 1 | frame | 进度轨 | fill_container×4 | cornerRadius=2 fill=#E2E8F0 |  |
| 2 | rectangle | 进度 | 170×4 | cornerRadius=2 fill=#0F766E |  |
| 1 | frame | 间距2 | fill_container×14 |  |  |
| 1 | text | 免责 | fill_container×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 曲线与数值反映身体状态，不代表盆底肌收缩质量 |
| 1 | frame | 骨架卡 HR　心率　bpm | fill_container×104 | layout=none cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | text | 通道标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | HR　心率　bpm |
| 2 | text | 占位值 | None×None | fontSize=28 fontWeight=600 fill=#CBD5E1 | -- |
| 2 | frame | 基线占位 | 404×1 | layout=none |  |
| 3 | rectangle | 虚线底 | 404×1 | fill=#E2E8F0 |  |
| 2 | text | 等待字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 信号建立中 |
| 1 | frame | 间距 | fill_container×10 |  |  |
| 1 | frame | 骨架卡 RR　呼吸率　次/分 | fill_container×104 | layout=none cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 428×1 | fill=#F1F5F9 |  |
| 2 | text | 通道标 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | RR　呼吸率　次/分 |
| 2 | text | 占位值 | None×None | fontSize=28 fontWeight=600 fill=#CBD5E1 | -- |
| 2 | frame | 基线占位 | 404×1 | layout=none |  |
| 3 | rectangle | 虚线底 | 404×1 | fill=#E2E8F0 |  |
| 2 | text | 等待字 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 信号建立中 |
| 1 | frame | 间距 | fill_container×10 |  |  |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 按钮行 | fill_container×None | gap=10 |  |
| 2 | frame | 暂停按钮 | fill_container×50 | gap=7 cornerRadius=25 stroke=#CBD5E1 |  |
| 3 | icon | 图标 | 15×15 | fill=#0F1A24 icon=pause |  |
| 3 | text | 字 | None×None | fontSize=16 fontWeight=600 fill=#0F1A24 | 暂停 |
| 2 | frame | 结束按钮 | fill_container×50 | gap=7 cornerRadius=25 fill=#0F1A24 |  |
| 3 | icon | 图标 | 13×13 | fill=#FFFFFF icon=square |  |
| 3 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 结束 |

---

## P05-a　完成与自评

> 不给疗效结论。覆盖率<60% 时平均心率整行不占位。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[16, 22, 22, 22] |  |
| 1 | ellipse | 完成图标底 | 56×56 | fill=#E7F6EC |  |
| 1 | text | 标题 | fill_container×None | fontSize=26 fontWeight=700 fill=#0F1A24 | 完成今天的练习 |
| 1 | text | 副文 | fill_container×None | fontSize=14 fontWeight=normal fill=#64748B | 坚持下来就是进步，明天同一时间再来一次。 |
| 1 | frame | 数据区 | fill_container×None | gap=10 |  |
| 2 | frame | 数据 引导时长 | fill_container×86 | layout=vertical gap=2 cornerRadius=14 fill=#F8FAFC stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=11 fontWeight=600 fill=#94A3B8 | 引导时长 |
| 3 | text | 值 | None×None | fontSize=24 fontWeight=600 fill=#0F1A24 | 3:12 |
| 2 | frame | 数据 完成次数 | fill_container×86 | layout=vertical gap=2 cornerRadius=14 fill=#F8FAFC stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=11 fontWeight=600 fill=#94A3B8 | 完成次数 |
| 3 | text | 值 | None×None | fontSize=24 fontWeight=600 fill=#0F1A24 | 8 |
| 2 | frame | 数据 暂停 | fill_container×86 | layout=vertical gap=2 cornerRadius=14 fill=#F8FAFC stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=11 fontWeight=600 fill=#94A3B8 | 暂停 |
| 3 | text | 值 | None×None | fontSize=24 fontWeight=600 fill=#0F1A24 | 1 次 |
| 1 | frame | 覆盖率卡 | fill_container×None | gap=12 padding=16 cornerRadius=14 fill=#E7F6EC |  |
| 2 | frame | 左 | None×None | layout=vertical gap=2 |  |
| 3 | text | 标签 | None×None | fontSize=12 fontWeight=600 fill=#047857 | 本次信号覆盖率 |
| 3 | text | 说明 | None×None | fontSize=11 fontWeight=normal fill=#047857CC | 数据完整，平均心率 71 bpm |
| 2 | text | 值 | None×None | fontSize=26 fontWeight=600 fill=#047857 | 96% |
| 1 | text | 难度标题 | fill_container×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 今天这个强度感觉如何？ |
| 1 | frame | 难度选项 | fill_container×None | gap=10 |  |
| 2 | frame | 难度 轻松 | fill_container×50 | layout=vertical cornerRadius=25 fill=#FFFFFF stroke=#CBD5E1 |  |
| 3 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#334155 | 轻松 |
| 2 | frame | 难度 合适 | fill_container×50 | layout=vertical cornerRadius=25 fill=#0F1A24 stroke=#0F1A24 |  |
| 3 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#FFFFFF | 合适 |
| 2 | frame | 难度 困难 | fill_container×50 | layout=vertical cornerRadius=25 fill=#FFFFFF stroke=#CBD5E1 |  |
| 3 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#334155 | 困难 |
| 1 | text | 不适标题 | fill_container×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 练习中有没有不舒服？ |
| 1 | frame | 不适选项 | fill_container×None | gap=10 |  |
| 2 | frame | 不适 没有不适 | fill_container×50 | layout=vertical cornerRadius=25 fill=#E7F6EC stroke=#047857 |  |
| 3 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#065F46 | 没有不适 |
| 2 | frame | 不适 有点不适 | fill_container×50 | layout=vertical cornerRadius=25 fill=#FFFFFF stroke=#CBD5E1 |  |
| 3 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#334155 | 有点不适 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 保存按钮 | fill_container×56 | layout=vertical cornerRadius=28 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 保存并返回首页 |

---

## P05-b　自评 · 选择有不适

> 选「有不适」立即插入就医指引，不出现任何加量建议。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[16, 22, 22, 22] |  |
| 1 | ellipse | 提醒底 | 56×56 | fill=#FEF3C7 |  |
| 1 | text | 标题 | fill_container×None | fontSize=26 fontWeight=700 fill=#0F1A24 | 先暂停练习 |
| 1 | text | 副文 | fill_container×None | fontSize=15 fontWeight=normal fill=#475569 | 你提到练习中有不舒服。这种情况建议先停止，不要继续加量。 |
| 1 | frame | 就医提示卡 | fill_container×None | layout=vertical gap=10 padding=18 cornerRadius=14 fill=#FEF3C7 |  |
| 2 | text | 卡标题 | fill_container×None | fontSize=15 fontWeight=700 fill=#92400E | 建议联系专业医生或盆底康复治疗师 |
| 2 | text | 卡正文 | fill_container×None | fontSize=13 fontWeight=normal fill=#92400ED9 | 如果出现疼痛、血尿、下坠感加重，请尽快就医。本应用不提供诊断，也不建议自行加重训练。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 主按钮 | fill_container×56 | layout=vertical cornerRadius=28 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 我知道了 |
| 1 | frame | 次按钮 | fill_container×52 | gap=8 cornerRadius=26 stroke=#CBD5E1 |  |
| 2 | icon | 图标 | 16×16 | fill=#0F1A24 icon=phone |  |
| 2 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#0F1A24 | 拨打 12356 心理援助热线 |

---

## P06-a　记录与 7 天

> 汇总值与列表逐条相加必须相等（AC-12）。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[10, 22] |  |
| 1 | text | 大标题 | fill_container×None | fontSize=28 fontWeight=700 fill=#0F1A24 | 训练记录 |
| 1 | frame | 汇总 | fill_container×None | gap=10 |  |
| 2 | frame | 汇总 7 天完成 | fill_container×82 | layout=vertical gap=2 cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=11 fontWeight=600 fill=#94A3B8 | 7 天完成 |
| 3 | text | 值 | None×None | fontSize=22 fontWeight=600 fill=#0F1A24 | 4 天 |
| 2 | frame | 汇总 累计时长 | fill_container×82 | layout=vertical gap=2 cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=11 fontWeight=600 fill=#94A3B8 | 累计时长 |
| 3 | text | 值 | None×None | fontSize=22 fontWeight=600 fill=#0F1A24 | 12:36 |
| 2 | frame | 汇总 训练次数 | fill_container×82 | layout=vertical gap=2 cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=11 fontWeight=600 fill=#94A3B8 | 训练次数 |
| 3 | text | 值 | None×None | fontSize=22 fontWeight=600 fill=#0F1A24 | 4 次 |
| 1 | frame | 日历 | fill_container×None | layout=vertical gap=12 padding=16 cornerRadius=16 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 月份行 | fill_container×None |  |  |
| 3 | text | 月 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 2026 年 11 月 |
| 3 | icon | 箭头 | 16×16 | fill=#94A3B8 icon=chevron-right |  |
| 2 | frame | 星期表头 | fill_container×None |  |  |
| 3 | text | wk | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 一 |
| 3 | text | wk | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 二 |
| 3 | text | wk | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 三 |
| 3 | text | wk | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 四 |
| 3 | text | wk | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 五 |
| 3 | text | wk | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 六 |
| 3 | text | wk | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 日 |
| 2 | frame | 日期格 | fill_container×None | layout=vertical gap=6 |  |
| 3 | frame | 周 1 | fill_container×None |  |  |
| 4 | frame | d0 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 1 |
| 5 | ellipse | dot | 5×5 | fill=#047857 |  |
| 4 | frame | d0 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 1 |
| 5 | ellipse | dot | 5×5 | fill=#047857 |  |
| 4 | frame | d0 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 1 |
| 5 | ellipse | dot | 5×5 | fill=#047857 |  |
| 4 | frame | d0 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 1 |
| 5 | ellipse | dot | 5×5 | fill=#047857 |  |
| 4 | frame | d0 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 1 |
| 5 | ellipse | dot | 5×5 | fill=#047857 |  |
| 4 | frame | d0 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 1 |
| 5 | ellipse | dot | 5×5 | fill=#047857 |  |
| 4 | frame | d0 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 1 |
| 5 | ellipse | dot | 5×5 | fill=#047857 |  |
| 3 | frame | 周 2 | fill_container×None |  |  |
| 4 | frame | d1 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 2 |
| 4 | frame | d1 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 2 |
| 4 | frame | d1 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 2 |
| 4 | frame | d1 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 2 |
| 4 | frame | d1 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 2 |
| 4 | frame | d1 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 2 |
| 4 | frame | d1 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 2 |
| 3 | frame | 周 3 | fill_container×None |  |  |
| 4 | frame | d2 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 3 |
| 4 | frame | d2 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 3 |
| 4 | frame | d2 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 3 |
| 4 | frame | d2 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 3 |
| 4 | frame | d2 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 3 |
| 4 | frame | d2 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 3 |
| 4 | frame | d2 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 3 |
| 3 | frame | 周 4 | fill_container×None |  |  |
| 4 | frame | d3 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 4 |
| 4 | frame | d3 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 4 |
| 4 | frame | d3 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 4 |
| 4 | frame | d3 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 4 |
| 4 | frame | d3 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 4 |
| 4 | frame | d3 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 4 |
| 4 | frame | d3 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 4 |
| 3 | frame | 周 5 | fill_container×None |  |  |
| 4 | frame | d4 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 5 |
| 4 | frame | d4 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 5 |
| 4 | frame | d4 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 5 |
| 4 | frame | d4 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 5 |
| 4 | frame | d4 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 5 |
| 4 | frame | d4 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 5 |
| 4 | frame | d4 | 32×36 | layout=vertical gap=3 |  |
| 5 | text | num | None×None | fontSize=11 fontWeight=normal fill=#475569 | 5 |
| 1 | text | 列表标题 | fill_container×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 最近记录 |
| 1 | frame | 记录 今天 20:14 | fill_container×None | gap=10 padding=[12, 14] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 时间 | None×None | fontSize=13 fontWeight=600 fill=#0F1A24 | 今天 20:14 |
| 3 | text | 副 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 8 次 · 3:12 · 覆盖率 96% |
| 2 | text | 心率 | None×None | fontSize=16 fontWeight=600 fill=#E11D48 | 71 |
| 1 | frame | 记录 昨天 08:02 | fill_container×None | gap=10 padding=[12, 14] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 时间 | None×None | fontSize=13 fontWeight=600 fill=#0F1A24 | 昨天 08:02 |
| 3 | text | 副 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 8 次 · 3:05 · 覆盖率 92% |
| 2 | text | 心率 | None×None | fontSize=16 fontWeight=600 fill=#E11D48 | 73 |
| 1 | frame | 记录 11 月 19 日 | fill_container×None | gap=10 padding=[12, 14] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 时间 | None×None | fontSize=13 fontWeight=600 fill=#0F1A24 | 11 月 19 日 |
| 3 | text | 副 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 8 次 · 3:20 · 覆盖率 88% |
| 2 | text | 心率 | None×None | fontSize=16 fontWeight=600 fill=#E11D48 | 70 |

---

## P06-b　记录 · 空态

> 无记录时必领显空态，绝不用模拟数据填充。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[10, 22] |  |
| 1 | text | 大标题 | fill_container×None | fontSize=28 fontWeight=700 fill=#0F1A24 | 训练记录 |
| 1 | frame | 空态区 | fill_container×fill_container | layout=vertical gap=14 |  |
| 2 | frame | 图标底 | 88×88 | layout=vertical cornerRadius=44 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | icon | 图标 | 34×34 | fill=#94A3B8 icon=calendar-x |  |
| 2 | text | 空态标题 | None×None | fontSize=19 fontWeight=700 fill=#0F1A24 | 还没有训练记录 |
| 2 | text | 空态说明 | fill_container×None | fontSize=14 fontWeight=normal fill=#64748B | 完成第一次练习后，这里会出现你的 打卡日历与心率趋势 |
| 2 | frame | 去练习按钮 | 220×54 | gap=8 cornerRadius=27 fill=#0F1A24 |  |
| 3 | icon | 图标 | 16×16 | fill=#FFFFFF icon=play |  |
| 3 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 去完成第一次练习 |

---

## M01　弹窗 · 结束训练确认

> 四级页面：底部弹窗。二次确认，写明不会补全剩余次数。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | rectangle | 背景底 | 468×860 | fill=#F4F6F8 |  |
| 0 | frame | 背景内容 | 468×860 | layout=vertical gap=14 padding=[60, 22, 22, 22] |  |
| 1 | text | 阶段 | None×None | fontSize=32 fontWeight=700 fill=#0F1A24 | 保持 |
| 1 | text | 秒 | None×None | fontSize=34 fontWeight=600 fill=#0F1A24 | 00:03 |
| 0 | rectangle | 底层暗罩 | 468×860 | fill=#0F1A2480 |  |
| 0 | frame | 底部弹窗 | 468×300 | layout=vertical gap=14 padding=[10, 22, 22, 22] cornerRadius=[24, 24, 0, 0] fill=#FFFFFF |  |
| 1 | rectangle | 拉手 | 44×5 | cornerRadius=3 fill=#CBD5E1 |  |
| 1 | text | 标题 | fill_container×None | fontSize=21 fontWeight=700 fill=#0F1A24 | 要结束今天的练习吗？ |
| 1 | text | 说明 | fill_container×None | fontSize=14 fontWeight=normal fill=#5C6B7A | 已练完 2 次，提前结束不会补全剩下 6 次。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 继续练习 | fill_container×54 | layout=vertical cornerRadius=27 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 继续练习 |
| 1 | frame | 确认结束 | fill_container×50 | layout=vertical cornerRadius=25 |  |
| 2 | text | 字 | None×None | fontSize=16 fontWeight=600 fill=#B91C1C | 仍然结束 |

---

## M02　弹窗 · 退出感知确认

> 点击摄像头角标时的轻确认，避免误关感知后指标突然消失。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | rectangle | 背景底 | 468×860 | fill=#F4F6F8 |  |
| 0 | frame | 背景内容 | 468×860 | layout=vertical gap=14 padding=[60, 22, 22, 22] |  |
| 1 | text | 阶段 | None×None | fontSize=32 fontWeight=700 fill=#0F1A24 | 保持 |
| 0 | rectangle | 底层暗罩 | 468×860 | fill=#0F1A2480 |  |
| 0 | frame | 底部弹窗 | 468×280 | layout=vertical gap=14 padding=[10, 22, 22, 22] cornerRadius=[24, 24, 0, 0] fill=#FFFFFF |  |
| 1 | rectangle | 拉手 | 44×5 | cornerRadius=3 fill=#CBD5E1 |  |
| 1 | text | 标题 | fill_container×None | fontSize=21 fontWeight=700 fill=#0F1A24 | 暂停感知吗？ |
| 1 | text | 说明 | fill_container×None | fontSize=14 fontWeight=normal fill=#5C6B7A | 暂停后节律会继续，但心率与呼吸将短暂隐藏，重新开启需再等约 30 秒。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 保持开启 | fill_container×54 | layout=vertical cornerRadius=27 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 保持开启 |
| 1 | frame | 暂停感知 | fill_container×50 | layout=vertical cornerRadius=25 |  |
| 2 | text | 字 | None×None | fontSize=16 fontWeight=600 fill=#B45309 | 暂停感知 |

---

## M03　弹窗 · 中断恢复

> 进程被杀后重进。提供继续 / 丢弃，不产生脏记录（AC-13）。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | rectangle | 背景底 | 468×860 | fill=#F4F6F8 |  |
| 0 | frame | 背景内容 | 468×860 | layout=vertical gap=12 padding=[60, 22, 22, 22] |  |
| 1 | text | 标题 | None×None | fontSize=26 fontWeight=700 fill=#0F1A24 | 训练中 |
| 0 | rectangle | 暗罩 | 468×860 | fill=#0F1A2480 |  |
| 0 | frame | 居中弹窗 | 400×None | layout=vertical gap=14 padding=24 cornerRadius=20 fill=#FFFFFF |  |
| 1 | text | 标题 | fill_container×None | fontSize=20 fontWeight=700 fill=#0F1A24 | 上次练习没有完成 |
| 1 | text | 说明 | fill_container×None | fontSize=14 fontWeight=normal fill=#5C6B7A | 检测到 2 次未结束的练习。你可以接着上次的进度继续，或者丢弃重新开始。 |
| 1 | frame | 继续 | fill_container×52 | layout=vertical cornerRadius=26 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 继续上次进度 |
| 1 | frame | 丢弃 | fill_container×48 | layout=vertical cornerRadius=24 |  |
| 2 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#B91C1C | 丢弃并重新开始 |

---

## M04　弹窗 · 相机权限

> 拒绝后不弹二次系统窗，只给可执行设置指引（AC-04）。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | rectangle | 背景底 | 468×860 | fill=#F4F6F8 |  |
| 0 | frame | 背景内容 | 468×860 | layout=vertical gap=12 padding=[60, 22, 22, 22] |  |
| 0 | rectangle | 暗罩 | 468×860 | fill=#0F1A2480 |  |
| 0 | frame | 居中弹窗 | 400×None | layout=vertical gap=16 padding=[26, 24, 24, 24] cornerRadius=20 fill=#FFFFFF |  |
| 1 | frame | 图标底 | 56×56 | layout=vertical cornerRadius=16 fill=#FEF3C7 |  |
| 2 | icon | 图标 | 26×26 | fill=#B45309 icon=video-off |  |
| 1 | text | 标题 | fill_container×None | fontSize=20 fontWeight=700 fill=#0F1A24 | 需要摄像头才能开始 |
| 1 | text | 说明 | fill_container×None | fontSize=14 fontWeight=normal fill=#5C6B7A | 练习中要看到你的心率与呼吸，这需要摄像头。画面只在手机本地处理，不录像也不上传。 |
| 1 | frame | 步骤 | fill_container×None | layout=vertical gap=8 padding=14 cornerRadius=12 fill=#F8FAFC |  |
| 2 | text | 步 | fill_container×None | fontSize=13 fontWeight=normal fill=#475569 | 1. 点下方按钮打开手机设置 |
| 2 | text | 步 | fill_container×None | fontSize=13 fontWeight=normal fill=#475569 | 2. 找到「相机」，改为允许 |
| 2 | text | 步 | fill_container×None | fontSize=13 fontWeight=normal fill=#475569 | 3. 返回本应用，自动重试 |
| 1 | frame | 去设置 | fill_container×52 | gap=8 cornerRadius=26 fill=#0F1A24 |  |
| 2 | icon | 图标 | 16×16 | fill=#FFFFFF icon=settings |  |
| 2 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 去设置开启 |
| 1 | frame | 稍后 | fill_container×46 | layout=vertical cornerRadius=23 |  |
| 2 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#64748B | 稍后再说 |

---

## M05　弹窗 · 激活失败诊断

> 五类原因各一条可执行指引，不允许静默失败。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | rectangle | 背景底 | 468×860 | fill=#F4F6F8 |  |
| 0 | rectangle | 暗罩 | 468×860 | fill=#0F1A2480 |  |
| 0 | frame | 居中弹窗 | 400×None | layout=vertical gap=14 padding=[26, 24, 24, 24] cornerRadius=20 fill=#FFFFFF |  |
| 1 | frame | 图标底 | 54×54 | layout=vertical cornerRadius=15 fill=#FEE2E2 |  |
| 2 | icon | 图标 | 25×25 | fill=#B91C1C icon=triangle-alert |  |
| 1 | text | 标题 | fill_container×None | fontSize=20 fontWeight=700 fill=#0F1A24 | 感知未能初始化 |
| 1 | text | 原因 | fill_container×None | fontSize=14 fontWeight=600 fill=#B91C1C | 未激活：本机尚未完成 License 激活 |
| 1 | text | 说明 | fill_container×None | fontSize=13 fontWeight=normal fill=#5C6B7A | 请连接网络后重试。若已联网仍失败，可能是包名与 AppKey 不匹配，或设备配额已满。 |
| 1 | frame | 五类原因 | fill_container×None | layout=vertical gap=7 padding=14 cornerRadius=12 fill=#F8FAFC |  |
| 2 | text | 条 | fill_container×None | fontSize=12 fontWeight=normal fill=#475569 | · 未授权：去设置开启相机 |
| 2 | text | 条 | fill_container×None | fontSize=12 fontWeight=normal fill=#475569 | · 未激活：联网后重试一次 |
| 2 | text | 条 | fill_container×None | fontSize=12 fontWeight=normal fill=#475569 | · 包名不匹配：联系技术支持 |
| 2 | text | 条 | fill_container×None | fontSize=12 fontWeight=normal fill=#475569 | · 无网络：检查网络或热点 |
| 2 | text | 条 | fill_container×None | fontSize=12 fontWeight=normal fill=#475569 | · 配额已满：换设备或申请扩容 |
| 1 | frame | 重试 | fill_container×52 | gap=8 cornerRadius=26 fill=#0F1A24 |  |
| 2 | icon | 图标 | 16×16 | fill=#FFFFFF icon=refresh-cw |  |
| 2 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 重新初始化 |
| 1 | frame | 仅节律 | fill_container×48 | layout=vertical cornerRadius=24 |  |
| 2 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#64748B | 先用仅节律模式 |

---

## M06　弹窗 · 首次激活需联网

> 仅首次需要；激活后飞行模式可正常训练（AC-03）。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | rectangle | 背景底 | 468×860 | fill=#F4F6F8 |  |
| 0 | rectangle | 暗罩 | 468×860 | fill=#0F1A2480 |  |
| 0 | frame | 居中弹窗 | 400×None | layout=vertical gap=16 padding=[26, 24, 24, 24] cornerRadius=20 fill=#FFFFFF |  |
| 1 | frame | 图标底 | 54×54 | layout=vertical cornerRadius=15 fill=#FEF3C7 |  |
| 2 | icon | 图标 | 25×25 | fill=#B45309 icon=wifi-off |  |
| 1 | text | 标题 | fill_container×None | fontSize=20 fontWeight=700 fill=#0F1A24 | 首次使用需要联网一次 |
| 1 | text | 说明 | fill_container×None | fontSize=14 fontWeight=normal fill=#5C6B7A | 联网只用于激活本机使用权，不会上传任何画面。激活后就绪后，即使断网也能正常训练。 |
| 1 | frame | 去联网 | fill_container×52 | gap=8 cornerRadius=26 fill=#0F1A24 |  |
| 2 | icon | 图标 | 16×16 | fill=#FFFFFF icon=wifi |  |
| 2 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 去连接网络 |

---

## M07　轻提示 · 信号降级三态

> toast 与内联提示，不用弹窗打断节律。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | rectangle | 背景底 | 468×860 | fill=#F4F6F8 |  |
| 0 | frame | 背景色块 | 468×860 | layout=vertical gap=16 padding=[70, 22, 22, 22] |  |
| 1 | frame | 状态 人脸丢失 < 3s | fill_container×None | layout=vertical gap=8 padding=16 cornerRadius=14 fill=#FEF3C7 |  |
| 2 | frame | 行 | fill_container×None | gap=8 |  |
| 3 | ellipse | 点 | 8×8 | fill=#FBBF24 |  |
| 3 | text | 标题 | fill_container×None | fontSize=14 fontWeight=700 fill=#B45309 | 人脸丢失 < 3s |
| 2 | text | 说明 | fill_container×None | fontSize=13 fontWeight=normal fill=#B45309 | 指标条半透明 + 轻提示 |
| 1 | frame | 状态 人脸丢失 ≥ 3s | fill_container×None | layout=vertical gap=8 padding=16 cornerRadius=14 fill=#F1F5F9 |  |
| 2 | frame | 行 | fill_container×None | gap=8 |  |
| 3 | ellipse | 点 | 8×8 | fill=#94A3B8 |  |
| 3 | text | 标题 | fill_container×None | fontSize=14 fontWeight=700 fill=#475569 | 人脸丢失 ≥ 3s |
| 2 | text | 说明 | fill_container×None | fontSize=13 fontWeight=normal fill=#475569 | 置灰显示「信号暂失」，节律继续 |
| 1 | frame | 状态 SQI 低于门限 | fill_container×None | layout=vertical gap=8 padding=16 cornerRadius=14 fill=#FEE2E2 |  |
| 2 | frame | 行 | fill_container×None | gap=8 |  |
| 3 | ellipse | 点 | 8×8 | fill=#F87171 |  |
| 3 | text | 标题 | fill_container×None | fontSize=14 fontWeight=700 fill=#B91C1C | SQI 低于门限 |
| 2 | text | 说明 | fill_container×None | fontSize=13 fontWeight=normal fill=#B91C1C | 心率显示 --，不记录本次值 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 0 | frame | 底部 toast | 424×None | gap=10 padding=[14, 18] cornerRadius=14 fill=#0F1AE6 |  |
| 1 | icon | 图标 | 16×16 | fill=#5EEAD4 icon=info |  |
| 1 | text | 字 | fill_container×None | fontSize=14 fontWeight=600 fill=#FFFFFF | 光线不足，请面向光源 |

---

## P07　设置（P1）

> 语音、提醒、隐私、重做校准、清除数据。危险操作单独分区。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=14 padding=[10, 22] |  |
| 1 | text | 大标题 | fill_container×None | fontSize=28 fontWeight=700 fill=#0F1A24 | 设置 |
| 1 | frame | 分组 语音 | fill_container×None | layout=vertical cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 组标题行 | fill_container×None | layout=vertical padding=[12, 16, 4, 16] |  |
| 3 | text | 组标题 | fill_container×None | fontSize=11 fontWeight=700 fill=#94A3B8 | 语音 |
| 2 | frame | 项 语音速度 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#475569 icon=gauge |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#0F1A24 | 语音速度 |
| 3 | text | 值 | None×None | fontSize=13 fontWeight=normal fill=#94A3B8 | 1.0x |
| 2 | frame | 项 音量 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#475569 icon=volume-2 |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#0F1A24 | 音量 |
| 3 | text | 值 | None×None | fontSize=13 fontWeight=normal fill=#94A3B8 | 跟随系统 |
| 2 | frame | 项 重复当前提示 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#475569 icon=rotate-ccw |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#0F1A24 | 重复当前提示 |
| 3 | icon | 箭头 | 16×16 | fill=#CBD5E1 icon=chevron-right |  |
| 1 | frame | 分组 提醒 | fill_container×None | layout=vertical cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 组标题行 | fill_container×None | layout=vertical padding=[12, 16, 4, 16] |  |
| 3 | text | 组标题 | fill_container×None | fontSize=11 fontWeight=700 fill=#94A3B8 | 提醒 |
| 2 | frame | 项 每日提醒 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#475569 icon=bell |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#0F1A24 | 每日提醒 |
| 3 | text | 值 | None×None | fontSize=13 fontWeight=normal fill=#94A3B8 | 已开启 |
| 2 | frame | 项 提醒时间 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#475569 icon=clock |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#0F1A24 | 提醒时间 |
| 3 | text | 值 | None×None | fontSize=13 fontWeight=normal fill=#94A3B8 | 每天 20:00 |
| 1 | frame | 分组 摄像头与校准 | fill_container×None | layout=vertical cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 组标题行 | fill_container×None | layout=vertical padding=[12, 16, 4, 16] |  |
| 3 | text | 组标题 | fill_container×None | fontSize=11 fontWeight=700 fill=#94A3B8 | 摄像头与校准 |
| 2 | frame | 项 重做校准 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#475569 icon=focus |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#0F1A24 | 重做校准 |
| 3 | icon | 箭头 | 16×16 | fill=#CBD5E1 icon=chevron-right |  |
| 2 | frame | 项 设备诊断 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#475569 icon=activity |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#0F1A24 | 设备诊断 |
| 3 | icon | 箭头 | 16×16 | fill=#CBD5E1 icon=chevron-right |  |
| 1 | frame | 分组 隐私与数据 | fill_container×None | layout=vertical cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 组标题行 | fill_container×None | layout=vertical padding=[12, 16, 4, 16] |  |
| 3 | text | 组标题 | fill_container×None | fontSize=11 fontWeight=700 fill=#94A3B8 | 隐私与数据 |
| 2 | frame | 项 隐私说明 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#475569 icon=shield-check |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#0F1A24 | 隐私说明 |
| 3 | icon | 箭头 | 16×16 | fill=#CBD5E1 icon=chevron-right |  |
| 2 | frame | 项 清除本机数据 | fill_container×None | padding=[14, 16] |  |
| 3 | frame | 左 | None×None | gap=10 |  |
| 4 | icon | 图标 | 17×17 | fill=#B91C1C icon=trash-2 |  |
| 4 | text | 文字 | None×None | fontSize=15 fontWeight=500 fill=#B91C1C | 清除本机数据 |
| 1 | text | 版本 | fill_container×None | fontSize=11 fontWeight=normal fill=#94A3B8 | HerRhythm v1.1.0（公测）　·　SDK v1.5.0 |

---

## P08　漏尿日记（P1）

> 五档频次 + 情境标签，全部可跳过，仅本机。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[10, 22, 22, 22] |  |
| 1 | text | 大标题 | fill_container×None | fontSize=26 fontWeight=700 fill=#0F1A24 | 今天有漏尿吗？ |
| 1 | text | 副文 | fill_container×None | fontSize=14 fontWeight=normal fill=#64748B | 记下来能帮你和医生看清规律，也可以跳过。 |
| 1 | frame | 频次卡 | fill_container×None | layout=vertical gap=10 padding=18 cornerRadius=16 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | text | 卡标题 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 漏尿次数 |
| 2 | frame | 五档 | fill_container×None | gap=8 |  |
| 3 | frame | 档 没有 | fill_container×52 | layout=vertical cornerRadius=12 fill=#0F1A24 stroke=#0F1A24 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#FFFFFF | 没有 |
| 3 | frame | 档 1 次 | fill_container×52 | layout=vertical cornerRadius=12 fill=#F8FAFC stroke=#E2E8F0 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 1 次 |
| 3 | frame | 档 2 次 | fill_container×52 | layout=vertical cornerRadius=12 fill=#F8FAFC stroke=#E2E8F0 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 2 次 |
| 3 | frame | 档 3 次 | fill_container×52 | layout=vertical cornerRadius=12 fill=#F8FAFC stroke=#E2E8F0 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 3 次 |
| 3 | frame | 档 更多 | fill_container×52 | layout=vertical cornerRadius=12 fill=#F8FAFC stroke=#E2E8F0 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 更多 |
| 1 | frame | 情境卡 | fill_container×None | layout=vertical gap=12 padding=18 cornerRadius=16 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | text | 卡标题2 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 当时在做什么？可多选 |
| 2 | frame | 标签流 | fill_container×None | gap=8 |  |
| 3 | frame | 行1 | fill_container×None | gap=8 |  |
| 4 | frame | 标签 咳嗽 | None×40 | padding=[0, 14] cornerRadius=20 fill=#E7F6EC stroke=#047857 |  |
| 5 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#065F46 | 咳嗽 |
| 4 | frame | 标签 打喷嚏 | None×40 | padding=[0, 14] cornerRadius=20 fill=#F8FAFC stroke=#E2E8F0 |  |
| 5 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 打喷嚏 |
| 4 | frame | 标签 提重物 | None×40 | padding=[0, 14] cornerRadius=20 fill=#F8FAFC stroke=#E2E8F0 |  |
| 5 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 提重物 |
| 4 | frame | 标签 走路 | None×40 | padding=[0, 14] cornerRadius=20 fill=#F8FAFC stroke=#E2E8F0 |  |
| 5 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 走路 |
| 2 | frame | 行2 | fill_container×None | gap=8 |  |
| 3 | frame | 标签 笑 | None×40 | padding=[0, 14] cornerRadius=20 fill=#F8FAFC stroke=#E2E8F0 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 笑 |
| 3 | frame | 标签 运动 | None×40 | padding=[0, 14] cornerRadius=20 fill=#F8FAFC stroke=#E2E8F0 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 运动 |
| 3 | frame | 标签 没注意 | None×40 | padding=[0, 14] cornerRadius=20 fill=#F8FAFC stroke=#E2E8F0 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 没注意 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 保存 | fill_container×54 | layout=vertical cornerRadius=27 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 保存今天的情况 |
| 1 | frame | 跳过 | fill_container×48 | layout=vertical cornerRadius=24 |  |
| 2 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#64748B | 今天先跳过 |

---

## L2-01　筛查结论

> 二级页：从 P01-b 完成三题后进入。根据 Q1/Q3 给分级建议。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 筛查结果 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[8, 22, 22, 22] |  |
| 1 | frame | 结论卡 | fill_container×None | layout=vertical gap=12 padding=[22, 20] cornerRadius=18 fill=#E7F6EC |  |
| 2 | frame | 图标底 | 48×48 | layout=vertical cornerRadius=14 fill=#FFFFFF |  |
| 3 | icon | 图标 | 24×24 | fill=#047857 icon=circle-check |  |
| 2 | text | 结论 | fill_container×None | fontSize=22 fontWeight=700 fill=#065F46 | 可以从今天开始练习 |
| 2 | text | 说明 | fill_container×None | fontSize=14 fontWeight=normal fill=#065F46D9 | 你的情况适合从基础节奏开始。建议每天 1 组 8 次，以不感到疲劳为准。 |
| 1 | text | 小标题 | fill_container×None | fontSize=13 fontWeight=700 fill=#94A3B8 | 根据你的回答 |
| 1 | frame | 回顾 专业指导 | fill_container×None | padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | text | 问题 | fill_container×None | fontSize=14 fontWeight=normal fill=#475569 | 专业指导 |
| 2 | text | 答 | None×None | fontSize=14 fontWeight=700 fill=#047857 | 有 |
| 1 | frame | 回顾 疼痛 / 血尿 / 排尿困难 | fill_container×None | padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | text | 问题 | fill_container×None | fontSize=14 fontWeight=normal fill=#475569 | 疼痛 / 血尿 / 排尿困难 |
| 2 | text | 答 | None×None | fontSize=14 fontWeight=700 fill=#047857 | 无 |
| 1 | frame | 回顾 能感知盆底肌收缩 | fill_container×None | padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | text | 问题 | fill_container×None | fontSize=14 fontWeight=normal fill=#475569 | 能感知盆底肌收缩 |
| 2 | text | 答 | None×None | fontSize=14 fontWeight=700 fill=#047857 | 能 |
| 1 | frame | 提醒 | fill_container×None | gap=10 padding=[14, 16] cornerRadius=12 fill=#F1F5F9 |  |
| 2 | icon | 图标 | 15×15 | fill=#64748B icon=info |  |
| 2 | text | 字 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 本应用不提供诊断与治疗。如症状加重请及时就医。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 主按钮 | fill_container×56 | gap=8 cornerRadius=28 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 进入首页 |

---

## L2-02　就医提示（Q2 选是）

> 二级页：筛查 Q2 选「是」时进入，并让训练页顶部常驻提示条。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 需要先看医生 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[8, 22, 22, 22] |  |
| 1 | frame | 警示卡 | fill_container×None | layout=vertical gap=12 padding=[22, 20] cornerRadius=18 fill=#FEF3C7 |  |
| 2 | frame | 图标底 | 48×48 | layout=vertical cornerRadius=14 fill=#FFFFFF |  |
| 3 | icon | 图标 | 24×24 | fill=#B45309 icon=triangle-alert |  |
| 2 | text | 结论 | fill_container×None | fontSize=22 fontWeight=700 fill=#92400E | 建议先咨询专业医生 |
| 2 | text | 说明 | fill_container×None | fontSize=14 fontWeight=normal fill=#92400ED9 | 你提到有盆腔疼痛、血尿或排尿困难。这类情况需要先明确原因，不建议自行开始训练。 |
| 1 | text | 小标题 | fill_container×None | fontSize=13 fontWeight=700 fill=#94A3B8 | 你可以怎么做 |
| 1 | frame | 建议 去医院 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | ellipse | 点 | 6×6 | fill=#94A3B8 |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 主 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 去医院 |
| 3 | text | 副 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 挂妇科、泌尿科或盆底康复科 |
| 1 | frame | 建议 先诊断 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | ellipse | 点 | 6×6 | fill=#94A3B8 |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 主 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 先诊断 |
| 3 | text | 副 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 确认没有急性问题后再运动 |
| 1 | frame | 建议 仍可记录 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | ellipse | 点 | 6×6 | fill=#94A3B8 |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 主 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 仍可记录 |
| 3 | text | 副 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 本应用可继续用来记日记、看知识 |
| 1 | frame | 建议 暂不建议 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | ellipse | 点 | 6×6 | fill=#94A3B8 |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 主 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 暂不建议 |
| 3 | text | 副 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 自行做收缩练习或增加强度 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 继续 | fill_container×56 | layout=vertical cornerRadius=28 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 我知道了，继续使用 |
| 1 | frame | 返回修改 | fill_container×48 | layout=vertical cornerRadius=24 |  |
| 2 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#64748B | 返回修改回答 |

---

## L2-03　今日训练详情

> 二级页：从 P02 今日训练卡点「详情」进入，展开四个阶段参数。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 今日训练 |
| 1 | text | 右 | None×None | fontSize=14 fontWeight=600 fill=#0F766E | 修改 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[8, 22, 22, 22] |  |
| 1 | frame | 总览卡 | fill_container×None | layout=vertical gap=8 padding=20 cornerRadius=18 fill=#0F1A24 |  |
| 2 | text | 标 | None×None | fontSize=12 fontWeight=600 fill=#5EEAD4 | 今日计划 |
| 2 | text | 主 | fill_container×None | fontSize=22 fontWeight=700 fill=#FFFFFF | 8 次　·　约 3 分 20 秒 |
| 2 | text | 副 | fill_container×None | fontSize=12 fontWeight=normal fill=#8FA3B8 | 单组循环，四阶段各 5 秒，共 8 次 |
| 1 | text | 小标题 | fill_container×None | fontSize=13 fontWeight=700 fill=#94A3B8 | 四个阶段 |
| 1 | frame | 阶段 准备 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | frame | 序号 | 32×32 | layout=vertical cornerRadius=16 fill=#FFFFFF |  |
| 3 | text | n | None×None | fontSize=14 fontWeight=700 fill=#475569 | 准 |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 名 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 准备 |
| 3 | text | 说 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 坐稳，放松肩膀和腹部 |
| 2 | text | 时长 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 00:05 |
| 1 | frame | 阶段 收缩 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | frame | 序号 | 32×32 | layout=vertical cornerRadius=16 fill=#FFFFFF |  |
| 3 | text | n | None×None | fontSize=14 fontWeight=700 fill=#475569 | 收 |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 名 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 收缩 |
| 3 | text | 说 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 轻轻向上提起盆底，不要向下用力 |
| 2 | text | 时长 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 00:05 |
| 1 | frame | 阶段 保持 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#E7F6EC stroke=#047857 |  |
| 2 | frame | 序号 | 32×32 | layout=vertical cornerRadius=16 fill=#047857 |  |
| 3 | text | n | None×None | fontSize=14 fontWeight=700 fill=#FFFFFF | 保 |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 名 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 保持 |
| 3 | text | 说 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 保持住，继续自然呼吸，不要憋气 |
| 2 | text | 时长 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 00:05 |
| 1 | frame | 阶段 放松 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | frame | 序号 | 32×32 | layout=vertical cornerRadius=16 fill=#FFFFFF |  |
| 3 | text | n | None×None | fontSize=14 fontWeight=700 fill=#475569 | 放 |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 名 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 放松 |
| 3 | text | 说 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 慢慢放松，感受盆底回到原位 |
| 2 | text | 时长 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 00:05 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 主按钮 | fill_container×56 | gap=8 cornerRadius=28 fill=#0F1A24 |  |
| 2 | icon | 图标 | 17×17 | fill=#FFFFFF icon=play |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 开始训练 |

---

## L2-04　设备诊断（详情）

> 二级页：从 P03-c 折叠区展开。SQI 小数只在此页出现。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 设备诊断 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=14 padding=[8, 22, 22, 22] |  |
| 1 | frame | 状态卡 | fill_container×None | gap=12 padding=[16, 18] cornerRadius=16 fill=#E7F6EC |  |
| 2 | ellipse | 点 | 10×10 | fill=#047857 |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 主 | None×None | fontSize=16 fontWeight=700 fill=#065F46 | 感知服务正常 |
| 3 | text | 副 | None×None | fontSize=11 fontWeight=normal fill=#065F46B3 | 上次拉取 1 秒前 |
| 1 | text | 小标题 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | SDK |
| 1 | frame | SDK 组 | fill_container×None | layout=vertical cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 行 SDK 版本 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | SDK 版本 |
| 3 | text | v | None×None | fontSize=12 fontWeight=600 fill=#0F1A24 | v1.5.0 |
| 2 | frame | 行 初始化结果 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 初始化结果 |
| 3 | text | v | None×None | fontSize=12 fontWeight=600 fill=#0F1A24 | success |
| 2 | frame | 行 模型资源 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 模型资源 |
| 3 | text | v | None×None | fontSize=12 fontWeight=600 fill=#0F1A24 | rppg_v15.enc |
| 2 | frame | 行 包名绑定 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 包名绑定 |
| 3 | text | v | None×None | fontSize=12 fontWeight=600 fill=#0F1A24 | com.herrhythm.app |
| 1 | text | 小标题2 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | License |
| 1 | frame | License 组 | fill_container×None | layout=vertical cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 行 激活状态 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 激活状态 |
| 3 | text | v | None×None | fontSize=12 fontWeight=600 fill=#0F1A24 | 已激活 |
| 2 | frame | 行 剩余天数 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 剩余天数 |
| 3 | text | v | None×None | fontSize=12 fontWeight=600 fill=#0F1A24 | 28 天 |
| 2 | frame | 行 本机设备数 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 本机设备数 |
| 3 | text | v | None×None | fontSize=12 fontWeight=600 fill=#0F1A24 | 1 / 1 |
| 1 | text | 小标题3 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 实时信号（调试信息） |
| 1 | frame | 信号组 | fill_container×None | layout=vertical cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 行 当前 SQI | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 当前 SQI |
| 3 | text | v | None×None | fontSize=13 fontWeight=700 fill=#047857 | 0.82 |
| 2 | frame | 行 显示门限 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 显示门限 |
| 3 | text | v | None×None | fontSize=13 fontWeight=700 fill=#64748B | 0.50 |
| 2 | frame | 行 人脸占比 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 人脸占比 |
| 3 | text | v | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 38 % |
| 2 | frame | 行 分析帧率 | fill_container×None | padding=[13, 16] |  |
| 3 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 分析帧率 |
| 3 | text | v | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 12 fps |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 复制日志 | fill_container×50 | gap=8 cornerRadius=25 stroke=#CBD5E1 |  |
| 2 | icon | 图标 | 15×15 | fill=#0F1A24 icon=copy |  |
| 2 | text | 字 | None×None | fontSize=15 fontWeight=600 fill=#0F1A24 | 复制诊断日志 |

---

## L2-05　单次训练详情

> 二级页：从 P06 列表点某条记录进入。含时间线、指标、自评。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 11 月 20 日 |
| 1 | text | 右 | None×None | fontSize=14 fontWeight=600 fill=#0F766E | 删除 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=14 padding=[8, 22, 22, 22] |  |
| 1 | frame | 头部卡 | fill_container×None | layout=vertical gap=6 padding=20 cornerRadius=18 fill=#0F1A24 |  |
| 2 | text | 标 | None×None | fontSize=11 fontWeight=600 fill=#5EEAD4 | 晚上 20:14　·　单组完成 |
| 2 | text | 主 | None×None | fontSize=30 fontWeight=600 fill=#FFFFFF | 3 分 12 秒 |
| 2 | text | 副 | None×None | fontSize=12 fontWeight=normal fill=#8FA3B8 | 8 次全部完成，暂停 1 次 |
| 1 | frame | 指标格 | fill_container×None | gap=10 |  |
| 2 | frame | 格 平均心率 | fill_container×84 | layout=vertical gap=2 cornerRadius=12 fill=#F8FAFC stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 平均心率 |
| 3 | text | 值 | None×None | fontSize=22 fontWeight=600 fill=#0F1A24 | 71 |
| 3 | text | 单位 | None×None | fontSize=9 fontWeight=normal fill=#94A3B8 | bpm |
| 2 | frame | 格 平均呼吸 | fill_container×84 | layout=vertical gap=2 cornerRadius=12 fill=#F8FAFC stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 平均呼吸 |
| 3 | text | 值 | None×None | fontSize=22 fontWeight=600 fill=#0F1A24 | 16 |
| 3 | text | 单位 | None×None | fontSize=9 fontWeight=normal fill=#94A3B8 | 次/分 |
| 2 | frame | 格 覆盖率 | fill_container×84 | layout=vertical gap=2 cornerRadius=12 fill=#F8FAFC stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 覆盖率 |
| 3 | text | 值 | None×None | fontSize=22 fontWeight=600 fill=#0F1A24 | 96 |
| 3 | text | 单位 | None×None | fontSize=9 fontWeight=normal fill=#94A3B8 | % |
| 1 | text | 小标题 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 本次节奏 |
| 1 | frame | 时间线 | fill_container×None | gap=3 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#CBD5E1 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#047857 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#0F766E |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#94A3B8 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#CBD5E1 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#047857 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#0F766E |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#94A3B8 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#CBD5E1 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#047857 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#0F766E |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#94A3B8 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#CBD5E1 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#047857 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#0F766E |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#94A3B8 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#CBD5E1 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#047857 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#0F766E |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#94A3B8 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#CBD5E1 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#047857 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#0F766E |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#94A3B8 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#CBD5E1 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#047857 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#0F766E |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#94A3B8 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#CBD5E1 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#047857 |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#0F766E |  |
| 2 | rectangle | 段 | fill_container×26 | cornerRadius=3 fill=#94A3B8 |  |
| 1 | frame | 图例 | fill_container×None | gap=14 |  |
| 2 | frame | 图例 准备 | None×None | gap=5 |  |
| 3 | rectangle | 色 | 10×10 | cornerRadius=2 fill=#CBD5E1 |  |
| 3 | text | 字 | None×None | fontSize=11 fontWeight=normal fill=#64748B | 准备 |
| 2 | frame | 图例 收缩 | None×None | gap=5 |  |
| 3 | rectangle | 色 | 10×10 | cornerRadius=2 fill=#047857 |  |
| 3 | text | 字 | None×None | fontSize=11 fontWeight=normal fill=#64748B | 收缩 |
| 2 | frame | 图例 保持 | None×None | gap=5 |  |
| 3 | rectangle | 色 | 10×10 | cornerRadius=2 fill=#0F766E |  |
| 3 | text | 字 | None×None | fontSize=11 fontWeight=normal fill=#64748B | 保持 |
| 2 | frame | 图例 放松 | None×None | gap=5 |  |
| 3 | rectangle | 色 | 10×10 | cornerRadius=2 fill=#94A3B8 |  |
| 3 | text | 字 | None×None | fontSize=11 fontWeight=normal fill=#64748B | 放松 |
| 1 | text | 小标题2 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 你的自评 |
| 1 | frame | 自评 强度感受 | fill_container×None | padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | text | k | None×None | fontSize=14 fontWeight=normal fill=#475569 | 强度感受 |
| 2 | text | v | None×None | fontSize=14 fontWeight=700 fill=#047857 | 合适 |
| 1 | frame | 自评 有无不适 | fill_container×None | padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | text | k | None×None | fontSize=14 fontWeight=normal fill=#475569 | 有无不适 |
| 2 | text | v | None×None | fontSize=14 fontWeight=700 fill=#047857 | 没有不适 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | text | 免责 | fill_container×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 曲线与数值反映身体状态，不代表盆底肌收缩质量 |

---

## L2-06　隐私说明（详情）

> 二级页：从 P01-a / P07 进入。完整版隐私说明。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 隐私说明 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=14 padding=[8, 22, 22, 22] |  |
| 1 | frame | 条款 画面只在本地处理 | fill_container×None | layout=vertical gap=6 padding=16 cornerRadius=14 fill=#F8FAFC |  |
| 2 | frame | 行 | fill_container×None | gap=8 |  |
| 3 | icon | 图标 | 15×15 | fill=#047857 icon=shield-check |  |
| 3 | text | 标题 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 画面只在本地处理 |
| 2 | text | 正文 | fill_container×None | fontSize=13 fontWeight=normal fill=#5C6B7A | 摄像头采集的每一帧都在手机内存中完成人脸检测与信号计算，处理完立即丢弃，不写入存储、不缓存截图。 |
| 1 | frame | 条款 不录像、不上传 | fill_container×None | layout=vertical gap=6 padding=16 cornerRadius=14 fill=#F8FAFC |  |
| 2 | frame | 行 | fill_container×None | gap=8 |  |
| 3 | icon | 图标 | 15×15 | fill=#047857 icon=shield-check |  |
| 3 | text | 标题 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 不录像、不上传 |
| 2 | text | 正文 | fill_container×None | fontSize=13 fontWeight=normal fill=#5C6B7A | 应用没有录制功能，也不会把画面或帧数据发送到任何服务器。关闭应用后没有任何图像残留。 |
| 1 | frame | 条款 只有激活数据出网 | fill_container×None | layout=vertical gap=6 padding=16 cornerRadius=14 fill=#F8FAFC |  |
| 2 | frame | 行 | fill_container×None | gap=8 |  |
| 3 | icon | 图标 | 15×15 | fill=#047857 icon=shield-check |  |
| 3 | text | 标题 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 只有激活数据出网 |
| 2 | text | 正文 | fill_container×None | fontSize=13 fontWeight=normal fill=#5C6B7A | 首次使用时会发送设备标识与 License 请求到激活服务，用于确认本机使用权。此外没有任何网络请求。 |
| 1 | frame | 条款 你可以随时关闭 | fill_container×None | layout=vertical gap=6 padding=16 cornerRadius=14 fill=#F8FAFC |  |
| 2 | frame | 行 | fill_container×None | gap=8 |  |
| 3 | icon | 图标 | 15×15 | fill=#047857 icon=shield-check |  |
| 3 | text | 标题 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 你可以随时关闭 |
| 2 | text | 正文 | fill_container×None | fontSize=13 fontWeight=normal fill=#5C6B7A | 训练中点击右上角摄像头角标即可暂停感知，节律仍会继续，指标会变灰而非报错。 |
| 1 | frame | 条款 本机数据可删除 | fill_container×None | layout=vertical gap=6 padding=16 cornerRadius=14 fill=#F8FAFC |  |
| 2 | frame | 行 | fill_container×None | gap=8 |  |
| 3 | icon | 图标 | 15×15 | fill=#047857 icon=shield-check |  |
| 3 | text | 标题 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 本机数据可删除 |
| 2 | text | 正文 | fill_container×None | fontSize=13 fontWeight=normal fill=#5C6B7A | 设置 → 清除本机数据，可删除全部训练记录、日记与设置，不可恢复。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | text | 版本 | fill_container×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 隐私条款版本 1.1　·　2026-11-20 生效 |

---

## L2-07　语音设置

> 二级页：从 P07 「语音」组进入。速度、音量、试听。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 语音 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=14 padding=[8, 22, 22, 22] |  |
| 1 | text | 小标题 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 语速 |
| 1 | frame | 语速卡 | fill_container×None | layout=vertical gap=14 padding=18 cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 值行 | fill_container×None |  |  |
| 3 | text | 名 | None×None | fontSize=14 fontWeight=normal fill=#475569 | 当前倍速 |
| 3 | text | 值 | None×None | fontSize=24 fontWeight=600 fill=#0F1A24 | 1.0x |
| 2 | frame | 滑轨 | fill_container×6 | layout=none cornerRadius=3 fill=#E2E8F0 |  |
| 3 | rectangle | 已选 | 155×6 | cornerRadius=3 fill=#0F766E |  |
| 3 | ellipse | 手杆 | 20×20 | fill=#FFFFFF stroke=#0F766E |  |
| 2 | frame | 刻度 | fill_container×None |  |  |
| 3 | text | 刻 | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 0.8x |
| 3 | text | 刻 | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 0.9x |
| 3 | text | 刻 | None×None | fontSize=10 fontWeight=normal fill=#0F766E | 1.0x |
| 3 | text | 刻 | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 1.1x |
| 3 | text | 刻 | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 1.2x |
| 1 | text | 小标题2 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 音量 |
| 1 | frame | 音量卡 | fill_container×None | padding=[16, 18] cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | None×None | gap=10 |  |
| 3 | icon | 图标 | 17×17 | fill=#475569 icon=volume-2 |  |
| 3 | text | 字 | None×None | fontSize=14 fontWeight=normal fill=#0F1A24 | 跟随系统媒体音量 |
| 2 | text | 值 | None×None | fontSize=13 fontWeight=600 fill=#047857 | 开启 |
| 1 | text | 小标题3 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 试听 |
| 1 | frame | 试听 准备阶段 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 播放底 | 36×36 | layout=vertical cornerRadius=18 fill=#F1F5F9 |  |
| 3 | icon | 播 | 14×14 | fill=#0F1A24 icon=play |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 名 | None×None | fontSize=14 fontWeight=600 fill=#0F1A24 | 准备阶段 |
| 3 | text | 文 | fill_container×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 坐稳，放松肩膀和腹部 |
| 1 | frame | 试听 收缩阶段 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 播放底 | 36×36 | layout=vertical cornerRadius=18 fill=#F1F5F9 |  |
| 3 | icon | 播 | 14×14 | fill=#0F1A24 icon=play |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 名 | None×None | fontSize=14 fontWeight=600 fill=#0F1A24 | 收缩阶段 |
| 3 | text | 文 | fill_container×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 轻轻提起盆底，不要向下用力 |
| 1 | frame | 试听 保持阶段 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 播放底 | 36×36 | layout=vertical cornerRadius=18 fill=#F1F5F9 |  |
| 3 | icon | 播 | 14×14 | fill=#0F1A24 icon=play |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 名 | None×None | fontSize=14 fontWeight=600 fill=#0F1A24 | 保持阶段 |
| 3 | text | 文 | fill_container×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 保持住，继续自然呼吸 |
| 1 | frame | 试听 放松阶段 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 播放底 | 36×36 | layout=vertical cornerRadius=18 fill=#F1F5F9 |  |
| 3 | icon | 播 | 14×14 | fill=#0F1A24 icon=play |  |
| 2 | frame | 文字 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 名 | None×None | fontSize=14 fontWeight=600 fill=#0F1A24 | 放松阶段 |
| 3 | text | 文 | fill_container×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 慢慢放松，感受盆底回到原位 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | text | 备注 | fill_container×None | fontSize=11 fontWeight=normal fill=#B45309 | 音频素材未就绪时使用系统 TTS 临时替代 |

---

## L2-08　提醒设置

> 二级页：从 P07 「提醒」组进入。单个每日时间，不用精确闹钟。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 每日提醒 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=14 padding=[8, 22, 22, 22] |  |
| 1 | frame | 开关卡 | fill_container×None | padding=18 cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | None×None | layout=vertical gap=3 |  |
| 3 | text | 名 | None×None | fontSize=15 fontWeight=700 fill=#0F1A24 | 每天提醒我练习 |
| 3 | text | 副 | None×None | fontSize=12 fontWeight=normal fill=#94A3B8 | 到时间会发一条通知 |
| 2 | frame | 开关 | 50×30 | layout=none cornerRadius=15 fill=#0F766E |  |
| 3 | ellipse | 钮 | 24×24 | fill=#FFFFFF |  |
| 1 | text | 小标题 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 提醒时间 |
| 1 | frame | 时间卡 | fill_container×None | padding=[22, 18] cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | text | 标签 | None×None | fontSize=14 fontWeight=normal fill=#475569 | 提醒时间 |
| 2 | text | 时间 | None×None | fontSize=26 fontWeight=600 fill=#0F1A24 | 20:00 |
| 1 | text | 小标题2 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 重复 |
| 1 | frame | 重复卡 | fill_container×None | layout=vertical gap=10 padding=[16, 18] cornerRadius=14 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | text | 标 | None×None | fontSize=14 fontWeight=normal fill=#0F1A24 | 每天（周一至周日） |
| 2 | frame | 星期 | fill_container×None | gap=6 |  |
| 3 | frame | 日 一 | fill_container×38 | layout=vertical cornerRadius=19 fill=#E7F6EC stroke=#047857 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=700 fill=#065F46 | 一 |
| 3 | frame | 日 二 | fill_container×38 | layout=vertical cornerRadius=19 fill=#E7F6EC stroke=#047857 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=700 fill=#065F46 | 二 |
| 3 | frame | 日 三 | fill_container×38 | layout=vertical cornerRadius=19 fill=#E7F6EC stroke=#047857 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=700 fill=#065F46 | 三 |
| 3 | frame | 日 四 | fill_container×38 | layout=vertical cornerRadius=19 fill=#E7F6EC stroke=#047857 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=700 fill=#065F46 | 四 |
| 3 | frame | 日 五 | fill_container×38 | layout=vertical cornerRadius=19 fill=#E7F6EC stroke=#047857 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=700 fill=#065F46 | 五 |
| 3 | frame | 日 六 | fill_container×38 | layout=vertical cornerRadius=19 fill=#E7F6EC stroke=#047857 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=700 fill=#065F46 | 六 |
| 3 | frame | 日 日 | fill_container×38 | layout=vertical cornerRadius=19 fill=#E7F6EC stroke=#047857 |  |
| 4 | text | 字 | None×None | fontSize=13 fontWeight=700 fill=#065F46 | 日 |
| 1 | frame | 说明 | fill_container×None | gap=10 padding=[14, 16] cornerRadius=12 fill=#F1F5F9 |  |
| 2 | icon | 图标 | 15×15 | fill=#64748B icon=info |  |
| 2 | text | 字 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 通知可能有几分钟偏差，不会申请精确闹钟权限。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 保存 | fill_container×54 | layout=vertical cornerRadius=27 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 保存 |

---

## L2-09　漏尿日记历史

> 二级页：从 P08 保存后进入，或从首页入口。近 7 天概览。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 漏尿日记 |
| 1 | text | 右 | None×None | fontSize=14 fontWeight=600 fill=#0F766E | 新增 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=14 padding=[8, 22, 22, 22] |  |
| 1 | frame | 统计卡 | fill_container×None | gap=10 |  |
| 2 | frame | 统 近 7 天 | fill_container×84 | layout=vertical gap=1 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 近 7 天 |
| 3 | text | 值 | None×None | fontSize=20 fontWeight=600 fill=#0F1A24 | 3 |
| 3 | text | 单 | None×None | fontSize=9 fontWeight=normal fill=#94A3B8 | 天有记录 |
| 2 | frame | 统 本周次数 | fill_container×84 | layout=vertical gap=1 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 本周次数 |
| 3 | text | 值 | None×None | fontSize=20 fontWeight=600 fill=#0F1A24 | 7 |
| 3 | text | 单 | None×None | fontSize=9 fontWeight=normal fill=#94A3B8 | 次 |
| 2 | frame | 统 最常见 | fill_container×84 | layout=vertical gap=1 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=600 fill=#94A3B8 | 最常见 |
| 3 | text | 值 | None×None | fontSize=20 fontWeight=600 fill=#0F1A24 | 咳啾 |
| 3 | text | 单 | None×None | fontSize=9 fontWeight=normal fill=#94A3B8 | 情境 |
| 1 | text | 小标题 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 最近记录 |
| 1 | frame | 条 今天 | fill_container×None | padding=16 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | None×None | layout=vertical gap=3 |  |
| 3 | text | 日 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 今天 |
| 3 | text | 情境 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 咳啾、提重物 |
| 2 | frame | 右 | None×None | gap=8 |  |
| 3 | text | 次 | None×None | fontSize=14 fontWeight=600 fill=#B45309 | 2 次 |
| 3 | icon | 箭头 | 15×15 | fill=#CBD5E1 icon=chevron-right |  |
| 1 | frame | 条 昨天 | fill_container×None | padding=16 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | None×None | layout=vertical gap=3 |  |
| 3 | text | 日 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 昨天 |
| 3 | text | 情境 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 打喷嚏 |
| 2 | frame | 右 | None×None | gap=8 |  |
| 3 | text | 次 | None×None | fontSize=14 fontWeight=600 fill=#B45309 | 1 次 |
| 3 | icon | 箭头 | 15×15 | fill=#CBD5E1 icon=chevron-right |  |
| 1 | frame | 条 11 月 18 日 | fill_container×None | padding=16 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | None×None | layout=vertical gap=3 |  |
| 3 | text | 日 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 11 月 18 日 |
| 3 | text | 情境 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 走路、笑 |
| 2 | frame | 右 | None×None | gap=8 |  |
| 3 | text | 次 | None×None | fontSize=14 fontWeight=600 fill=#B45309 | 4 次 |
| 3 | icon | 箭头 | 15×15 | fill=#CBD5E1 icon=chevron-right |  |
| 1 | frame | 条 11 月 16 日 | fill_container×None | padding=16 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | None×None | layout=vertical gap=3 |  |
| 3 | text | 日 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 11 月 16 日 |
| 3 | text | 情境 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | — |
| 2 | frame | 右 | None×None | gap=8 |  |
| 3 | text | 次 | None×None | fontSize=14 fontWeight=600 fill=#94A3B8 | 0 次 |
| 3 | icon | 箭头 | 15×15 | fill=#CBD5E1 icon=chevron-right |  |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | text | 说明 | fill_container×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 日记只存在本机，不会上传，也不会用于任何评估结论 |

---

## L3-01　心率曲线详情

> 三级页：从 P04 波形区点入。全屏大图 + 时间轴缩放。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 心率曲线 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=14 padding=[8, 22, 22, 22] |  |
| 1 | frame | 读数头 | fill_container×None |  |  |
| 2 | frame | 左 | None×None | layout=vertical gap=2 |  |
| 3 | text | 标 | None×None | fontSize=11 fontWeight=600 fill=#94A3B8 | 当前心率 |
| 3 | frame | 行 | None×None | gap=6 |  |
| 4 | text | 值 | None×None | fontSize=44 fontWeight=600 fill=#E11D48 | 72 |
| 4 | text | 单 | None×None | fontSize=13 fontWeight=normal fill=#94A3B8 | bpm |
| 2 | frame | 右 | None×None | layout=vertical gap=2 |  |
| 3 | text | 标 | None×None | fontSize=11 fontWeight=600 fill=#94A3B8 | 区间 |
| 3 | text | 值 | None×None | fontSize=15 fontWeight=600 fill=#0F1A24 | 68 – 76 |
| 1 | frame | 大图卡 | fill_container×280 | layout=none cornerRadius=16 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | rectangle | 网格 | 424×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 424×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 424×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 424×1 | fill=#F1F5F9 |  |
| 2 | rectangle | 网格 | 424×1 | fill=#F1F5F9 |  |
| 2 | text | 上限 | None×None | fontSize=9 fontWeight=normal fill=#CBD5E1 | 80 |
| 2 | text | 下限 | None×None | fontSize=9 fontWeight=normal fill=#CBD5E1 | 60 |
| 2 | path | 曲线 | 396×240 | fill=#00000000 stroke=#E11D48 |  |
| 2 | rectangle | 游标 | 2×240 | fill=#0F1A2466 |  |
| 2 | ellipse | 游标点 | 14×14 | fill=#FFFFFF stroke=#E11D48 |  |
| 1 | frame | 时间范围 | fill_container×None | gap=8 |  |
| 2 | frame | 范围 30 秒 | fill_container×40 | layout=vertical cornerRadius=20 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 30 秒 |
| 2 | frame | 范围 1 分钟 | fill_container×40 | layout=vertical cornerRadius=20 fill=#0F1A24 stroke=#0F1A24 |  |
| 3 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#FFFFFF | 1 分钟 |
| 2 | frame | 范围 全程 | fill_container×40 | layout=vertical cornerRadius=20 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#475569 | 全程 |
| 1 | frame | 统 最高 | fill_container×None | padding=[12, 16] cornerRadius=10 fill=#F8FAFC |  |
| 2 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 最高 |
| 2 | text | v | None×None | fontSize=13 fontWeight=600 fill=#E11D48 | 76 bpm |
| 1 | frame | 统 最低 | fill_container×None | padding=[12, 16] cornerRadius=10 fill=#F8FAFC |  |
| 2 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 最低 |
| 2 | text | v | None×None | fontSize=13 fontWeight=600 fill=#E11D48 | 68 bpm |
| 1 | frame | 统 平均 | fill_container×None | padding=[12, 16] cornerRadius=10 fill=#F8FAFC |  |
| 2 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 平均 |
| 2 | text | v | None×None | fontSize=13 fontWeight=600 fill=#0F1A24 | 71 bpm |
| 1 | frame | 统 数据覆盖 | fill_container×None | padding=[12, 16] cornerRadius=10 fill=#F8FAFC |  |
| 2 | text | k | None×None | fontSize=13 fontWeight=normal fill=#64748B | 数据覆盖 |
| 2 | text | v | None×None | fontSize=13 fontWeight=600 fill=#047857 | 96 % |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | text | 免责 | fill_container×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 心率反映身体状态，不代表盆底肌收缩质量 |

---

## L3-02　提醒时间选择

> 三级页：从 L2-08 点时间进入。滚轮选择，不用键盘。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 提醒时间 |
| 1 | text | 右 | None×None | fontSize=15 fontWeight=700 fill=#0F766E | 完成 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[8, 22, 22, 22] |  |
| 1 | text | 提示 | fill_container×None | fontSize=14 fontWeight=normal fill=#64748B | 选择一个你每天方便的时间 |
| 1 | frame | 滚轮卡 | fill_container×260 | cornerRadius=16 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 列 时 | fill_container×None | layout=vertical |  |
| 3 | frame | 项 18 | fill_container×52 | layout=vertical |  |
| 4 | text | 字 | None×None | fontSize=21 fontWeight=normal fill=#CBD5E1 | 18 |
| 3 | frame | 项 19 | fill_container×52 | layout=vertical |  |
| 4 | text | 字 | None×None | fontSize=21 fontWeight=normal fill=#CBD5E1 | 19 |
| 3 | frame | 项 20 | fill_container×52 | layout=vertical fill=#F1F5F9 |  |
| 4 | text | 字 | None×None | fontSize=30 fontWeight=600 fill=#0F1A24 | 20 |
| 3 | frame | 项 21 | fill_container×52 | layout=vertical |  |
| 4 | text | 字 | None×None | fontSize=21 fontWeight=normal fill=#CBD5E1 | 21 |
| 3 | frame | 项 22 | fill_container×52 | layout=vertical |  |
| 4 | text | 字 | None×None | fontSize=21 fontWeight=normal fill=#CBD5E1 | 22 |
| 2 | frame | 列 分 | fill_container×None | layout=vertical |  |
| 3 | frame | 项 00 | fill_container×52 | layout=vertical |  |
| 4 | text | 字 | None×None | fontSize=21 fontWeight=normal fill=#CBD5E1 | 00 |
| 3 | frame | 项 15 | fill_container×52 | layout=vertical fill=#F1F5F9 |  |
| 4 | text | 字 | None×None | fontSize=30 fontWeight=600 fill=#0F1A24 | 15 |
| 3 | frame | 项 30 | fill_container×52 | layout=vertical |  |
| 4 | text | 字 | None×None | fontSize=21 fontWeight=normal fill=#CBD5E1 | 30 |
| 3 | frame | 项 45 | fill_container×52 | layout=vertical |  |
| 4 | text | 字 | None×None | fontSize=21 fontWeight=normal fill=#CBD5E1 | 45 |
| 1 | text | 注 | fill_container×None | fontSize=12 fontWeight=normal fill=#94A3B8 | 已选 20:15。通知可能有几分钟偏差。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 确认 | fill_container×54 | layout=vertical cornerRadius=27 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 设定为 20:15 |

---

## L3-03　全屏月历

> 三级页：从 P06 日历点展开。可翻月、点单日看详情。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 2026 年 11 月 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=12 padding=[8, 18, 22, 18] |  |
| 1 | frame | 月份切换 | fill_container×None |  |  |
| 2 | icon | 上 | 22×22 | fill=#0F1A24 icon=chevron-left |  |
| 2 | text | 月 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 2026 年 11 月 |
| 2 | icon | 下 | 22×22 | fill=#0F1A24 icon=chevron-right |  |
| 1 | frame | 星期 | fill_container×None | padding=[8, 0] |  |
| 2 | frame | w | fill_container×None | layout=vertical |  |
| 3 | text | 字 | None×None | fontSize=12 fontWeight=600 fill=#94A3B8 | 一 |
| 2 | frame | w | fill_container×None | layout=vertical |  |
| 3 | text | 字 | None×None | fontSize=12 fontWeight=600 fill=#94A3B8 | 二 |
| 2 | frame | w | fill_container×None | layout=vertical |  |
| 3 | text | 字 | None×None | fontSize=12 fontWeight=600 fill=#94A3B8 | 三 |
| 2 | frame | w | fill_container×None | layout=vertical |  |
| 3 | text | 字 | None×None | fontSize=12 fontWeight=600 fill=#94A3B8 | 四 |
| 2 | frame | w | fill_container×None | layout=vertical |  |
| 3 | text | 字 | None×None | fontSize=12 fontWeight=600 fill=#94A3B8 | 五 |
| 2 | frame | w | fill_container×None | layout=vertical |  |
| 3 | text | 字 | None×None | fontSize=12 fontWeight=600 fill=#94A3B8 | 六 |
| 2 | frame | w | fill_container×None | layout=vertical |  |
| 3 | text | 字 | None×None | fontSize=12 fontWeight=600 fill=#94A3B8 | 日 |
| 1 | frame | 日期格 | fill_container×None | layout=vertical gap=4 |  |
| 2 | frame | 周0 | fill_container×None | gap=4 |  |
| 3 | frame | d0 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 1 |
| 3 | frame | d1 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 2 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 3 | frame | d2 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 3 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 3 | frame | d3 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 4 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 3 | frame | d4 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 5 |
| 3 | frame | d5 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 6 |
| 3 | frame | d6 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 7 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 2 | frame | 周1 | fill_container×None | gap=4 |  |
| 3 | frame | d7 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 8 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 3 | frame | d8 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 9 |
| 3 | frame | d9 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 10 |
| 3 | frame | d10 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 11 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 3 | frame | d11 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 12 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 3 | frame | d12 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 13 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 3 | frame | d13 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 14 |
| 2 | frame | 周2 | fill_container×None | gap=4 |  |
| 3 | frame | d14 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 15 |
| 3 | frame | d15 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 16 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 3 | frame | d16 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 17 |
| 3 | frame | d17 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 18 |
| 3 | frame | d18 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 19 |
| 3 | frame | d19 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#0F1A24 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=700 fill=#FFFFFF | 20 |
| 4 | ellipse | 点 | 5×5 | fill=#5EEAD4 |  |
| 3 | frame | d20 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 21 |
| 4 | ellipse | 点 | 5×5 | fill=#047857 |  |
| 2 | frame | 周3 | fill_container×None | gap=4 |  |
| 3 | frame | d21 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 22 |
| 3 | frame | d22 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 23 |
| 3 | frame | d23 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 24 |
| 3 | frame | d24 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 25 |
| 3 | frame | d25 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 26 |
| 3 | frame | d26 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 27 |
| 3 | frame | d27 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 28 |
| 2 | frame | 周4 | fill_container×None | gap=4 |  |
| 3 | frame | d28 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 29 |
| 3 | frame | d29 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 4 | text | n | None×None | fontSize=13 fontWeight=normal fill=#475569 | 30 |
| 3 | frame | d30 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 3 | frame | d31 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 3 | frame | d32 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 3 | frame | d33 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 3 | frame | d34 | fill_container×56 | layout=vertical gap=3 cornerRadius=10 fill=#FFFFFF stroke=#F1F5F9 |  |
| 1 | frame | 图例 | fill_container×None | gap=16 |  |
| 2 | frame | 图 有练习 | None×None | gap=6 |  |
| 3 | ellipse | 点 | 8×8 | fill=#047857 |  |
| 3 | text | 字 | None×None | fontSize=11 fontWeight=normal fill=#64748B | 有练习 |
| 2 | frame | 图 今天 | None×None | gap=6 |  |
| 3 | ellipse | 点 | 8×8 | fill=#0F1A24 |  |
| 3 | text | 字 | None×None | fontSize=11 fontWeight=normal fill=#64748B | 今天 |
| 2 | frame | 图 无记录 | None×None | gap=6 |  |
| 3 | ellipse | 点 | 8×8 | fill=#E2E8F0 |  |
| 3 | text | 字 | None×None | fontSize=11 fontWeight=normal fill=#64748B | 无记录 |
| 1 | text | 小标题 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 本月小结 |
| 1 | frame | 小结 | fill_container×None | gap=10 |  |
| 2 | frame | 小 完成 | fill_container×70 | layout=vertical gap=1 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 完成 |
| 3 | text | 值 | None×None | fontSize=16 fontWeight=600 fill=#0F1A24 | 12 天 |
| 2 | frame | 小 累计 | fill_container×70 | layout=vertical gap=1 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 累计 |
| 3 | text | 值 | None×None | fontSize=16 fontWeight=600 fill=#0F1A24 | 38:20 |
| 2 | frame | 小 平均 | fill_container×70 | layout=vertical gap=1 cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 3 | text | 标签 | None×None | fontSize=10 fontWeight=normal fill=#94A3B8 | 平均 |
| 3 | text | 值 | None×None | fontSize=16 fontWeight=600 fill=#0F1A24 | 8 次 |
| 1 | text | 提示 | fill_container×None | fontSize=12 fontWeight=normal fill=#94A3B8 | 点日期可看当天训练详情 |

---

## L3-04　日记单日详情

> 三级页：从 L2-09 点某天进入。可修改或删除当天记录。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 今天 |
| 1 | text | 右 | None×None | fontSize=14 fontWeight=600 fill=#0F766E | 删除 |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[8, 22, 22, 22] |  |
| 1 | text | 日期 | fill_container×None | fontSize=13 fontWeight=normal fill=#64748B | 2026 年 11 月 21 日　周三 |
| 1 | frame | 频次卡 | fill_container×None | layout=vertical gap=10 padding=18 cornerRadius=14 fill=#F8FAFC |  |
| 2 | text | 标 | None×None | fontSize=13 fontWeight=700 fill=#64748B | 漏尿次数 |
| 2 | text | 值 | None×None | fontSize=34 fontWeight=600 fill=#B45309 | 2 次 |
| 1 | text | 小标题 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 当时情境 |
| 1 | frame | 标签行 | fill_container×None | gap=8 |  |
| 2 | frame | 标签 咳啾 | None×40 | padding=[0, 16] cornerRadius=20 fill=#E7F6EC stroke=#047857 |  |
| 3 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#065F46 | 咳啾 |
| 2 | frame | 标签 提重物 | None×40 | padding=[0, 16] cornerRadius=20 fill=#E7F6EC stroke=#047857 |  |
| 3 | text | 字 | None×None | fontSize=13 fontWeight=600 fill=#065F46 | 提重物 |
| 1 | text | 小标题2 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 当天训练 |
| 1 | frame | 训练 晚上 20:14 | fill_container×None | padding=[14, 16] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | None×None | layout=vertical gap=2 |  |
| 3 | text | 时 | None×None | fontSize=13 fontWeight=600 fill=#0F1A24 | 晚上 20:14 |
| 3 | text | 数 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 8 次　·　3:12　·　平均心率 71 |
| 2 | icon | 箭头 | 15×15 | fill=#CBD5E1 icon=chevron-right |  |
| 1 | frame | 训练 下午 15:30 | fill_container×None | padding=[14, 16] cornerRadius=12 fill=#FFFFFF stroke=#E2E8F0 |  |
| 2 | frame | 左 | None×None | layout=vertical gap=2 |  |
| 3 | text | 时 | None×None | fontSize=13 fontWeight=600 fill=#0F1A24 | 下午 15:30 |
| 3 | text | 数 | None×None | fontSize=11 fontWeight=normal fill=#94A3B8 | 6 次　·　2:28（提前结束） |
| 2 | icon | 箭头 | 15×15 | fill=#CBD5E1 icon=chevron-right |  |
| 1 | text | 小标题3 | fill_container×None | fontSize=12 fontWeight=700 fill=#94A3B8 | 备注 |
| 1 | frame | 备注框 | fill_container×96 | layout=vertical padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | text | 字 | fill_container×None | fontSize=13 fontWeight=normal fill=#475569 | 今天抱着孩子上楼梯时有一点。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 编辑 | fill_container×52 | gap=8 cornerRadius=26 fill=#0F1A24 |  |
| 2 | icon | 图标 | 15×15 | fill=#FFFFFF icon=pencil |  |
| 2 | text | 字 | None×None | fontSize=16 fontWeight=700 fill=#FFFFFF | 修改这条记录 |

---

## L3-05　阶段说明

> 三级页：训练中点阶段名进入。解释这一阶段要做什么。

| 深度 | 类型 | 名称 | 尺寸 | 属性 | 文本 |
| --- | --- | --- | --- | --- | --- |
| 0 | frame | 状态栏 | fill_container×44 | padding=[0, 22] |  |
| 1 | text | 时间 | None×None | fontSize=13 fontWeight=700 fill=#0F1A24 | 9:41 |
| 0 | frame | 导航栏 | fill_container×52 | padding=[0, 18] |  |
| 1 | icon | 返回 | 23×23 | fill=#0F1A24 icon=chevron-left |  |
| 1 | text | 标题 | None×None | fontSize=17 fontWeight=700 fill=#0F1A24 | 保持阶段 |
| 1 | rectangle | 占位 | 23×1 | fill=#00000000 |  |
| 0 | frame | 内容 | fill_container×fill_container | layout=vertical gap=16 padding=[8, 22, 22, 22] |  |
| 1 | frame | 标题区 | fill_container×None | layout=vertical gap=8 padding=[24, 20] cornerRadius=18 fill=#E7F6EC |  |
| 2 | frame | 图标底 | 46×46 | layout=vertical cornerRadius=13 fill=#FFFFFF |  |
| 3 | icon | 图标 | 23×23 | fill=#047857 icon=wind |  |
| 2 | text | 阶段 | None×None | fontSize=28 fontWeight=700 fill=#065F46 | 保持 |
| 2 | text | 时长 | None×None | fontSize=12 fontWeight=normal fill=#065F46B3 | 持续 5 秒　·　第 3 个阶段 |
| 1 | text | 正文标题 | fill_container×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 这个阶段要做什么 |
| 1 | text | 正文 | fill_container×None | fontSize=15 fontWeight=normal fill=#475569 | 把刚才提起的力保持住，不要继续收紧，也不要掉下去。整个 5 秒里都保持自然呼吸，千万不要憋气。 |
| 1 | text | 要点标题 | fill_container×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 三个要点 |
| 1 | frame | 要点 呼吸不能停 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | ellipse | 点 | 6×6 | fill=#047857 |  |
| 2 | frame | 文 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 主 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 呼吸不能停 |
| 3 | text | 副 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 憋气会让腹压升高，反而不利于盆底 |
| 1 | frame | 要点 力气不要加大 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | ellipse | 点 | 6×6 | fill=#047857 |  |
| 2 | frame | 文 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 主 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 力气不要加大 |
| 3 | text | 副 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 保持同一个力度，不追求越紧越好 |
| 1 | frame | 要点 只在心里数秒 | fill_container×None | gap=12 padding=[14, 16] cornerRadius=12 fill=#F8FAFC |  |
| 2 | ellipse | 点 | 6×6 | fill=#047857 |  |
| 2 | frame | 文 | fill_container×None | layout=vertical gap=2 |  |
| 3 | text | 主 | None×None | fontSize=14 fontWeight=700 fill=#0F1A24 | 只在心里数秒 |
| 3 | text | 副 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 不要靠缩肚子或夹腿来代偿 |
| 1 | frame | 免责 | fill_container×None | gap=10 padding=[14, 16] cornerRadius=12 fill=#F1F5F9 |  |
| 2 | icon | 图标 | 15×15 | fill=#64748B icon=info |  |
| 2 | text | 字 | fill_container×None | fontSize=12 fontWeight=normal fill=#64748B | 本页只解释动作要求，不评估你做得好不好。 |
| 1 | frame | 弹性 | fill_container×fill_container |  |  |
| 1 | frame | 回到训练 | fill_container×54 | layout=vertical cornerRadius=27 fill=#0F1A24 |  |
| 2 | text | 字 | None×None | fontSize=17 fontWeight=700 fill=#FFFFFF | 回到训练 |
