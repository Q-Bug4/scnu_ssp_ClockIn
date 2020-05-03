# -*- coding:utf8 -*-
import time
import os
from urllib import parse
import base64
import tkinter.messagebox as tk
from selenium import webdriver
from selenium.webdriver.support.select import Select
from selenium.webdriver.support.wait import WebDriverWait

driver = webdriver.Firefox()
html_path = r'E:\study\code\py\clock\html.txt'
driver.minimize_window()      # 最小化
wait = WebDriverWait(driver,5,1)    # 等待网页加载
driver.get('https://ssp.scnu.edu.cn/login.aspx')

username = '2018213xxxx'    # 学号
password = '你的密码'       # 密码
p = 'xx省'                  # 省份
location = 'xx市xx区/县'    # 地区

def hasChange(driver):
    '''
    判断打卡内容是否改变
    :param driver: 浏览器对象
    :return: 打卡内容改变返回True
    '''
    newHtml = str(driver.page_source)
    start = newHtml.find('<dt>学号/姓名</dt>')
    end = newHtml.find('<span id="SysErrMsg" class="msgstr"></span>')
    newHtml = newHtml[start:end]
    if not os.path.exists(html_path):
        # 第一次使用或者检测文件不存在
        html_file = open(html_path, 'w')
        html_file.write(newHtml)
        html_file.close()
        return False
    html_file = open(html_path,'r')
    oldHtml = html_file.read()
    html_file.close()
    if newHtml == oldHtml:
        return False
    os.rename(html_path,str(time.strftime('%m月%d日.txt',time.localtime(time.time()))))
    html_file = open(html_path, 'w')
    html_file.write(newHtml)
    html_file.close()
    return True

try:
    # 清空输入框并送入数据
    wait.until(lambda driver: driver.find_element_by_id('log_username'))
    driver.find_element_by_id('log_username').clear()
    driver.find_element_by_id('log_username').send_keys(username)
    time.sleep(0.5)
    driver.find_element_by_id('log_password').clear()
    driver.find_element_by_id('log_password').send_keys(password)
    time.sleep(0.5)

    # 点击登录按钮
    driver.find_element_by_id('logon').click()

    # 跳转到疫情打卡
    wait.until(lambda driver: driver.find_element_by_xpath('/html/body/form/div[2]/div[1]/nav/ul/li[4]/a'))
    driver.find_element_by_xpath('/html/body/form/div[2]/div[1]/nav/ul/li[4]/a').click()
    wait.until(lambda driver: driver.find_element_by_xpath('/html/body/form/div[2]/div[3]/div[2]/div[2]/div/div[1]/ul/li[1]/a'))
    driver.find_element_by_xpath('/html/body/form/div[2]/div[3]/div[2]/div[2]/div/div[1]/ul/li[1]/a').click()

    # 点击开始打卡
    wait.until(lambda driver: driver.find_element_by_xpath('//*[@id="cph_right_ok_submit"]'))
    driver.find_element_by_xpath('//*[@id="cph_right_ok_submit"]').click()
    wait.until(lambda driver: driver.find_element_by_id('cph_right_e_area'))

    # 检测打卡内容是否变更
    if hasChange(driver):
        raise Exception('打卡内容已改变')


    # 打卡内容
    Select(driver.find_element_by_id('cph_right_e_area')).select_by_value(p)
    driver.find_element_by_id('cph_right_e_location').clear()
    driver.find_element_by_id('cph_right_e_location').send_keys(location)
    driver.find_element_by_id('cph_right_e_observation_0').click()
    driver.find_element_by_id('cph_right_e_health_0').click()
    driver.find_element_by_id('cph_right_e_temp').clear()
    driver.find_element_by_id('cph_right_e_temp').send_keys('36.6')
    driver.find_element_by_id('cph_right_e_survey01_0').click()
    time.sleep(0.5)

    # 提交
    driver.find_element_by_id('cph_right_e_submit').click()

except Exception as e:
    tk.showerror('异常',str(e))

finally:
    driver.quit()