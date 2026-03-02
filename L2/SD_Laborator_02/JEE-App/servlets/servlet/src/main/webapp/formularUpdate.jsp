<html xmlns:jsp="http://java.sun.com/JSP/Page">
	<head>
		<title>Formular student</title>
		<meta charset="UTF-8" />
	</head>
	<body>
		<h3>Formular actualizare student</h3>
		Introduceti datele despre student:
		<form action="./update-student" method="post">
			Nume: <input type="text" name="nume" />
			<br />
			Prenume: <input type="text" name="prenume" />
			<br />
			<br />
			<br />
			Nume nou: <input type="text" name="numeNou" />
			<br />
			Prenume nou: <input type="text" name="prenumeNou" />
			<br />
			Varsta: <input type="number" name="varsta" />
			<button type="submit" name="submit">Actualizare</button>
		</form>
	</body>
</html>