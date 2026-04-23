# Task-CLI

轻量级命令行任务管理工具，支持多语言。

## 运行方式

双击桌面 `Task-CLI.bat` 启动，或在命令行中运行：

```bash
java -jar Task-Helper-0.0.1-SNAPSHOT.jar
```

## 命令列表

| 命令 | 说明 | 示例 |
|------|------|------|
| `add <内容>` | 添加新任务 | `add 完成作业` |
| `list` | 列出所有任务 | `list` |
| `list <状态>` | 按状态筛选任务 | `list todo` |
| `update <id> <内容>` | 更新任务内容 | `update 1 新内容` |
| `delete <id>` | 删除任务 | `delete 1` |
| `mark-in-progress <id>` | 标记为进行中 | `mark-in-progress 1` |
| `mark-done <id>` | 标记为已完成 | `mark-done 1` |
| `option language` | 切换语言 | `option language` |

## 任务状态

- `todo` - 待办
- `in-progress` - 进行中
- `done` - 已完成

## 配置文件

首次运行后，配置数据保存在用户目录下：

```
~/.taskhelper/
├── Task.json       # 任务数据
├── TaskFormat.json # 任务模板
└── Language.json   # 语言配置
```

## 多语言

支持简体中文、English、日本語，通过 `option language` 命令切换。
