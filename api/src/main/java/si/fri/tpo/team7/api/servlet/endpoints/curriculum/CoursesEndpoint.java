package si.fri.tpo.team7.api.servlet.endpoints.curriculum;

import com.kumuluz.ee.cors.annotations.CrossOrigin;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import si.fri.tpo.team7.api.servlet.annotations.Secured;
import si.fri.tpo.team7.beans.curriculum.CoursesBean;
import si.fri.tpo.team7.beans.enrollments.EnrollmentCoursesBean;
import si.fri.tpo.team7.entities.curriculum.Course;
import si.fri.tpo.team7.entities.curriculum.ICourse;
import si.fri.tpo.team7.entities.enrollments.EnrollmentCourse;
import si.fri.tpo.team7.entities.enums.Role;
import si.fri.tpo.team7.beans.users.StudentsBean;
import si.fri.tpo.team7.entities.users.Lecturer;
import si.fri.tpo.team7.entities.users.Student;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;

import org.apache.pdfbox.pdmodel.PDDocument;

@Path("/courses")
@CrossOrigin
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class CoursesEndpoint {

    @Inject
    private CoursesBean coursesBean;

    @Inject
    private EnrollmentCoursesBean enrollmentCoursesBean;

    @GET
    public Response getCourses(){
        return Response.ok(coursesBean.get()).build();
    }

    @GET
    @Secured({Role.LECTURER, Role.ADMIN, Role.CLERK, Role.STUDENT})
    @Path("{id}")
    public Response getCourse(@PathParam("id") int id){
        return Response.ok(coursesBean.get(id)).build();
    }

    @PUT
    @Path("{id}")
    public  Response updateCourse(@PathParam("id") int id, Course course) {
        return Response.ok(coursesBean.update(id, course)).build();
    }

    @GET
    //@Secured({Role.LECTURER, Role.ADMIN, Role.CLERK})
    @Path("{id}/enrollments")
    public Response getCourseEnrollments(@PathParam("id") int id){
        Course course = coursesBean.get(id);
        return Response.ok(course.getEnrollmentCourses()).build();
    }

    @GET
    //@Secured({Role.LECTURER, Role.ADMIN, Role.CLERK})
    @Path("{id}/enrollments/csv")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getCourseEnrollmentsCsv(@PathParam("id") int id){
        Course course = coursesBean.get(id);
        Set<EnrollmentCourse> courses = course.getEnrollmentCourses();
        String csv = "";
        int cnt = 1;
        for (EnrollmentCourse i:courses) {
            Student s = i.getEnrollment().getToken().getStudent();
            csv += cnt+","+s.getEnrollmentNumber()+","+s.getSurname()+","+s.getName()+","+"\n";//TODO: vrsta vpisa
            cnt++;
        }

        return Response.ok(csv).build();
    }

    @GET
    //@Secured({Role.LECTURER, Role.ADMIN, Role.CLERK})
    @Path("{id}/enrollments/pdf")
    @Produces("application/pdf")
    public Response getCourseEnrollmentsPdf(@PathParam("id") int id){
        Course course = coursesBean.get(id);
        Set<EnrollmentCourse> courses = course.getEnrollmentCourses();

        try{
            PDDocument doc = new PDDocument();
            PDPage page = new PDPage();
            doc.addPage(page);

            PDFont font = PDType0Font.load(doc, new File("arial.ttf"));

            try (PDPageContentStream contents = new PDPageContentStream(doc, page))
            {
                contents.beginText();
                contents.setFont(font, 14);
                contents.newLineAtOffset(100, 700);
                contents.showText(course.getName());
                contents.setFont(font, 12);

                boolean first = true;
                String out = "";
                for(Lecturer l:course.getLecturers()){
                    if(!first) out += ",";
                    first = false;
                    out += " "+l.getName()+" "+l.getSurname();
                }
                contents.newLineAtOffset(10, -20);
                contents.showText(out);

                contents.newLineAtOffset(-10, -20);
                int cnt = 1;
                for (EnrollmentCourse i:courses) {
                    Student s = i.getEnrollment().getToken().getStudent();
                    contents.newLineAtOffset(0, -20);
                    contents.showText(cnt+". "+s.getEnrollmentNumber()+" "+s.getSurname()+" "+s.getName());
                    cnt++;
                }
                contents.endText();
            }
            doc.save(Instant.now().getEpochSecond()+".pdf");
        }
        catch(Exception e){
            e.printStackTrace();
        }
        File file = new File(Instant.now().getEpochSecond()+".pdf");

        Response.ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition",
                "attachment; filename=export.pdf");
        return response.build();
    }
}
