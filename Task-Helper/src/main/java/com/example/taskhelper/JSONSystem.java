package com.example.taskhelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class JSONSystem {
    // json文件读取
    String path;
    File jsFile;

    // 配置目录
    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.taskhelper";

    // 默认路径
    public JSONSystem() {
        ensureConfigDir();
        path = CONFIG_DIR + "/Task.json";
        jsFile = new File(path);
    }

    // 指定文件路径
    public JSONSystem(String fileName) {
        ensureConfigDir();
        // 如果传入的是完整路径直接用，否则拼到配置目录下
        if (new File(fileName).isAbsolute()) {
            path = fileName;
        } else {
            path = CONFIG_DIR + "/" + fileName;
        }
        jsFile = new File(path);
    }

    // 确保配置目录存在
    private void ensureConfigDir() {
        File dir = new File(CONFIG_DIR);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    // 获取配置目录路径
    public static String getConfigDir() {
        return CONFIG_DIR;
    }

    // 通过 classpath 读取 JAR 内置资源
    public static String readFromClasspath(String fileName) throws IOException {
        var is = JSONSystem.class.getResourceAsStream("/" + fileName);
        if (is == null) throw new IOException("Resource not found: " + fileName);
        return new String(is.readAllBytes());
    }

    // get File to String：优先读外部文件，读不到则用 classpath fallback
    public String fileReader() throws IOException {
        File f = new File(path);
        if (f.exists() && f.isFile()) {
            return Files.readString(Path.of(path));
        }
        // 外部没有，则从 classpath 读取（JAR 内置资源）
        return readFromClasspath(new File(path).getName());
    }

    public JSONArray getJSONArrays() throws IOException {
        return new JSONArray(fileReader());
    }

    // 写入文件（自动创建父目录）
    public void write(JSONArray ja) throws IOException {
        File f = new File(path);
        f.getParentFile().mkdirs();
        Files.writeString(f.toPath(), ja.toString(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public void add(JSONArray ja, String Task) throws IOException {
        Task = taskFormat(Task);
        int id = ja.length() + 1;
        // 优先从外部配置目录读取格式，没有则从 classpath 读
        JSONObject jo;
        try {
            jo = new JSONObject(new JSONSystem("TaskFormat.json").fileReader());
        } catch (IOException e) {
            jo = new JSONObject(readFromClasspath("TaskFormat.json"));
        }
        jo.put("Task", Task);
        jo.put("id", id);
        ja.put(jo);
        write(ja);
    }

    // 按 id 排序（升序/降序）
    public JSONArray sort(JSONArray ja, Boolean asc) {
        List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < ja.length(); i++)
            list.add(ja.getJSONObject(i));
        if (asc) {
            list.sort((a, b) -> Integer.compare(a.getInt("id"), b.getInt("id")));
        } else {
            list.sort((a, b) -> Integer.compare(b.getInt("id"), a.getInt("id")));
        }
        return new JSONArray(list);
    }

    // 打印所有任务
    public void print(JSONArray ja) {
        for (int i = 0; i < ja.length(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            System.out.println("任务" + jo.getInt("id") + " " + jo.get("Task") + "\t" + jo.get("Status"));
        }
    }

    // 按状态打印
    public void print(JSONArray ja, String status) {
        for (int i = 0; i < ja.length(); i++) {
            JSONObject jo = ja.getJSONObject(i);
            if (status.equalsIgnoreCase(jo.get("Status").toString()))
                System.out.println("任务" + jo.getInt("id") + " " + jo.get("Task") + "\t" + jo.get("Status"));
        }
    }

    // 格式化任务字符串（去掉多余引号）
    public String taskFormat(String Task) {
        if (Task.startsWith("\"") && Task.endsWith("\""))
            return Task.substring(1, Task.length() - 1);
        return Task;
    }

    // 删除任务
    public void delete(JSONArray ja, int id) throws IOException {
        for (int i = 0; i < ja.length(); i++) {
            if (ja.getJSONObject(i).getInt("id") == id) {
                System.out.println("删除: " + ja.get(i));
                ja.remove(i);
                break;
            }
        }
        idSort(ja);
        write(ja);
    }

    // 重新排序 id（从 1 开始连续编号）
    public void idSort(JSONArray ja) {
        for (int i = 0; i < ja.length(); i++)
            ja.getJSONObject(i).put("id", i + 1);
    }

    // 更新任务状态
    public void statusPut(JSONArray ja, String status, int id) throws IOException {
        for (int i = 0; i < ja.length(); i++)
            if (ja.getJSONObject(i).getInt("id") == id) {
                ja.getJSONObject(i).put("Status", status);
                break;
            }
        write(ja);
    }
}
