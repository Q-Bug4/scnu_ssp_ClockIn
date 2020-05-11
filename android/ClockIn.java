package com.example.test_apk;

import org.jsoup.Connection;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClockIn {
    final int MES_LOGIN = 1;
    final int MES_MID = 2;
    final int MES_SUBMIT = 3;
    final int MES_KEYLOST = 4;
    final int MES_SUCCESS = 5;

    String loginUrl;
    String submitUrl;
    String pageEncode;
    String user;
    String password;
    String area;
    String location;
    String tem;
    boolean success;

    public boolean getSuccess(){
        return success;
    }
    ClockIn() {
        try {
            loginUrl = "https://ssp.scnu.edu.cn/login.aspx";
            submitUrl = "https://ssp.scnu.edu.cn/";
            pageEncode = "UTF8";

            user = "";
            password = "";
            area = "";
            location = "";
            tem = "";
            success = false;
        } catch (Exception e) {
            showInfo(e.getMessage());
        }
    }

    /**
     * 提示信息
     *
     * @param info 提示文本
     */
    private void showInfo(String info) {
        //TODO 使用弹窗进行提醒
        System.out.println(info);

    }

    /**
     * 读取网页上的__VIEWSTATE等参数
     *
     * @param html   所在页面
     * @param target 读取参数名
     * @return 参数值
     */
    public String get__(String html, String target) {
        String res = "";

        Pattern pattern = Pattern.compile("id=\"" + target + "\" value=\"" + "([/+=\\w]+)\"");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            res = matcher.group(1);
        } else {
            showInfo("找不到" + target);
        }
        return res;
    }

    public int clock() {
        int mes = -1;
        try {
            mes = MES_LOGIN;
            String __EVENTVALIDATION;
            String __VIEWSTATE;
            Connection login_con = Jsoup.connect(loginUrl);
            login_con.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Safari/537.36");
            Response rs = login_con.execute();// 获取响应
            __VIEWSTATE = get__(rs.body(), "__VIEWSTATE");
            __EVENTVALIDATION = get__(rs.body(), "__EVENTVALIDATION");

            // 获取，cooking和表单属性，下面map存放post时的数据
            Map<String, String> login_data = new HashMap<>();
            login_data.put("log_username", user);
            login_data.put("log_password", password);
            login_data.put("__EVENTTARGET", "logon");
            login_data.put("__EVENTVALIDATION", __EVENTVALIDATION);
            login_data.put("__VIEWSTATE", __VIEWSTATE);
            // 设置cookie和post上面的map数据
            Response login = login_con.ignoreContentType(true).method(Method.POST)
                    .data(login_data).cookies(rs.cookies()).execute();
            if (loginUrl.equals(login.url().toString()))
                return mes;
            mes = MES_MID;
            // 找key
            String key = "opt_rc_jkdk.aspx\\?key=[\\w\\d]+&fid=55";
            Pattern P_key = Pattern.compile(key);
            Matcher M_key = P_key.matcher(login.body());
            if (M_key.find()) {
                key = M_key.group(0);
            } else {
                showInfo("找不到key！");
                mes = MES_KEYLOST;   //找不到key即为登录失败
                return mes;
            }
            submitUrl += key;

            // 转到打卡页面
            Connection submit_con = Jsoup.connect(submitUrl);
            submit_con.header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.129 Safari/537.36");
            Response temp = submit_con.cookies(rs.cookies()).execute();// 获取响应
            Map<String, String> temp_data = new HashMap<>();
            temp_data.put("ctl00$cph_right$e_ok", "on");
            temp_data.put("ctl00$cph_right$ok_submit", "开始填报");
            temp_data.put("__EVENTVALIDATION", get__(temp.body(), "__EVENTVALIDATION"));
            temp_data.put("__VIEWSTATE", get__(temp.body(), "__VIEWSTATE"));
            Response submit = submit_con.ignoreContentType(true).method(Method.POST)
                    .data(temp_data).cookies(rs.cookies()).execute();
            mes = MES_SUBMIT;
            // 开始打卡
            Map<String, String> clockin_data = new HashMap<>();
            clockin_data.put("ctl00$cph_right$e_area", area);
            clockin_data.put("ctl00$cph_right$e_location", location);
            clockin_data.put("ctl00$cph_right$e_observation", "无下列情况");
            clockin_data.put("ctl00$cph_right$e_health$0", "无不适");
            clockin_data.put("ctl00$cph_right$e_temp", tem);
            clockin_data.put("ctl00$cph_right$e_survey01", "疫情期间未出国出境");
            clockin_data.put("ctl00$cph_right$e_submit", "提交保存");
            clockin_data.put("__EVENTVALIDATION", get__(submit.body(), "__EVENTVALIDATION"));
            clockin_data.put("__VIEWSTATE", get__(submit.body(), "__VIEWSTATE"));
            Response clockin = null;
            Connection final_con = Jsoup.connect(submitUrl);
            clockin = final_con.ignoreContentType(true).method(Method.POST)
                    .data(clockin_data).cookies(rs.cookies()).execute();
            mes = MES_SUBMIT;
            success = clockin.body().contains("打卡成功");
            if(success)
                mes = MES_SUCCESS;
        } catch (Exception e) {
            return mes;
        }
        return mes;
    }

    public String start() {
        int mes = clock();
        if (mes == MES_SUCCESS)
            return "打卡成功！";
        else if (mes == MES_LOGIN)
            return "登录失败！";
        else if (mes == MES_MID)
            return "转到打卡页面失败！";
        else if(mes==MES_SUBMIT)
            return "打卡内容有误！";
        else if (mes == MES_KEYLOST)
            return "找不到key！";
        return "u cant see me";
    }


    public void setUser(String user) {
        this.user = user;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setTem(String tem) {
        this.tem = tem;
    }
}
