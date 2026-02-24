<html xmlns:jsp="http://java.sun.com/JSP/Page">
	<head>
		<title>Informatii student</title>
		<meta charset="UTF-8" />
	</head>
	<body>
		<h3>Informatii student</h3>

		<!-- populare bean cu informatii din cererea HTTP -->
		<jsp:useBean id="studentBean" class="beans.StudentBean" scope="request" />
		<jsp:setProperty name="studentBean" property="nume" value='<%= request.getAttribute("nume") %>'/>
		<jsp:setProperty name="studentBean" property="prenume" value='<%= request.getAttribute("prenume") %>'/>
		<jsp:setProperty name="studentBean" property="varsta" value='<%= request.getAttribute("varsta") %>'/>

		<!-- folosirea bean-ului pentru afisarea informatiilor -->
		<p>Urmatoarele informatii au fost introduse:</p>
		<ul type="bullet">
			<li>Nume: <%=request.getAttribute("nume")%></li>
			<li>Prenume: <jsp:getProperty name="studentBean" property="prenume" /></li>
			<li>Varsta: <jsp:getProperty name="studentBean" property="varsta" /></li>

			<!-- anul nasterii nu face parte din bean, il afisam separat (daca exista) -->
			<li>Anul nasterii: <%
			    Object anNastere = request.getAttribute("anNastere");
			    if (anNastere != null) {
			        out.print(anNastere);
			    } else {
			        out.print("necunoscut");
			    }
			%></li>
		</ul>

				<h3>Formular student</h3>
        		Actualizare student:
        		<form action="./read-student" method="post">
        			Nume vechi: <input type="text" name="numeVechi" />
        			<br />
        			Prenume vechi: <input type="text" name="prenumeVechi" />
        			<br />
                    <br />
                    <br />

        			Nume nou: <input type="text" name="numeNou" />
                    <br />
                    Prenume nou: <input type="text" name="prenumeNou" />
                    <br />
                    Varsta noua: <input type="number" name="varstaNoua" />
                    <br />
                    <br />
                    <button type="submit" name="submit">Trimite</button>
        		</form>
	</body>
</html>