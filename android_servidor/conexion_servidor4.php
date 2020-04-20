<?php 
//echo "Entrando";
	$H="localhost";
    $U="root";
    $C="root";
    $D="bd_appservidor";

	//recivir objeto/json
	$json=file_get_contents('php://input');
	//decodificarlo
	$data=json_decode($json);
	//echo "reciviendo json";

		
	$bd=mysqli_connect($H, $U, $C, $D);
	var_dump($bd);
	//echo "conexion establecida";

	//Responder
	foreach ($data as $dat) {
		//obtener los valores del array // -> es para acceder
		$da1=$dat->nombre;
		$da2=$dat->apellido;
		$da3=$dat->edad;
		$da4=$dat->radio;
		//echo "Valores ya obtenidos";
		//Se realiza una consulta para verificar si existen los datos
		$sqlVeri = "SELECT c_nombre,c_apellido,c_edad,c_truefalse FROM tbl_datosapp WHERE c_nombre='$da1' AND c_apellido='$da2' AND c_edad='$da3' AND c_truefalse='$da4'";

		$mm=mysqli_query($bd, $sqlVeri);

		if ($mm -> num_rows < 1) {
			$sql="INSERT INTO tbl_datosapp(c_nombre,c_apellido,c_edad,c_truefalse) VALUES ('$da1','$da2',$da3,$da4)";
			$result=mysqli_query($bd, $sql);
			echo "Datos guardados";
		}else{
			echo "Datos Ya Exixten";
		}
	}

 ?>