import java.util.LinkedList;

/*
 文本数据模型：以“行列表”的形式管理文本内容。
 - 行/列索引均为0起始（0-based）
 - 始终至少存在一行（空文档时为单个空字符串）
*/

public class TextModel {
    // 行容器：每个元素代表一行文本；LinkedList便于整行插入/删除
    private LinkedList<String> lines;

    //构造函数：初始化为包含一条空行的文档
    public TextModel() {
        lines = new LinkedList<>();
        lines.add(""); // 初始空行，保证至少一行
    }

    //获取总行数，返回当前文本的行数（等同于行列表大小）
    public int getTotalLines() {
        return lines.size();
    }

    //插入行
    public void insertLine(int lineIndex, String text) {
        if (lineIndex >= 0 && lineIndex <= lines.size()) {
            lines.add(lineIndex, text);
        }
    }

    //删除行
    public void deleteLine(int lineIndex) {
        if (lineIndex >= 0 && lineIndex < lines.size()) {
            lines.remove(lineIndex);
            if (lines.isEmpty()) {
                lines.add("");
            }
        }
    }

    //获取行内容
    public String getLine(int index) {
        if (index >= 0 && index < lines.size()) {
            return lines.get(index);
        }
        return null;
    }

    //更新行内容
    public void updateLine(int index, String newContent) {
        if (index >= 0 && index < lines.size()) {
            lines.set(index, newContent);
        }
    }

    //从字符串设置内容（视图到模型同步）
    public void setContentFromString(String content) {
        lines.clear();
        String[] contentLines = content.split("\n", -1);
        for (String line : contentLines) {
            lines.add(line);
        }
        if (lines.isEmpty()) {
            lines.add("");
        }
    }

    //将内容转换为字符串（模型到视图同步）
    public String getContentAsString() {
        return String.join("\n", lines);
    }
}
