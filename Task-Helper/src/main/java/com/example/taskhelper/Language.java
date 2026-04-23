package com.example.taskhelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Scanner;

public class Language {
    JSONSystem fj;
    JSONArray ja;
    JSONObject jo;
    int index = 1;

    void LanguageOption() {
        try {
            loadLanguage();
            printLanguage();
            Scanner sc = new Scanner(System.in);
            String str = "";
            try {
                str = sc.next();
                int i = Integer.parseInt(str);
                if (i > ja.length() || i < 1) {
                    System.err.println("不在语言包范围内\nOut of language package range");
                } else {
                    languageBag(i);
                }
            } catch (NumberFormatException e) {
                languageBag(str);
            }
        } catch (IOException e) {
            System.err.println("语言包解析出错\nLanguage package parsing error");
        }
    }

    Language() {
        try {
            loadLanguage();
        } catch (IOException e) {
            System.err.println("语言包解析出错\nLanguage package parsing error");
        }
    }

    // 加载语言包：优先外部配置，依次 fallback 到 classpath resources
    private void loadLanguage() throws IOException {
        // 优先从外部配置目录读
        fj = new JSONSystem("Language.json");
        try {
            ja = new JSONArray(fj.fileReader());
        } catch (IOException e) {
            // 外部没有，则用 classpath resource 读取（JAR 内置）
            var is = Language.class.getResourceAsStream("/Language.json");
            if (is == null) throw new IOException("Language.json not found in classpath");
            ja = new JSONArray(new String(is.readAllBytes()));
            fj = null; // 读的是内置资源，写的时候用外部路径
        }
        for (int i = 0; i < ja.length(); i++) {
            if (ja.getJSONObject(i).getBoolean("default")) {
                jo = ja.getJSONObject(i);
                index = i;
            }
        }
    }

    // 打印语言种类
    private void printLanguage() {
        for (int i = 0; i < ja.length(); i++) {
            System.out.println(i + 1 + "、" + ja.getJSONObject(i).get("Language"));
        }
    }

    // 选择语言
    private void languageBag(int idx) {
        defaultLanguage(idx);
        System.out.println(jo.get("languageOnTrue") + "-" + jo.get("Language"));
    }

    private void languageBag(String bagName) {
        for (int i = 0; i < ja.length(); i++)
            if (ja.getJSONObject(i).get("Language").equals(bagName)) {
                defaultLanguage(i + 1);
                System.out.println(jo.get("languageOnTrue") + "-" + jo.get("Language"));
                return;
            }
        System.err.println("不在语言包范围内\nOut of language package range");
    }

    private void defaultLanguage(int idx) {
        jo.put("default", false);
        ja.put(this.index, jo);
        jo = ja.getJSONObject(idx - 1);
        this.index = idx - 1;
        jo.put("default", true);
        ja.put(this.index, jo);
        try {
            if (fj != null) {
                fj.write(ja);
            } else {
                // 从 resources 读的，写回外部配置目录
                JSONSystem extFj = new JSONSystem("Language.json");
                extFj.write(ja);
            }
        } catch (IOException e) {
            System.err.println("写入语言配置失败: " + e.getMessage());
        }
    }
}
