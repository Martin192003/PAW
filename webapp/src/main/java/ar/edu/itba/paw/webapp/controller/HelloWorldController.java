package ar.edu.itba.paw.webapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import ar.edu.itba.paw.model.Role;
import ar.edu.itba.paw.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import ar.edu.itba.paw.service.UserService;

import java.util.UUID;

@Controller
public class HelloWorldController {

    private UserService us; // Inyección del servicio

    @GetMapping({"", "/"})
    public String HelloWorldController() {
        return "redirect:/clubs";
    }

    @Autowired
    public HelloWorldController(UserService us){
        this.us = us;
    }

    @RequestMapping("/create")
    public ModelAndView create(
            @RequestParam(value = "username") final String username,
            @RequestParam(value = "password") final String password,
            @RequestParam(value = "surname") final String surname,
            @RequestParam(value = "email") final String email,
            @RequestParam(value = "cellphone") final String cellphone,
            @RequestParam(value = "role") final String role
    ) {
        //final String userRole = Role.valueOf(role.toUpperCase()); // Convertir a mayúsculas
        final User u = us.create(username, password, surname, email, cellphone, role);
        return new ModelAndView("redirect:/user?id=" + u.getId());
    }

    @RequestMapping("/user")
    public ModelAndView index(@RequestParam(value = "id", required = true) final UUID id) {
        final ModelAndView mav = new ModelAndView("user");
        mav.addObject("user", us.findById(id));
        return mav;
    }

