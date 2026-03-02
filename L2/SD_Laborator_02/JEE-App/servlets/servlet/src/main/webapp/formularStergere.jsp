<html xmlns:jsp="http://java.sun.com/JSP/Page">
	<head>
		<title>Formular student</title>
		<meta charset="UTF-8" />
	</head>
	<body>
		<h3>Formular stergere student</h3>
		Introduceti datele despre student:
		<form action="./remove-student" method="post">
			Nume: <input type="text" name="nume" />
			<br />
			Prenume: <input type="text" name="prenume" />
			<br />
			<button type="submit" name="submit">Stergere</button>
		</form>
	</body>
</html>