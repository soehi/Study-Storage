import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        HttpClient client = HttpClient.newHttpClient();

        while (true) {
            System.out.print("username: ");
            String username = sc.nextLine().trim();

            if (username.isEmpty()) {
                System.out.println("用户名不能为空，请重新输入\n");
                continue;
            }

            System.out.print("Token (不需要直接回车): ");
            String token = sc.nextLine().trim();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/users/" + username + "/events"))
                    .headers("Accept", "application/vnd.github+json",
                            "User-Agent", "Java-App",
                            "X-GitHub-Api-Version", "2022-11-28")
                    .timeout(Duration.ofSeconds(15));

            if (!token.isEmpty()) {
                builder.header("Authorization", "Bearer " + token);
                System.out.println("[已携带 Token]");
            } else {
                System.out.println("[未使用 Token，仅获取公开数据]");
            }

            HttpRequest request = builder.GET().build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 404) {
                    System.out.println("错误：用户 '" + username + "' 不存在\n");
                    continue;
                }
                if (response.statusCode() == 403) {
                    System.out.println("错误：请求被限制，可能需要 Token 或稍后再试\n");
                    continue;
                }
                if (response.statusCode() != 200) {
                    System.out.println("错误：HTTP " + response.statusCode() + "\n");
                    continue;
                }

                // 解析 JSON 字符串
                String body = response.body();
                List<String> behaviors = parseEvents(body);

                System.out.println("\n========== 用户活动记录 ==========");
                System.out.println("用户: " + username);
                System.out.println("共 " + behaviors.size() + " 条活动\n");

                for (String behavior : behaviors) {
                    System.out.println(behavior);
                }
                System.out.println("==================================\n");

            } catch (IOException | InterruptedException e) {
                System.out.println("请求失败: " + e.getMessage() + "\n");
            }
        }
    }

    /**
     * 从 JSON 字符串中解析事件列表
     */
    public static List<String> parseEvents(String json) {
        List<String> behaviors = new ArrayList<>();
        List<String> events = splitJsonArray(json);

        for (int i = 0; i < events.size(); i++) {
            String event = events.get(i);
            String description = parseSingleEvent(event);
            behaviors.add((i + 1) + ". " + description);
        }

        return behaviors;
    }

    /**
     * 将 JSON 数组字符串拆分为单个对象字符串列表
     */
    private static List<String> splitJsonArray(String jsonArray) {
        List<String> list = new ArrayList<>();
        jsonArray = jsonArray.trim();
        if (!jsonArray.startsWith("[") || !jsonArray.endsWith("]")) {
            return list;
        }

        String content = jsonArray.substring(1, jsonArray.length() - 1).trim();
        if (content.isEmpty()) return list;

        int braceCount = 0;
        int start = 0;
        boolean inString = false;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                else if (c == ',' && braceCount == 0) {
                    String obj = content.substring(start, i).trim();
                    if (!obj.isEmpty()) list.add(obj);
                    start = i + 1;
                }
            }
        }

        String lastObj = content.substring(start).trim();
        if (!lastObj.isEmpty()) list.add(lastObj);

        return list;
    }

    /**
     * 解析单条事件 JSON 字符串
     */
    private static String parseSingleEvent(String eventJson) {
        String type = extractStringValue(eventJson, "type");
        String time = extractStringValue(eventJson, "created_at");
        if (time != null && time.length() >= 10) {
            time = time.substring(0, 10);
        }

        String repoName = extractNestedStringValue(eventJson, "repo", "name");

        if (type == null || repoName == null) {
            return String.format("[%s] 未知事件", time != null ? time : "?");
        }

        switch (type) {
            case "WatchEvent":
                return String.format("[%s] Star 了 %s 仓库", time, repoName);

            case "PushEvent": {
                String ref = extractNestedStringValue(eventJson, "payload", "ref");
                String branch = ref != null ? ref.replace("refs/heads/", "") : "unknown";
                return String.format("[%s] 向 %s 仓库的 %s 分支推送了代码", time, repoName, branch);
            }

            case "CreateEvent": {
                String refType = extractNestedStringValue(eventJson, "payload", "ref_type");
                String ref = extractNestedStringValue(eventJson, "payload", "ref");
                if ("branch".equals(refType)) {
                    return String.format("[%s] 在 %s 仓库创建了 %s 分支", time, repoName, ref != null ? ref : "?");
                } else if ("repository".equals(refType)) {
                    return String.format("[%s] 创建了 %s 仓库", time, repoName);
                } else {
                    return String.format("[%s] 在 %s 仓库创建了 %s", time, repoName, refType != null ? refType : "?");
                }
            }

            case "ForkEvent": {
                String forkee = extractDeepNestedValue(eventJson, "payload", "forkee", "full_name");
                return String.format("[%s] Fork 了 %s 仓库到 %s", time, repoName, forkee != null ? forkee : "?");
            }

            case "DeleteEvent": {
                String ref = extractNestedStringValue(eventJson, "payload", "ref");
                String refType = extractNestedStringValue(eventJson, "payload", "ref_type");
                return String.format("[%s] 删除了 %s 仓库的 %s %s", time, repoName,
                        refType != null ? refType : "?", ref != null ? ref : "?");
            }

            case "IssuesEvent": {
                String action = extractNestedStringValue(eventJson, "payload", "action");
                return String.format("[%s] %s 了 %s 仓库的 Issue", time, translateAction(action), repoName);
            }

            case "PullRequestEvent": {
                String action = extractNestedStringValue(eventJson, "payload", "action");
                return String.format("[%s] %s 了 %s 仓库的 Pull Request", time, translateAction(action), repoName);
            }

            case "ReleaseEvent": {
                String action = extractNestedStringValue(eventJson, "payload", "action");
                return String.format("[%s] %s 了 %s 仓库的 Release", time, translateAction(action), repoName);
            }

            default:
                return String.format("[%s] 在 %s 仓库进行了 %s 操作", time, repoName, type);
        }
    }

    /**
     * 从 JSON 字符串中提取字段值（简单实现）
     */
    private static String extractStringValue(String json, String key) {
        String pattern = "\"" + key + "\":\"";
        int start = json.indexOf(pattern);
        if (start == -1) {
            // 尝试数字值
            pattern = "\"" + key + "\":";
            start = json.indexOf(pattern);
            if (start == -1) return null;
            start += pattern.length();
            int end = json.indexOf(",", start);
            if (end == -1) end = json.indexOf("}", start);
            String value = json.substring(start, end).trim();
            return value;
        }
        start += pattern.length();
        int end = json.indexOf("\"", start);
        // 处理转义引号
        while (end > 0 && json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }
        return json.substring(start, end);
    }

    /**
     * 提取嵌套对象的字符串值
     */
    private static String extractNestedStringValue(String json, String objectKey, String fieldKey) {
        String objPattern = "\"" + objectKey + "\":{";
        int objStart = json.indexOf(objPattern);
        if (objStart == -1) return null;
        objStart += objPattern.length() - 1;
        int objEnd = findMatchingBrace(json, objStart);
        if (objEnd == -1) return null;
        String objContent = json.substring(objStart, objEnd + 1);
        return extractStringValue(objContent, fieldKey);
    }

    /**
     * 提取深层嵌套值
     */
    private static String extractDeepNestedValue(String json, String key1, String key2, String key3) {
        String val1 = extractNestedStringValue(json, key1, key2);
        if (val1 == null) return null;
        // val1 应该是包含 key2 的对象，我们需要在其中找 key3
        // 重新定位到 key1 的对象
        String objPattern = "\"" + key1 + "\":{";
        int objStart = json.indexOf(objPattern);
        if (objStart == -1) return null;
        objStart += objPattern.length() - 1;
        int objEnd = findMatchingBrace(json, objStart);
        if (objEnd == -1) return null;
        String objContent = json.substring(objStart, objEnd + 1);

        // 在 objContent 中找 key2 的对象
        String obj2Pattern = "\"" + key2 + "\":{";
        int obj2Start = objContent.indexOf(obj2Pattern);
        if (obj2Start == -1) return null;
        obj2Start += obj2Pattern.length() - 1;
        int obj2End = findMatchingBrace(objContent, obj2Start);
        if (obj2End == -1) return null;
        String obj2Content = objContent.substring(obj2Start, obj2End + 1);

        return extractStringValue(obj2Content, key3);
    }

    /**
     * 找到匹配的右大括号
     */
    private static int findMatchingBrace(String str, int openBracePos) {
        int count = 1;
        boolean inString = false;
        for (int i = openBracePos + 1; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"' && str.charAt(i - 1) != '\\') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') count++;
                else if (c == '}') {
                    count--;
                    if (count == 0) return i;
                }
            }
        }
        return -1;
    }

    /**
     * 翻译英文 action 为中文
     */
    private static String translateAction(String action) {
        if (action == null) return "操作";
        switch (action) {
            case "opened": return "创建/打开";
            case "closed": return "关闭";
            case "reopened": return "重新打开";
            case "edited": return "编辑";
            case "deleted": return "删除";
            case "published": return "发布";
            case "created": return "创建";
            case "started": return "开始";
            default: return action;
        }
    }
}
