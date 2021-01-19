# -*- coding:utf8 -*-
import random
import requests
import re
from requests_toolbelt import MultipartEncoder

# 填写下面信息即可
tem = 37 - random.randint(5, 10) / 10  # 正常温度：36° - 36.5°
user = '2018xxxxx'         # 你的学号
password = 'password'      # 你的密码
location = 'xx省xx市xx县'      # 详细位置(xx省xx市xx区/县)
travel = 'xx省xx市'
# [可选]健康码、行程卡
health_code = None
travel_code = None
# health_code = open('xxxx', 'rb')
# travel_code = open('xxxx', 'rb')    
# 利用Server酱(http://sc.ftqq.com/3.version)将打卡结果推送到微信
# [YOUR_SCKEY]需要替换为你自己的SCKEY
push_url = 'http://sc.ftqq.com/[YOUR_SCKEY].send'

def clockin():
    try:

        # 抓取网页上的__VIEWSTATE 等参数
        def get__(target, html):
            '''
            抓取网页上的__VIEWSTATE 等参数用于post
            :param target: 要抓取的参数名
            :param html:   抓取参数所在的html文本
            :return: 返回参数的值
            '''
            text = 'id="' + target + '" value="'
            res = re.search(text + '[/+=\w]+"', html)  # 查找含有类似于id="__VIEWSTATE" value="/wsdijd...."的字段
            start = res.span()[0] + len(text)
            end = res.span()[1] - 1
            return html[start:end]  # 截取参数内容返回

        # 登录Requests Header
        login_head = {
            'Origin': 'https://ssp.scnu.edu.cn',
            'Referer': 'https://ssp.scnu.edu.cn/login.aspx',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36'
        }

        # 打卡Requests Header
        clock_head = {
            'Origin': 'https://ssp.scnu.edu.cn',
            'Referer': '',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.163 Safari/537.36',
        }

        # 登录界面
        s = requests.session()
        login_url = 'https://ssp.scnu.edu.cn/login.aspx'  # 登录url
        login = s.get(login_url)
        login_html = login.text

        # 登录Requests  Data
        login_data = {
            # '__VIEWSTATEGENERATOR': 'C2EE9ABB',
            '__EVENTTARGET': 'logon',
            '__VIEWSTATE': get__('__VIEWSTATE', login_html),
            '__EVENTVALIDATION': get__('__EVENTVALIDATION', login_html),
            'log_username': user,
            'log_password': password
        }

        # 向登录页面post数据
        res = s.post(url=login_url, data=login_data, headers=login_head)

        # 利用登录后网页源代码查找key
        # 因为打卡页面上参数key每次登录都会变化
        # 找到key就可以定位到打卡页面
        temp_html = res.text
        start = temp_html.find('opt_yq_jkdk.aspx?')
        end = temp_html.find('>健康打卡') - 1
        clock_site = 'https://ssp.scnu.edu.cn/' + temp_html[start:end]  # 截取key获得打卡网址
        clock_head['Referer'] = clock_site  # 用打卡网址更新 Requests Header 的参数

        clock_html = s.get(clock_site).text  # 用于中间Requests Data查找请求参数
        data = MultipartEncoder(
            fields={
                '__VIEWSTATE': (None, get__('__VIEWSTATE', clock_html)),
                '__EVENTVALIDATION': (None, get__('__EVENTVALIDATION', clock_html)),
                'ctl00$cph_right$e_location': (None, location),
                'ctl00$cph_right$e_health$0': (None, '无不适'),
                'ctl00$cph_right$e_temp': (None, str(tem)),
                'ctl00$cph_right$e_travel': (None, travel),
                'ctl00$cph_right$e_describe': (None, ''),
                'ctl00$cph_right$e_submit': (None, '提交保存'),
                'ctl00$cph_right$e_annex':(health_code.name if health_code else '', health_code, 'application/octet-stream'),
                'ctl00$cph_right$e_annex2':(travel_code.name if travel_code else '', travel_code, 'application/octet-stream')
            }
        )
        clock_head['Content-Type']=data.content_type
        # 提交打卡内容
        res2 = s.post(url=clock_site, data=data, headers=clock_head)

        if res2.text.find("alert('打卡成功')") == -1:
            return ('打卡失败', '请查看打卡内容是否变更')
        
        return ('打卡成功','今天打卡额温为' + str(tem) + '°C')
        
    except Exception as e:
        return ('打卡失败', '错误信息: ' + str(e))

result = clockin()
d = {'text': result[0], 'desp': result[1]}
requests.post(push_url , data=d)