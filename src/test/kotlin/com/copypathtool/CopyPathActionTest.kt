package com.copypathtool

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.editor.Document
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.awt.datatransfer.DataFlavor

/**
 * 在真实平台（2024.2.4）上验证插件行为：
 * 扁平菜单结构（4 个可点击动作，复制行为绑定在菜单名称上）、菜单项随选中状态动态显隐、
 * File 项不带行号、Code Block 项附带行号、边界场景。
 */
class CopyPathActionTest : BasePlatformTestCase() {

    private val fileText = """
        package demo;

        public class Main {
            public static void main(String[] args) {
                System.out.println("alpha");
                System.out.println("beta");
                System.out.println("gamma");
            }
        }
    """.trimIndent() + "\n"

    private lateinit var doc: Document

    override fun setUp() {
        super.setUp()
        val psiFile = myFixture.addFileToProject("src/Main.java", fileText)
        myFixture.configureFromExistingVirtualFile(psiFile.virtualFile)
        doc = myFixture.editor.document
    }

    /** 构造模拟编辑器右键菜单场景的 AnActionEvent */
    private fun event(): AnActionEvent {
        val dataContext = DataContext { dataId ->
            when (dataId) {
                CommonDataKeys.PROJECT.name -> project
                CommonDataKeys.EDITOR.name -> myFixture.editor
                CommonDataKeys.VIRTUAL_FILE.name -> myFixture.file.virtualFile
                else -> null
            }
        }
        return AnActionEvent(
            null, dataContext, ActionPlaces.EDITOR_POPUP,
            Presentation(), ActionManager.getInstance(), 0
        )
    }

    private fun clipboardText(): String? =
        CopyPasteManager.getInstance().getContents(DataFlavor.stringFlavor)

    private fun selectLines(startLine: Int, endLine: Int) {
        myFixture.editor.selectionModel.setSelection(
            doc.getLineStartOffset(startLine),
            doc.getLineEndOffset(endLine)
        )
    }

    fun testTopGroupStructure() {
        val top = ActionManager.getInstance().getAction("CopyPathTool.Group")
        assertNotNull("顶级子菜单组未注册", top)
        assertTrue(top is CopyPathGroup)
        assertEquals(4, (top as CopyPathGroup).childrenCount)

        val children = top.getChildren(null)
        assertTrue(children[0] is CopyPathAction)
        assertEquals("Copy File Project Path", children[0].templatePresentation.text)
        assertEquals("Copy File Disk Path", children[1].templatePresentation.text)
        assertEquals("Copy Code Block Project Path", children[2].templatePresentation.text)
        assertEquals("Copy Code Block Disk Path", children[3].templatePresentation.text)
    }

    fun testVisibilityTogglesWithSelection() {
        val fileAction = CopyPathAction(PathKind.DISK, codeBlock = false)
        val codeAction = CopyPathAction(PathKind.DISK, codeBlock = true)

        // 未选中代码：只显示 File 项
        val eFile1 = event()
        fileAction.update(eFile1)
        val eCode1 = event()
        codeAction.update(eCode1)
        assertTrue(eFile1.presentation.isVisible && eFile1.presentation.isEnabled)
        assertFalse("未选中代码时 Code Block 项应隐藏", eCode1.presentation.isVisible)

        // 选中代码：只显示 Code Block 项
        selectLines(4, 6)
        val eFile2 = event()
        fileAction.update(eFile2)
        val eCode2 = event()
        codeAction.update(eCode2)
        assertFalse("选中代码时 File 项应隐藏", eFile2.presentation.isVisible)
        assertTrue(eCode2.presentation.isVisible && eCode2.presentation.isEnabled)
    }

    fun testCopyFileDiskPath() {
        CopyPathAction(PathKind.DISK, codeBlock = false).actionPerformed(event())
        // 磁盘路径统一使用正斜杠（agent 应用对话友好）
        assertEquals(myFixture.file.virtualFile.path, clipboardText())
    }

    fun testCopyFileProjectPath() {
        CopyPathAction(PathKind.PROJECT, codeBlock = false).actionPerformed(event())
        assertEquals("src/Main.java", clipboardText())
    }

    fun testCodeBlockPathsWhenSelected() {
        // 选中第 5~7 行（println alpha/beta/gamma 三行）
        selectLines(4, 6)

        CopyPathAction(PathKind.DISK, codeBlock = true).actionPerformed(event())
        assertEquals(myFixture.file.virtualFile.path + ":5-7", clipboardText())

        CopyPathAction(PathKind.PROJECT, codeBlock = true).actionPerformed(event())
        assertEquals("src/Main.java:5-7", clipboardText())
    }

    fun testSingleLineSelectionSuffix() {
        selectLines(5, 5) // 第 6 行
        CopyPathAction(PathKind.PROJECT, codeBlock = true).actionPerformed(event())
        assertEquals("src/Main.java:6", clipboardText())
    }

    fun testSelectionEndingAtLineStartExcludesEmptyLine() {
        // 从第 5 行行首选到第 6 行行首：最后一行未包含实际内容，应只算第 5 行
        myFixture.editor.selectionModel.setSelection(
            doc.getLineStartOffset(4),
            doc.getLineStartOffset(5)
        )
        CopyPathAction(PathKind.PROJECT, codeBlock = true).actionPerformed(event())
        assertEquals("src/Main.java:5", clipboardText())
    }

    fun testMenuHiddenWithoutEditor() {
        val dataContext = DataContext { dataId ->
            if (dataId == CommonDataKeys.PROJECT.name) project else null
        }
        val e = AnActionEvent(
            null, dataContext, ActionPlaces.EDITOR_POPUP,
            Presentation(), ActionManager.getInstance(), 0
        )
        val group = CopyPathGroup()
        group.update(e)
        assertFalse("没有编辑器时菜单应隐藏", e.presentation.isVisible)
    }
}
