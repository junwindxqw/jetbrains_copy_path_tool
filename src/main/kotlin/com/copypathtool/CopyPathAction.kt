package com.copypathtool

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection

/** 路径种类：磁盘完整路径 / 项目相对路径 */
enum class PathKind { DISK, PROJECT }

/** 路径前缀：无前缀 / @ / #（适配不同 agent 应用的上下文引用约定） */
enum class PathPrefix { NONE, AT, HASH }

/** 路径文本计算的共享工具 */
internal object PathFormats {

    /**
     * 完整磁盘路径，统一使用正斜杠（如 D:/work/demo/src/App.java）。
     * 正斜杠在 AI agent 应用的 Markdown 对话框中不会被转义吞字符，且 Windows 下所有工具链均原样识别。
     */
    fun diskPath(file: VirtualFile): String = file.path

    /** 项目相对路径：优先相对项目根目录（project.basePath），否则取最上层的内容根目录 */
    fun projectPath(project: Project, file: VirtualFile): String {
        val projectDir = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
        projectDir?.let { root -> VfsUtilCore.getRelativePath(file, root, '/')?.let { return it } }

        ProjectRootManager.getInstance(project).contentRoots
            .mapNotNull { root -> VfsUtilCore.getRelativePath(file, root, '/') }
            .maxByOrNull { it.length }
            ?.let { return it }
        return file.name
    }

    /** 选中代码对应的行号后缀，如 “:12” 或 “:12-34” */
    fun lineRangeSuffix(document: Document, selection: SelectionModel): String {
        val start = selection.selectionStart
        val end = selection.selectionEnd
        if (start >= end) return ""

        val startLine = document.getLineNumber(start)
        var endLine = document.getLineNumber(end)
        // 选区恰好结束在某一行的行首时，最后一行并没有实际内容，不计入范围
        if (endLine > startLine && document.getLineStartOffset(endLine) == end) {
            endLine--
        }
        return if (startLine == endLine) ":${startLine + 1}" else ":${startLine + 1}-${endLine + 1}"
    }

    /**
     * 组装最终复制文本：前缀 + 路径。
     * codeBlock = true 时附加所选代码的行号范围；false 时始终复制整个文件路径（不带行号）。
     */
    fun buildText(
        project: Project,
        editor: Editor,
        file: VirtualFile,
        kind: PathKind,
        prefix: PathPrefix,
        codeBlock: Boolean,
    ): String {
        val base = when (kind) {
            PathKind.DISK -> diskPath(file)
            PathKind.PROJECT -> projectPath(project, file)
        }
        val path = if (codeBlock && editor.selectionModel.hasSelection()) {
            base + lineRangeSuffix(editor.document, editor.selectionModel)
        } else {
            base
        }
        return when (prefix) {
            PathPrefix.NONE -> path
            PathPrefix.AT -> "@$path"
            PathPrefix.HASH -> "#$path"
        }
    }
}

/**
 * 可点击的复制动作（二级菜单叶子项）。
 *
 * @param codeBlock true = 「Copy Code Block *」分区：必须选中代码才可用，复制“路径:行号”或“路径:起始行-结束行”；
 *                  false = 「Copy File *」分区：始终复制整个文件路径（不带行号）。
 */
open class PrefixCopyAction(
    private val kind: PathKind,
    private val prefix: PathPrefix,
    private val codeBlock: Boolean,
) : AnAction() {

    init {
        templatePresentation.text = when (prefix) {
            PathPrefix.NONE -> "    - None"
            PathPrefix.AT -> "    - @ Prefix"
            PathPrefix.HASH -> "    - # Prefix"
        }
        templatePresentation.description = when {
            !codeBlock && prefix == PathPrefix.AT -> "Copy file path with @ prefix"
            !codeBlock && prefix == PathPrefix.HASH -> "Copy file path with # prefix"
            !codeBlock -> "Copy file path"
            prefix == PathPrefix.AT -> "Copy code block path with @ prefix (requires selection)"
            prefix == PathPrefix.HASH -> "Copy code block path with # prefix (requires selection)"
            else -> "Copy code block path (requires selection)"
        }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        // 未选中代码只显示 File 分区；选中代码只显示 Code Block 分区
        e.presentation.isVisible = e.project != null && editor != null && file != null &&
            codeBlock == hasSelection
        e.presentation.isEnabled = e.presentation.isVisible &&
            file != null && file.isInLocalFileSystem
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val text = PathFormats.buildText(project, editor, file, kind, prefix, codeBlock)
        CopyPasteManager.getInstance().setContents(StringSelection(text))
    }
}
