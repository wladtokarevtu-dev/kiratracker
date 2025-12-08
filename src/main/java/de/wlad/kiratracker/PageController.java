package de.wlad.kiratracker;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/admin") // Aufrufbar unter /admin (ohne .html)
    public String adminPage() {
        return "admin"; // Sucht nach templates/admin.html
    }

    // Optional: Auch index explizit mappen, falls index.html auch in templates liegt
    // (Falls index.html in static liegt, funktioniert es automatisch unter /)
}
