package com.copypathtool

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.awt.datatransfer.DataFlavor
import java.io.File

/**
 * 内置自检（默认休眠，不影响正常使用）。
 *
 * 仅当以 `-Dcopypath.selftest`（任意值）启动 IDE 时激活：
 * 项目打开后自动打开项目内指定文件（`-Dcopypath.selftest.file=<项目内文件名>`），
 * 依次执行扁平菜单中 File / Code Block 各菜单项在不同选中状态下的复制动作，
 * 把菜单项可用状态与剪贴板实际内容写入报告文件（IDE 日志目录下 copypath-selftest-report.txt），
 * 用于在真实 IDE（PhpStorm / WebStorm 等）中做端到端验证。
 *
 * 安全约定：报告固定写入平台日志目录；目标文件仅按“文件名”在项目目录内查找，
 * 不解析任何外部传入路径。
 */
class SelfTestActivity : ProjectActivity {

    override suspend fun execute(project: Project) {
        if (System.getProperty("copypath.selftest") == null) return
        val fileName = System.getProperty("copypath.selftest.file")?.takeIf { it.isNotBlank() }
        delay(4000) // 等待项目完成初始化
        runSelfTest(project, fileName)
    }

    /** 在项目目录内按文件名查找（仅名称匹配，逐级 findChild，不做任何路径解析） */
    private fun findInProject(base: VirtualFile, fileName: String, depth: Int = 0): VirtualFile? {
        if (depth > 6) return null
        base.findChild(fileName)?.let { if (!it.isDirectory) return it }
        for (child in base.children) {
            if (child.isDirectory) {
                findInProject(child, fileName, depth + 1)?.let { return it }
            }
        }
        return null
    }

    private suspend fun runSelfTest(project: Project, fileName: String?) {
        val report = StringBuilder()
        try {
            report.append("platform=").append(ApplicationInfo.getInstance().build.asString()).append('\n')

            val projectDir = project.basePath?.let { LocalFileSystem.getInstance().findFileByPath(it) }
            val vfsFile = if (projectDir != null && fileName != null) {
                withContext(Dispatchers.EDT) { findInProject(projectDir, fileName) }
            } else {
                null
            }

            // 在 EDT 上打开文件并等待编辑器就绪（最多重试 10 秒）
            var editor: Editor? = null
            if (vfsFile != null) {
                repeat(10) {
                    editor = withContext(Dispatchers.EDT) {
                        val descriptor = com.intellij.openapi.fileEditor.OpenFileDescriptor(project, vfsFile)
                        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
                            ?: FileEditorManager.getInstance(project).selectedTextEditor
                    }
                    if (editor != null) return@repeat
                    delay(1000)
                }
            }
            if (vfsFile == null || editor == null) {
                report.append("result=FAIL reason=no_editor file=").append(fileName).append('\n')
                return
            }
            val readyEditor = editor!!
            report.append("file=").append(vfsFile.path).append('\n')

            val topGroup = withContext(Dispatchers.EDT) {
                ActionManager.getInstance().getAction("CopyPathTool.Group") as? CopyPathGroup
            }
            if (topGroup == null || topGroup.childrenCount != 4) {
                report.append("result=FAIL reason=actions_not_registered count=")
                    .append(topGroup?.childrenCount ?: -1).append('\n')
                return
            }
            report.append("register=OK\n")

            withContext(Dispatchers.EDT) {
                fun event() = AnActionEvent(
                    null,
                    DataContext { dataId ->
                        when (dataId) {
                            CommonDataKeys.PROJECT.name -> project
                            CommonDataKeys.EDITOR.name -> readyEditor
                            CommonDataKeys.VIRTUAL_FILE.name -> vfsFile
                            else -> null
                        }
                    },
                    ActionPlaces.EDITOR_POPUP, Presentation(), ActionManager.getInstance(), 0
                )

                fun clipboardText(): String? = try {
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                        .getContents(null)?.getTransferData(DataFlavor.stringFlavor) as? String
                } catch (e: Exception) {
                    null
                }

                fun run(action: CopyPathAction, key: String) {
                    val ae = event()
                    action.update(ae)
                    report.append(key).append("Visible=").append(ae.presentation.isVisible).append('\n')
                    if (!ae.presentation.isVisible) return
                    action.actionPerformed(ae)
                    report.append(key).append('=').append(clipboardText()).append('\n')
                }

                // 场景 1：未选中代码 —— 只显示 File 项，Code Block 项隐藏
                readyEditor.selectionModel.removeSelection()
                run(CopyPathAction(PathKind.DISK, codeBlock = false), "fileDiskNoSel")
                run(CopyPathAction(PathKind.PROJECT, codeBlock = false), "fileProjNoSel")
                run(CopyPathAction(PathKind.DISK, codeBlock = true), "codeBlockDiskNoSel")

                // 场景 2：选中第 2~4 行（0 基下标 1..3）—— 只显示 Code Block 项并附带行号，File 项隐藏
                if (readyEditor.document.lineCount >= 5) {
                    readyEditor.selectionModel.setSelection(
                        readyEditor.document.getLineStartOffset(1),
                        readyEditor.document.getLineEndOffset(3)
                    )
                    run(CopyPathAction(PathKind.DISK, codeBlock = true), "codeBlockDiskSel")
                    run(CopyPathAction(PathKind.PROJECT, codeBlock = true), "codeBlockProjSel")
                    run(CopyPathAction(PathKind.DISK, codeBlock = false), "fileDiskWithSelection")
                }

                report.append("result=OK\n")
            }
        } catch (t: Throwable) {
            report.append("EXCEPTION=").append(t).append('\n')
            report.append("result=FAIL\n")
        } finally {
            try {
                val reportFile = File(PathManager.getLogPath(), "copypath-selftest-report.txt")
                reportFile.parentFile?.mkdirs()
                reportFile.writeText(report.toString(), Charsets.UTF_8)
            } catch (_: Exception) {
            }
        }
    }
}
