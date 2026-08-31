package com.copypathtool

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup

/**
 * 分区标题：置灰、不可点击，仅用于展示。
 * 随选中状态切换显示——未选中代码时显示 File 分区标题，选中后显示 Code Block 分区标题。
 */
internal class SectionTitleAction(private val codeBlock: Boolean, title: String) : AnAction(title) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
        // 分区标题不可点击，无动作
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isVisible = e.getData(CommonDataKeys.VIRTUAL_FILE) != null &&
            editor != null && codeBlock == hasSelection
        e.presentation.isEnabled = false
    }
}

/**
 * “Copy Path Tool” 顶级子菜单（扁平二级结构）：
 * 四个分区标题仅作展示（置灰不可点击），下面各挂三个可点击的前缀复制动作；
 * 分区随选中状态动态显隐——未选中只显示 File 分区，选中代码后只显示 Code Block 分区。
 */
class CopyPathGroup : DefaultActionGroup() {

    init {
        add(SectionTitleAction(codeBlock = false, title = "Copy File Disk Path"))
        PathPrefix.entries.forEach { add(PrefixCopyAction(PathKind.DISK, it, codeBlock = false)) }

        add(SectionTitleAction(codeBlock = false, title = "Copy File Project Path"))
        PathPrefix.entries.forEach { add(PrefixCopyAction(PathKind.PROJECT, it, codeBlock = false)) }

        add(SectionTitleAction(codeBlock = true, title = "Copy Code Block Disk Path"))
        PathPrefix.entries.forEach { add(PrefixCopyAction(PathKind.DISK, it, codeBlock = true)) }

        add(SectionTitleAction(codeBlock = true, title = "Copy Code Block Project Path"))
        PathPrefix.entries.forEach { add(PrefixCopyAction(PathKind.PROJECT, it, codeBlock = true)) }
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = editor != null && file != null
    }
}
