import ejb.StudentEntity;

import javax.persistence.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class UpdateStudentServlet extends HttpServlet{
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String nume = req.getParameter("nume");
        String prenume = req.getParameter("prenume");

        String numeNou = req.getParameter("numeNou");
        String prenumeNou = req.getParameter("prenumeNou");
        Integer varsta = Integer.parseInt(req.getParameter("varsta"));
        Float medie = Float.parseFloat(req.getParameter("medie"));

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        em.createQuery("update StudentEntity s set s.nume = :numeNou, s.prenume = :prenumeNou, " +
                "s.varsta = :varsta, s.medie = :medie where s.nume = :nume and s.prenume = :prenume")
                .setParameter("numeNou", numeNou)
                .setParameter("prenumeNou", prenumeNou)
                .setParameter("nume", nume)
                .setParameter("prenume", prenume)
                .setParameter("varsta", varsta)
                .setParameter("medie", medie)
                .executeUpdate();

        transaction.commit();

        em.close();
        factory.close();

        resp.setContentType("text/html");
        resp.getWriter().println("Studentul a fost actualizat." +
                "<br /><br /><a href='./'>Inapoi la meniul principal</a>");
    }
}
