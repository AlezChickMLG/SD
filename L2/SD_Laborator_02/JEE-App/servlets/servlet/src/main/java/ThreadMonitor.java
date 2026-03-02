import ejb.StudentEntity;

import javax.persistence.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class ThreadMonitor extends Thread {
    private final Integer ageInf = 10, ageSup = 30;
    private final Float meanInf = 5.0f, meanSup = 10.0f;

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

            System.out.println("[ThreadMonitor]: Interogare query varsta");
            TypedQuery<Integer> query = em.createQuery("select student.varsta from StudentEntity student", Integer.class);
            List<Integer> results = query.getResultList();
            for (Integer age : results) {
                if (age < ageInf || age > ageSup) {
                    System.out.println("[ThreadMonitor]: Alerta -> campul varsta a iesit din interval: " + age);
                }
            }

            System.out.println("[ThreadMonitor]: Interogare query medie");
            TypedQuery<Float> newQuery = em.createQuery("select student.medie from StudentEntity student", Float.class);
            List<Float> resultsFloat = newQuery.getResultList();
            for (Float mean: resultsFloat) {
                if (mean < meanInf || mean > meanSup) {
                    System.out.println("[ThreadMonitor]: Alerta -> campul medie a iesit din interval: " + mean);
                }
            }
        }
    }
}
