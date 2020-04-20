package com.example.appservidor

//App conexion con el servidor, si hay internet los datos se  envian al servidor y si no, los datos se
//guardan en SQLite y se pueden mostrar los datos almacenados en SQLite. Verifica si el servidor esta
//encendido o esta apagado y si no, guardarlos en SQLite y cuando lo enciendan o tenga internet los
// datos guardados en SQLite los envie automaticamente al servidor

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteOpenHelper
import android.net.ConnectivityManager
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.provider.Settings.Global.getString
import android.provider.Settings.Secure.getString
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.TypedArrayUtils.getString
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.net.URI.create
import java.util.*


class MainActivity : AppCompatActivity() {
    lateinit var nom: EditText
    lateinit var ape: EditText
    lateinit var eda: EditText
    lateinit var but: RadioButton
    lateinit var gua: Button
    lateinit var mos: Button

    //instancia para acceder a la clase de SQLite
    internal var dbHelper = DatabaseHelper(this)

    //Objeto handler
    var handler: Handler? = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nom = findViewById(R.id.nombre)
        ape = findViewById(R.id.apellido)
        eda = findViewById(R.id.edad)
        but = findViewById(R.id.radio)

        gua = findViewById(R.id.guardar)
        mos = findViewById(R.id.mostrar)

        gua.setOnClickListener {
            var nombre = nom.text.toString()
            var apellido = ape.text.toString()
            var edad = eda.text.toString()
            var ver = but.isChecked.toString()

            //Verificar si los campos estan vacios
            if (validarCam().toString()!="vacio") {
                //Obtener acceso a servicio de conectividad de internet de android
                val con = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                //Obteniendola informacion de la red
                val netInfo = con.activeNetworkInfo
                //Evaluando si hay conexion
                if (netInfo != null && netInfo.isConnected) {
                    //println("entra proceso de envio a servidor")
                    val ser = subproceso(this).execute(nombre, apellido, edad, ver)
                    clearEditText()
                } else {
                    //Manda a traer la funcion AccionesSQLite
                    AccionesSQLite(nombre, apellido, edad, ver)
                }
            }
        }
        //Método para ejecutar cada 30 segundos, si tiene conexion a internet
        tarea()
        //mandar a llamar mostrar datos sqlite
        handleViewing()
    }

//Apartado que permite invocar el envio automatico de SQLite al Servidor
//--------------------------------------------------------------------------------------------------
    private val TIEMPO: Int = 30000
    //Funcion que se ejecuta cada 30 segundos para verificar la conectividad a internet
    fun tarea() {
        handler?.postDelayed(object : Runnable {
            override fun run() { //Función a ejecutar
                val con = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                //Obteniendo información de la red
                val netInfo = con.activeNetworkInfo
                //Evaluando si existe la conexion
                if (netInfo != null && netInfo.isConnected) {
                    //showToast("Ejecutando funcion cada 30 segundos, verificando si tiene acceso a internt")
                    showToast("Verificando si hay Internet")
                    runRespaldo()
                } else {
                    showToast("Cuando tenga internet se ejecutara el subproceso para enviar los datos al servidor")
                }
                handler?.postDelayed(this, TIEMPO.toLong())
            }
        }, TIEMPO.toLong())
    }

//Apartado para enviar datos al servidor
//--------------------------------------------------------------------------------------------------
    //funcion en donde se realiza la peticion a servidor
    fun run1 (no: String, ap: String, ed: String, bu: String) {
        var nam = no
        var pel = ap
        var da = ed.toInt()
        var bt = bu.toBoolean()

        val client = OkHttpClient()

        //Creamos un objeto - los que esta en " " se utiliza en el php
        val json = JSONObject()
        json.put("nombre", nam)
        json.put("apellido", pel)
        json.put("edad", da)
        json.put("radio", bt)
        //println(json)

        //Dirección del servidor
        val url = "http://10.5.52.21/android_servidor/conexion_servidor.php"
        //Para indicar el tipo de texto
        val mediaType = "application/json; charset=utf-8".toMediaType()
        //Para formatear y convertir el tipo de datos request aceptado para el body
        val body = json.toString().toRequestBody(mediaType)
        //println(body.toString())

        //Para contruir la petición
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        //Ejecutar la petición
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            println(response.body!!.string())
        }
    }

