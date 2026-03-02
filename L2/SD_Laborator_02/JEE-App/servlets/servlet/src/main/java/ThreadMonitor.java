import ejb.StudentEntity;

import javax.persistence.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class ThreadMonitor extends Thread {
    private final Integer a = 10, b = 30;

    @Override
    public void run() {
        System.out.println("[ThreadMonitor]: Am inceput monitorizarea");

        EntityManagerFactory factory = Persistence.createEntityManagerFactory("bazaDeDateSQLite");
        EntityManager em = factory.createEntityManager();

        while (true) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            System.out.println("[ThreadMonitor]: Interogare query");
            TypedQuery<Integer> query = em.createQuery("select student.varsta from StudentEntity student", Integer.class);
            List<Integer> results = query.getResultList();
            for (Integer age : results) {
                if (age < a || age > b) {
                    System.out.println("[ThreadMonitor]: Alerta -> campul varsta a iesit din interval: " + age);
                }
            }
        }
    }
}
