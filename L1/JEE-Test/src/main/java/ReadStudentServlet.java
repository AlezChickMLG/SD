import beans.StudentBean;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Year;

public class ReadStudentServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String nume = request.getParameter("nume");
        String prenume = request.getParameter("prenume");

        if (nume == null || prenume == null) {
            response.sendError(400, "Parametri lipsa");
            return;
        }

        String path = nume + '_' + prenume;

        Path studentPath = AppConfig.getStudentsPath().resolve(Paths.get(path + "/student.xml"));
        File file = studentPath.toFile();

        if (!file.exists()) {
            response.sendError(404, "Nu a fost gasit niciun student serializat pe disc!");
            return;
        }

        XmlMapper xmlMapper = new XmlMapper();
        StudentBean bean = xmlMapper.readValue(file, StudentBean.class);

        request.setAttribute("nume", bean.getNume());
        request.setAttribute("prenume", bean.getPrenume());
        request.setAttribute("varsta", bean.getVarsta());

        // redirectionare date catre pagina de afisare a informatiilor studentului
        request.getRequestDispatcher("./info-student.jsp").forward(request, response);
    }

    //doPostXML
    /*public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // se citesc parametrii din cererea de tip POST
        String numeVechi = request.getParameter("numeVechi");
        String prenumeVechi = request.getParameter("prenumeVechi");
        String numeNou = request.getParameter("numeNou");
        String prenumeNou = request.getParameter("prenumeNou");
        int varsta = Integer.parseInt(request.getParameter("varstaNoua"));

        int anCurent = Year.now().getValue();
        int anNastere = anCurent - varsta;

        // initializare serializator Jackson
        XmlMapper mapper = new XmlMapper();

        // creare bean si populare cu date
        StudentBean bean = new StudentBean();
        bean.setNume(numeNou);
        bean.setPrenume(prenumeNou);
        bean.setVarsta(varsta);

        //Redenumire xml vechi
        String oldPath = numeVechi + '_' + prenumeVechi;
        String newPath = numeNou + '_' + prenumeNou;

        Path oldStudentPath = AppConfig.getStudentsPath().resolve(Paths.get(oldPath + "/student.xml"));
        Path studentPath = AppConfig.getStudentsPath().resolve(Paths.get(newPath + "/student.xml"));

        if (!Files.exists(oldStudentPath)) {
            response.sendError(404, "Studentul" + prenumeVechi + " " + numeVechi + "nu exista");
            return;
        }

        Files.createDirectories(studentPath.getParent());
        Files.move(oldStudentPath, studentPath, StandardCopyOption.REPLACE_EXISTING);
        mapper.writeValue(studentPath.toFile(), bean);

        // se trimit datele primite si anul nasterii catre o alta pagina JSP pentru afisare
        request.setAttribute("nume", numeNou);
        request.setAttribute("prenume", prenumeNou);
        request.setAttribute("varsta", varsta);
        request.setAttribute("anNastere", anNastere);

        request.getRequestDispatcher("info-student.jsp").forward(request, response);
    }*/

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // se citesc parametrii din cererea de tip POST
        String numeVechi = request.getParameter("numeVechi");
        String prenumeVechi = request.getParameter("prenumeVechi");
        String numeNou = request.getParameter("numeNou");
        String prenumeNou = request.getParameter("prenumeNou");
        int varsta = Integer.parseInt(request.getParameter("varstaNoua"));

        int anCurent = Year.now().getValue();
        int anNastere = anCurent - varsta;

        // initializare serializator Jackson
        XmlMapper mapper = new XmlMapper();

        // creare bean si populare cu date
        StudentBean bean = new StudentBean();
        bean.setNume(numeNou);
        bean.setPrenume(prenumeNou);
        bean.setVarsta(varsta);

        //Redenumire xml vechi
        String oldPath = numeVechi + '_' + prenumeVechi;
        String newPath = numeNou + '_' + prenumeNou;

        Path oldStudentPath = AppConfig.getStudentsPath().resolve(Paths.get(oldPath + "/student.xml"));
        Path studentPath = AppConfig.getStudentsPath().resolve(Paths.get(newPath + "/student.xml"));

        if (!Files.exists(oldStudentPath)) {
            response.sendError(404, "Studentul" + prenumeVechi + " " + numeVechi + "nu exista");
            return;
        }

        Files.createDirectories(studentPath.getParent());
        Files.move(oldStudentPath, studentPath, StandardCopyOption.REPLACE_EXISTING);
        mapper.writeValue(studentPath.toFile(), bean);

        // se trimit datele primite si anul nasterii catre o alta pagina JSP pentru afisare
        request.setAttribute("nume", numeNou);
        request.setAttribute("prenume", prenumeNou);
        request.setAttribute("varsta", varsta);
        request.setAttribute("anNastere", anNastere);

        request.getRequestDispatcher("info-student.jsp").forward(request, response);
    }
}
