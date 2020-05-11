package com.example.test_apk

import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

import java.io.File
import java.io.FileNotFoundException
import kotlin.math.tan
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    var filePath = ""


    /**
     * 保存打卡配置
     */
    fun saveData(data: String) {

        try {
            File(filePath).writeText(data)//覆盖原先的文本内容

        } catch (e: Exception) {
            //把异常放在文本框中
            findViewById<EditText>(R.id.tem_text).setText(e.toString())
        }

    }

    /**
     * 读取打卡配置
     */
    fun loadData() {
        try {
            val s = File(filePath).readLines()
            //读取打卡内容
            findViewById<EditText>(R.id.user_text).setText(s[0])
            findViewById<EditText>(R.id.pass_text).setText(s[1])
            findViewById<EditText>(R.id.area_text).setText(s[2])
            findViewById<EditText>(R.id.location_text).setText(s[3])
            findViewById<EditText>(R.id.tem_text).setText(s[4])
            if (s[5] == "1"){
                findViewById<CheckBox>(R.id.tem_checkbox).setChecked(true)
            }

        } catch (e: FileNotFoundException) {
            //文件未找到即是第一次使用
            val dialog = AlertDialog.Builder(this).create()
            dialog.setTitle("帮助")
            dialog.setMessage("欢迎使用。根据输入框提示输入相应内容，点击打卡按钮即可进行打卡。点击打卡后请耐心等候1至3秒即可。\n" +
                    "每次打卡都会保存本次打卡内容，下次就不用手动输入直接愉快打卡啦！")
            dialog.show()
        }catch (e:Exception){

        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        filePath = getExternalFilesDir(null)?.absolutePath + File.separator + "clockIn.dat"     // 打卡内容存储路径

        loadData()      //读取上一次配置

        // 按钮点击事件
        findViewById<Button>(R.id.submit_button).setOnClickListener {
            // 打卡内容
            val user = findViewById<EditText>(R.id.user_text).text.toString()
            val password = findViewById<EditText>(R.id.pass_text).text.toString()
            val area = findViewById<EditText>(R.id.area_text).text.toString()
            val location = findViewById<EditText>(R.id.location_text).text.toString()
            var tem = findViewById<EditText>(R.id.tem_text).text.toString()
            var rand_tem = "0"
            if (findViewById<CheckBox>(R.id.tem_checkbox).isChecked){
                //随机额温
                rand_tem = "1"
                tem = (360..365).random().div(10.0).toString()
                findViewById<EditText>(R.id.tem_text).setText(tem)
            }
            // 存储本次打卡配置
            val data = user + "\n" + password + "\n" + area + "\n" + location + "\n" + tem + "\n" + rand_tem

            //进行打卡
            Thread {
                Looper.prepare()
                val ci = ClockIn()
                // 设置打卡参数
                ci.setUser(user)
                ci.setPassword(password)
                ci.setArea(area)
                ci.setLocation(location)
                ci.setTem(tem)
                val mes = ci.start()
                val dialog = AlertDialog.Builder(this).create()
                if(ci.getSuccess()){
                    saveData(data)
                    dialog.setMessage("打卡成功！打卡额温为：$tem")
                }else{
                    dialog.setMessage("打卡失败：$mes")
                }
                dialog.show()
                Looper.loop()
            }.start()
        }

    }
}
