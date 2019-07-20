package com.brioa.serviceimplgenerator;

import com.intellij.ide.util.DirectoryUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;

import java.io.File;


public class ServiceImplGenerator extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 获取当前编辑的文件, 通过PsiFile可获得PsiClass, PsiField等对象
        PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
        // 获取当前的project对象
        Project project = e.getProject();
        PsiManager psiManager = psiFile.getManager();
        // 获取当前文件夹对象
        PsiDirectory currentDir = psiFile.getContainingDirectory();
        // 判断是否已经存在impl
        PsiDirectory implDir = currentDir.findSubdirectory("impl");
        if (implDir == null) {
            // 创建文件夹
            implDir = DirectoryUtil.createSubdirectories("impl", currentDir, ".");
        }
        // 父类ServiceName
        String parentServiceName = getFileName(psiFile);
        // 创建一个文件 TestServiceImpl.java
        String className = parentServiceName + "Impl";
        PsiClass psiClass = getClassByName(implDir, className);
        // 设置实现和注解
        GlobalSearchScope searchScope = GlobalSearchScope.allScope(project);
        PsiClass[] psiClasses = PsiShortNamesCache.getInstance(project).getClassesByName(parentServiceName, searchScope);
        // 获取第一个丰富类
        PsiClass parentClass = psiClasses[0];
        // 元素创建起
        PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(project);
        // 导入父类
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                //do something
                // 创建实现类
                PsiJavaCodeReferenceElement ref = psiElementFactory.createClassReferenceElement(parentClass);
                PsiReferenceList psiReferenceList = psiClass.getImplementsList();
                if (!psiReferenceList.textMatches(ref)) {
                    psiReferenceList.add(ref);
                }
                // 添加注解
                PsiModifierList modifierList = psiClass.getModifierList();
                if (!modifierList.textMatches("Service")) {
                    psiClass.getModifierList().addAnnotation("Service");
                }
                // 获取父类方法列表 todo
//                PsiMethod[] parentMethods = parentClass.getAllMethods();
//                // 循环实现父类方法
//                for (PsiMethod method : parentMethods) {
//                    // 判断是否存在
//                    PsiMethod exitMethod = psiClass.findMethodBySignature(method, false);
//                    if (exitMethod == null) {
//                        // 不存在
//                        psiClass.add(method);
//                    } else  {
//                        // 存在，不管
//
//                    }
//                }

            }
        });
        // 格式化代码
        CodeStyleManager.getInstance(project).reformat(psiClass);
        // 打开文件并且聚焦
        FileEditorManager manager = FileEditorManager.getInstance(project);
        manager.openFile(psiClass.getContainingFile().getVirtualFile(), true, true);
    }

    /**
     * 获取文件的名称,没有后缀
     *
     * @param psiFile
     * @return
     */
    public static String getFileName(PsiFile psiFile) {
        String className = getFileNameWithDot(psiFile);
        if (className == null) {
            return null;
        }
        int index = className.indexOf(".");
        if (index == -1) {
            return null;
        }
        return className.substring(0, index);
    }

    /**
     * 获取文件的完整路径
     *
     * @param psiFile
     * @return
     */
    public static String getFileNameWithDot(PsiFile psiFile) {
        if (psiFile == null) {
            return null;
        }
        return psiFile.getVirtualFile().getName();
    }

    /**
     * 根据文件夹和类名获取Class对象，如果不存在则新建
     *
     * @param directory 文件夹
     * @param className 类名，不包含后缀
     * @return
     */
    public static PsiClass getClassByName(PsiDirectory directory, String className) {
        PsiClass psiClass = null;
        // 判断文件是否存在
        File file = new File(directory.getVirtualFile().getCanonicalPath().concat("/")
                .concat(className.concat(".java")));
        if (file.exists()) {
            // 文件存在
            PsiFile psiFile1 = directory.findFile(className + ".java");
            if ((psiFile1 instanceof PsiJavaFile) && ((PsiJavaFile) psiFile1).getClasses().length > 0) {
                psiClass = ((PsiJavaFile) psiFile1).getClasses()[0];
            }

        } else {
            // 文件不存在，新建
            psiClass = JavaDirectoryService.getInstance().createClass(directory, className);
        }
        return psiClass;
    }
}