    /*private final Map<String, Map<String, String>> clubsInformation = initializeClubsInformation();

    @RequestMapping("/")
    public ModelAndView helloWorld() {
        return  new ModelAndView("index");
    }

    @RequestMapping("/boliche")
    public ModelAndView clubDetail(@RequestParam("id") String id) {
        Map<String, String> club = clubsInformation.get(id);

        if (club == null) {
            return new ModelAndView("redirect:/");
        }

        club.put("id", id);

        ModelAndView mav = new ModelAndView("clubDetails");
        mav.addObject("club", club);
        return mav;
    }

    @RequestMapping("/contact")
    public ModelAndView showContactForm(@RequestParam("id") String id) {
        Map<String, String> club = clubsInformation.get(id);

        if (club == null) {
            return new ModelAndView("redirect:/");
        }

        club.put("id", id);

        ModelAndView mav = new ModelAndView("contactForm");
        mav.addObject("clubId", id);
        mav.addObject("clubName", club.get("title"));
        return mav;
    }

    //yo creería que va acá pero no estoy seguro
    //vamos a usar String para el Id para no hacer conversiones innecesarias después para el path
    /*private Map<String, Map<String, String>> initializeClubsInformation() {
        Map<String, Map<String, String>> clubsInfo = new HashMap<>();

        Map<String, String> bali = new HashMap<>();
        bali.put("title", "BALI");
        bali.put("image", "/resources/img/Bali-Imagen.webp");
        bali.put("location", "CABA-Costanera");
        bali.put("musicType", "Electrónica, Reggaetón");
        bali.put("hours", "Jueves a Sábado: 00:00 - 05:30");
        bali.put("rating", "3.7 (258 reseñas)");
        bali.put("price", "Entrada: $2500 - $3500");
        bali.put("audience", "18+ años");
        bali.put("website", "www.bali.com.ar");
        bali.put("phone", "+54 11 4567-8900");
        bali.put("description", "Bali Costanera es uno de los boliches más populares de Buenos Aires, ubicado en la Costanera Norte. Con una decoración inspirada en la isla de Bali, el lugar ofrece un ambiente único para disfrutar de la noche porteña. Cuenta con varios ambientes, cada uno con su propio estilo musical, desde electrónica hasta reggaetón y pop.");
        clubsInfo.put("1", bali);

        Map<String, String> clubOne = new HashMap<>();
        clubOne.put("title", "CLUB ONE");
        clubOne.put("image", "/resources/img/Bali-Imagen.webp");
        clubOne.put("location", "CABA-Palermo");
        clubOne.put("musicType", "House, Tech House");
        clubOne.put("hours", "Viernes y Sábado: 23:30 - 06:00");
        clubOne.put("rating", "4.2 (176 reseñas)");
        clubOne.put("price", "Entrada: $3000 - $4000");
        clubOne.put("audience", "21+ años");
        clubOne.put("website", "www.clubone.com.ar");
        clubOne.put("phone", "+54 11 5678-9012");
        clubOne.put("description", "Club One es el destino perfecto para los amantes de la música electrónica en Buenos Aires. Ubicado en el corazón de Palermo, cuenta con un potente sistema de sonido y una pista de baile espaciosa. DJs internacionales suelen presentarse aquí todos los fines de semana.");
        clubsInfo.put("2", clubOne);

        Map<String, String> groove = new HashMap<>();
        groove.put("title", "GROOVE");
        groove.put("image", "/resources/img/Bali-Imagen.webp");
        groove.put("location", "CABA-Palermo");
        groove.put("musicType", "Rock, Indie, Alternativo");
        groove.put("hours", "Jueves a Domingo: 22:00 - 05:00");
        groove.put("rating", "4.0 (320 reseñas)");
        groove.put("price", "Entrada: $2000 - $3000");
        groove.put("audience", "18+ años");
        groove.put("website", "www.groove.com.ar");
        groove.put("phone", "+54 11 2345-6789");
        groove.put("description", "Groove es un lugar emblemático para la escena de música en vivo en Buenos Aires. Con una gran variedad de espectáculos que van desde rock hasta indie y música alternativa, este venue ha sido hogar de numerosos artistas nacionales e internacionales.");
        clubsInfo.put("3", groove);

        Map<String, String> bahrein = new HashMap<>();
        bahrein.put("title", "BAHREIN");
        bahrein.put("image", "/resources/img/Bali-Imagen.webp");
        bahrein.put("location", "CABA-Centro");
        bahrein.put("musicType", "Techno, Minimal");
        bahrein.put("hours", "Viernes y Sábado: 00:00 - 06:00");
        bahrein.put("rating", "4.1 (215 reseñas)");
        bahrein.put("price", "Entrada: $2800 - $3500");
        bahrein.put("audience", "21+ años");
        bahrein.put("website", "www.bahrein.com.ar");
        bahrein.put("phone", "+54 11 3456-7890");
        bahrein.put("description", "Bahrein es un referente de la escena underground de Buenos Aires. Ubicado en el microcentro porteño en una antigua bóveda bancaria, este club ofrece las mejores propuestas de música electrónica con DJs locales e internacionales en un ambiente único.");
        clubsInfo.put("4", bahrein);

        Map<String, String> roseInRio = new HashMap<>();
        roseInRio.put("title", "ROSE IN RIO");
        roseInRio.put("image", "/resources/img/Bali-Imagen.webp");
        roseInRio.put("location", "CABA-Costanera");
        roseInRio.put("musicType", "Pop, Reggaetón, Comercial");
        roseInRio.put("hours", "Viernes a Domingo: 00:00 - 06:00");
        roseInRio.put("rating", "4.3 (280 reseñas)");
        roseInRio.put("price", "Entrada: $3000 - $5000");
        roseInRio.put("audience", "23+ años");
        roseInRio.put("website", "www.roseinrio.com.ar");
        roseInRio.put("phone", "+54 11 7890-1234");
        roseInRio.put("description", "Rose in Rio es uno de los boliches más exclusivos de Buenos Aires, con una vista privilegiada al Río de la Plata. Este club de estilo sofisticado ofrece una experiencia premium con la mejor selección musical comercial y un ambiente para ver y ser visto.");
        clubsInfo.put("5", roseInRio);

        return clubsInfo;
    }*/
}
