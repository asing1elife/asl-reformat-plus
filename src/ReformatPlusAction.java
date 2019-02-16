import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReformatPlusAction extends ReformatCodeAction {

    public static final String VUE_STYLE_PREFIX = "(<style).+(lang=\"stylus\").*(>)";
    public static final String VUE_STYLE_SUFFIX = "</style>";

    @Override
    public void actionPerformed(AnActionEvent e) {
        // 执行系统原生格式化
        super.actionPerformed(e);

        // 格式化Stylus代码
        formatStylusCode(e);
    }

    /**
     * 格式化Stylus代码
     * 涉及到 *.styl *.vue
     */
    private void formatStylusCode(AnActionEvent e) {
        // 获取当前文件
        VirtualFile virtualFile = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);

        // 获取文件扩展名
        String name = virtualFile.getExtension();

        if (name == null) {
            return;
        }

        // styl为单独的Stylus样式表
        // vue为内嵌在Vue中的Stylus样式
        if (!name.contains("styl") && !name.contains("vue")) {
            return;
        }

        // 获取当前编辑窗口
        Editor editor = e.getRequiredData(CommonDataKeys.EDITOR);
        // 获取窗口文档对象
        Document document = editor.getDocument();
        // 获取文档内容
        String text = document.getText();

        if (name.contains("styl")) {
            text = generateOptimizationStyleOfStylus(text);
        }

        if (name.contains("vue")) {
            text = optimizationStyleOfVue(text);
        }

        String finalText = text;

        // 将优化后的代码重新写入文档
        WriteCommandAction.runWriteCommandAction(editor.getProject(), () -> document.setText(finalText));
    }

    /**
     * Vue的优化风格
     */
    private String optimizationStyleOfVue(String text) {
        // 首先需要在Vue中找到<style/>代码块
        int start = matcherStartIndexOfVue(text);
        int end = text.indexOf(VUE_STYLE_SUFFIX) + VUE_STYLE_SUFFIX.length();

        // 没有找到符合要求的代码块
        if (start == -1) {
            return text;
        }

        // 获取正确的样式区域
        String effectiveText = text.substring(start, end);

        // 优化风格
        String cleanlyText = generateOptimizationStyleOfStylus(effectiveText);

        // 将之前获取到的样式区域内容替换为优化后的内容
        return text.replace(effectiveText, cleanlyText);
    }

    /**
     * 在Vue文件中通过正则表达式匹配正确的样式表开头
     */
    private int matcherStartIndexOfVue(String text) {
        Pattern pattern = Pattern.compile(VUE_STYLE_PREFIX);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.start();
        }

        return -1;
    }

    /**
     * 匹配并清理所有符合要求的冒号
     * 符合要求：height: 300px
     * 不符合要求: .className:hover
     */
    private String matcherReplaceAllEffectiveColons(String text) {
        Pattern pattern = Pattern.compile("\\w+(:)\\s+");
        Matcher matcher = pattern.matcher(text);

        // 匹配成功
        if (matcher.find()) {
            // 获取当前匹配的内容
            String group = matcher.group();
            // 将匹配内容的多余冒号移除
            String cleanlyGroup = group.replace(":", "");

            // 使用清理后的内容替换之前的内容
            text = text.replace(group, cleanlyGroup);

            // 传入清理后的总内容，继续匹配
            return matcherReplaceAllEffectiveColons(text);
        }

        // 匹配失败，返回清理结果
        return text;
    }

    /**
     * Stylus的通用优化风格
     */
    private String generateOptimizationStyleOfStylus(String text) {
        // 移除冒号
        if (text.contains(":")) {
            // 匹配并清理所有符合要求的冒号
            text = matcherReplaceAllEffectiveColons(text);
        }

        // 移除分号
        if (text.contains(";")) {
            text = text.replace(";", "");
        }

        // 移除左侧大括号
        if (text.contains("{")) {
            text = text.replace("{", "");
        }

        // 移除右侧大括号
        if (text.contains("}")) {
            text = text.replace("}", "");
        }

        return text;
    }
}
