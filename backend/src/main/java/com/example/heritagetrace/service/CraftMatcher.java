package com.example.heritagetrace.service;

/**
 * 工艺匹配规则：工具的适用工艺（tool.craft_type）必须与
 * 项目的分类（project.category）是一路。
 *
 * 两边任一未填写时不参与比较，视为可匹配（兼容历史档案）；
 * 都填写时去空白、忽略大小写后必须完全一致，例如「木雕」凿子
 * 挂到「刺绣」项目名下即判定为对不上。
 */
public final class CraftMatcher {

    private CraftMatcher() {
    }

    public static boolean isCraftMatched(String toolCraftType, String projectCategory) {
        String craft = normalize(toolCraftType);
        String category = normalize(projectCategory);
        if (craft.isEmpty() || category.isEmpty()) {
            return true;
        }
        return craft.equalsIgnoreCase(category);
    }

    /** 对不上时给绑定页/绑定接口看的明确提示 */
    public static String mismatchMessage(String toolCraftType, String projectCategory) {
        String craft = normalize(toolCraftType);
        String category = normalize(projectCategory);
        return "工艺对不上，不能挂上：工具工艺为「" + craft + "」，项目分类为「" + category + "」，两者必须一致";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
