package com.example.taskhelper;

import org.json.JSONArray;

import java.io.IOException;
import java.util.*;

public class TaskHelperApplication {
    public static void main(String[] args) {
        System.setErr(System.out);
        //语言包加载
        Language language = new Language();
        //启动时自动设置语言
//        language.LanguageOption();

        JSONSystem fj = new JSONSystem();
        Scanner sc = new Scanner(System.in);
        StringTokenizer st = new StringTokenizer("");
        JSONArray ja = null;
        try {
            System.out.println(language.jo.get("Task get"));
            ja = new JSONArray(fj.fileReader());
        } catch (IOException e) {
            System.err.println(language.jo.get("errIO"));
            st = new StringTokenizer(sc.nextLine());
        }
        String str = "";
        if (ja != null)
            while (true) {
                System.out.print("\r$ task-cli: ");
                str = sc.nextLine().trim();

                // 直接回车则跳过，重新显示提示符
                if (str.isEmpty()) continue;

                st = new StringTokenizer(str);

                String cmd = st.hasMoreTokens() ? st.nextToken() : "";

                switch (cmd.toLowerCase()) {
                    case "option":
                        try {
                            JSONArray optionArray = language.jo.getJSONArray("Option");
                            for (int i = 0; i < optionArray.length(); i++) {
                                System.out.println(optionArray.get(i));
                            }
                            // 从同一行读取子命令
                            if (st.hasMoreTokens()) {
                                OptionMap.valueOf(st.nextToken().toUpperCase()).defaultMethod();
                            }
                        } catch (NullPointerException e) {
                            System.out.println(language.jo.get("NullPointerException"));
                        } catch (IllegalArgumentException e) {
                            System.err.println("未知选项");
                        }
                        break;
                    case "list":
                        if (st.hasMoreTokens()) {
                            String nextCmd = st.nextToken();
                            fj.print(ja, nextCmd);
                        } else {
                            fj.print(ja);
                        }
                        break;
                    case "add":
                        if (st.hasMoreTokens()) {
                            String Task = st.nextToken();
                            if (Task.startsWith("\"") && Task.endsWith("\"")) {
                                Task = Task.substring(1, Task.length() - 1);
                            }
                            try {
                                fj.add(ja, Task);
                                System.out.println(language.jo.get("Task add OK"));
                            } catch (IOException e) {
                                System.err.println(language.jo.get("errIO"));
                            }
                        } else {
                            System.out.println(language.jo.get("Task Request"));
                            String Task = sc.next();
                            try {
                                fj.add(ja,Task);
                            } catch (IOException e) {
                                System.err.println(language.jo.get("errIO"));
                            }
                        }
                        break;
                    case "update":
                        if (st.countTokens()>=2){
                            String idSTring = st.nextToken();
                            StringBuilder Task = new StringBuilder(st.nextToken());
                            while (st.hasMoreTokens())
                                Task.append(st.nextToken());
                            Task = new StringBuilder(fj.taskFormat(Task.toString()));
                            try {
                                int id = Integer.parseInt(idSTring);
                                if (id>=1&&id<=ja.length()){
                                    for (int i = 0;i < ja.length();i++)
                                        if (ja.getJSONObject(i).getInt("id") == id) {
                                            ja.getJSONObject(i).putOpt("Task", Task.toString());
                                            break;
                                        }
                                    fj.write(ja);
                                    System.out.println(language.jo.get("Operation successful"));
                                }
                                else
                                    System.err.println(language.jo.get("ID out"));
                            } catch (NumberFormatException e) {
                                System.err.println(language.jo.get("Format Err"));
                                System.err.println(language.jo.get("NumberFormatException"));
                            } catch (IOException e) {
                                System.err.println(language.jo.get("errIO"));
                            }
                        }else {
                            System.err.println(language.jo.get("Format Err"));
                        }
                        break;
                    case "delete":
                        if (st.hasMoreTokens()){
                            String idStr = st.nextToken();
                            try {
                                int id = Integer.parseInt(idStr);
                                if (id<=ja.length()||id>0){
                                    fj.delete(ja,id);
                                    System.out.println(language.jo.get("Operation successful"));
                                }else
                                    System.err.println(language.jo.get("ID out"));

                            } catch (NumberFormatException e) {
                                System.err.println(language.jo.get("NumberFormatException"));
                            } catch (IOException e) {
                                System.err.println(language.jo.get("errIO"));
                            }
                        }else {
                            System.err.println(language.jo.get("Format delete"));
                        }
                        break;
                    case "mark-in-progress":
                        if (st.hasMoreTokens()){
                            try {
                                int id = Integer.parseInt(st.nextToken());
                                fj.statusPut(ja,"in-progress",id);
                                System.out.println(language.jo.get("Operation successful"));
                            } catch (NumberFormatException e) {
                                System.err.println(language.jo.get("NumberFormatException"));
                            } catch (IOException e) {
                                System.err.println(language.jo.get("errIO"));
                            }
                        }else
                            System.err.println(language.jo.get("Format mark"));
                        break;
                    case "mark-done":
                        if (st.hasMoreTokens()){
                            try {
                                int id = Integer.parseInt(st.nextToken());
                                fj.statusPut(ja,"done",id);
                                System.out.println(language.jo.get("Operation successful"));
                            } catch (NumberFormatException e) {
                                System.err.println(language.jo.get("NumberFormatException"));
                            }catch (IOException e) {
                                System.err.println(language.jo.get("errIO"));
                            }
                        }else
                            System.err.println(language.jo.get("Format mark"));
                        break;
                }

            }


    }

}