//Subproceso para ejecutar una petición de red y ejecute un hilo secundario
//--------------------------------------------------------------------------------------------------
    //Se crea una clase interna
    internal class subproceso(param:MainActivity) : AsyncTask<String, Void, String>() {
        //este es el subproceso con asyncTask
        //Se realiza un metodo constructor e inicializa con init{}
        var para: MainActivity

        init {
            this.para = param
        }

        //params recibe el array que se le envio de en onclick al invocar subproceso().execute(//parametros a enviar)
        override fun doInBackground(vararg params: String?): String {
            //Realiza un llamado a la funcion en donde se hace la peticion a servidor Objeto.Metodo(//parametros a
            // enviar segun el tipo que se reciva)
            var ma = (params[0]).toString()
            var ma1 = (params[1]).toString()
            var ma2 = (params[2]).toString()
            var ma3 = (params[3]).toString()
            try {
               val res = para.run1(ma, ma1, ma2, ma3)
                //Thread para que se pedan ejecutar varios subprocesos y no se sature
                //Se muestra el toast si hay internet
                Thread(Runnable {
                    para.runOnUiThread(Runnable {
                        para.showToast("Datos guardados en el servidor")
                    })
                }).start()

                //Excepcion que los datos se guarden en SQLite
                //Si el servidor no responde los datos se guardan en SQLite
            } catch (e: Exception) {
                e.printStackTrace()
                //Thread para que se puedan ejecutar varios subprocesos y no se sature
                Thread(Runnable {
                    para.runOnUiThread(Runnable {
                        para.AccionesSQLite(ma, ma1, ma2, ma3)
                        para.showToast("Datos guardados en SQLite")
                    })
                }).start()
            }
            return " "
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            println(result)
        }
    }

    //Funciones adicionales necesarias y requeridas
//--------------------------------------------------------------------------------------------------
    //funcion para ejecutar toast cuando sea requerido
    fun showToast(text:String) {
        Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
    }

    //funcion para invocar un cuadro de di alogo alert
    fun showDialog(title: String, Message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setCancelable(true)
        builder.setTitle(title)
        builder.setMessage(Message)
        builder.show()
    }

    ///funcion para limpiar las cajas de texto
    fun clearEditText() {
        nom.setText("")
        ape.setText("")
        eda.setText("")
        but.isChecked = false
    }

    //Función para validar campos
    fun validarCam():String {
        if (nom.text.toString().isEmpty() or eda.text.toString().isEmpty()) {
            Toast.makeText(applicationContext, "Ingresar datos en los campos", Toast.LENGTH_SHORT).show()
            return "vacio"
        } else {
            return "lleno"
        }
    }

    //funcion para ejecutar insert en SQLite
    fun AccionesSQLite(no: String, ap: String, ed: String, bo: String) {
        var nom = no
        var ape = ap
        var edad = ed.toInt()
        var bol = bo.toBoolean()
        try {
            val x = dbHelper.insertData(nom, ape, edad, bol)
            clearEditText()
            if (x == kotlin.Unit) {
                //Se muestra el toast si no hay internet
                showToast("Datos guardados en SQLite")
            }

        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            showToast(e.message.toString())
        }
    }

    //funcion para ver los datos guardados
    fun handleViewing() {
        mos.setOnClickListener(
            View.OnClickListener {
                val res = dbHelper.allDate
                if (res.count == 0) {
                    showToast("Datos no encontrados")
                    return@OnClickListener
                }

                val buffer = StringBuffer()
                while (res.moveToNext()) {
                    buffer.append("ID : " + res.getString(0) + "\n")
                    buffer.append("NOMBRE : " + res.getString(1) + "\n")
                    buffer.append("APELLIDO : " + res.getString(2) + "\n")
                    buffer.append("EDAD : " + res.getString(3) + "\n")
                    buffer.append("RADIO : " + res.getString(4) + "\n" + "\n")
                }
                showDialog("Lista de Datos:", buffer.toString())
            }
        )
    }

//Apartado para migrar los datos de SQLite al servidor
//--------------------------------------------------------------------------------------------------
    //Función que manda a traer la consulta SQLite y donde se construyo el array de los datos SQLite y lo que se va a enviar al servidor
    fun runRespaldo() {
        val res = dbHelper.allDate
        val count = res.count
        var arrayDatos = arrayOfNulls<String?>(count)
        var m = 0
        var a = 0
        while (res.moveToNext()) {
            arrayDatos[m] = Datos(res.getString(1), res.getString(2), res.getString(3), res.getString(4)).toString()
            m++
        }
        var arrayDa = Arrays.toString(arrayDatos)
        respaldo(this).execute(arrayDa)
    }

    //funcion en donde se construye y ejecuta la petición para enviar los datos de SQLite al servidor
    fun run2 (arrayDatos: String) {
        val client = OkHttpClient()
        var r = 0
        val json = arrayDatos

        //Dirección del servidor
        val url = "http://10.5.52.21/android_servidor/conexion_servidor4.php"
        //Para indicar el tipo de texto
        val mediaType = "application/json; charset=utf-8".toMediaType()
        //Para formatear y convertir el tipo de datos request aceptado para el body
        val body = json.toString().toRequestBody(mediaType)

        //Para construir la petición
        //println("Para contruir la petición")
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        //Ejecutar la petición
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            //println(response.body!!.string())

            dbHelper.vaciarBD()
        }
    }

    //Subproceso interna para migrar los datos de SQLite al sevidor
    internal class respaldo(paramArray:MainActivity) : AsyncTask<String,Void,String>() {
    var pa: MainActivity

    init {
        this.pa = paramArray
    }

    override fun doInBackground(vararg params:String): String {
        var arrayDat = (params[0]).toString()
        pa.run2(arrayDat)
        return ""
    }

       // override fun onPostExecute(result: String?) {
         //   super.onPostExecute(result)
        //}
    }
}