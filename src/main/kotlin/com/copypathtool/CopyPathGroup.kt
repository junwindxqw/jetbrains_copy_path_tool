package com.copypathtool

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DefaultActionGroup

/**
 * “Copy Path Tool” 顶级子菜单（扁平二级结构）：
 * 四个可点击的复制动作，复制行为直接绑定在菜单名称上；
 * 菜单项随选中状态动态显隐——未选中只显示两个 File 项，选中代码后只显示两个 Code Block 项。
 */
class CopyPathGroup : DefaultActionGroup() {

    init {
        add(CopyPathAction(PathKind.PROJECT, codeBlock = false))
        add(CopyPathAction(PathKind.DISK, codeBlock = false))
        add(CopyPathAction(PathKind.PROJECT, codeBlock = true))
        add(CopyPathAction(PathKind.DISK, codeBlock = true))
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
        e.presentation.isEnabledAndVisible = editor != null && file != null
    }
}
