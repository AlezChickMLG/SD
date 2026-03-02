import ejb.StudentEntity;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class RemoveStudentServlet extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String nume = req.getParameter("nume");
        String prenume = req.getParameter("prenume");

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();;

        EntityTransaction transaction = em.getTransaction();
        transaction.begin();

        em.createQuery("delete from StudentEntity s where s.nume = :nume and s.prenume = :prenume")
                .setParameter("nume", nume)
                .setParameter("prenume", prenume)
                .executeUpdate();

        transaction.commit();

        em.close();
        factory.close();

        resp.setContentType("text/html");
        resp.getWriter().println("Studentul a fost sters din baza de date." +
                "<br /><br /><a href='./'>Inapoi la meniul principal</a>");
    }
}
