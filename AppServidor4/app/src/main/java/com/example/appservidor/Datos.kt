package com.example.appservidor

import org.json.JSONObject

class Datos (n:String, a:String, e:String, r:String) {

//se crea el metodo constructor y se incializa con init
    var Nombre: String
    var Apellido: String
    var Edad: String
    var Radio: String

    init {
        this.Nombre = n
        this.Apellido = a
        this.Edad = e
        this.Radio = r
    }

    //Se crea una funcion toString
     override fun toString(): String {

        //Se crae un objeto
        val json = JSONObject()
        json.put("nombre", Nombre)
        json.put("apellido", Apellido)
        json.put("edad", Edad)
        json.put("radio", Radio)

        return json.toString()

    }
}