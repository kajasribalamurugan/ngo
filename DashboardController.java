package com.ngo.transparency;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class DashboardController {
    private final JdbcTemplate jdbc;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public DashboardController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        BigDecimal raised = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM donations", BigDecimal.class);
        BigDecimal spent = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM expenses", BigDecimal.class);
        BigDecimal goal = jdbc.queryForObject("SELECT COALESCE(SUM(goal_amount),0) FROM campaigns WHERE active = true", BigDecimal.class);
        List<Map<String,Object>> categories = jdbc.queryForList("SELECT category, SUM(amount) AS amount FROM expenses GROUP BY category ORDER BY amount DESC");
        return Map.of("totalRaised", raised, "totalSpent", spent, "available", raised.subtract(spent), "totalGoal", goal, "categories", categories);
    }

    @GetMapping("/campaigns")
    public List<Map<String,Object>> campaigns() {
        return jdbc.queryForList("""
            SELECT c.id, c.name, c.description, c.goal_amount AS goal,
              COALESCE((SELECT SUM(d.amount) FROM donations d WHERE d.campaign_id=c.id),0) AS raised,
              COALESCE((SELECT SUM(e.amount) FROM expenses e WHERE e.campaign_id=c.id),0) AS spent
            FROM campaigns c WHERE c.active=true ORDER BY c.id DESC""");
    }

    @GetMapping("/expenses")
    public List<Map<String,Object>> expenses() {
        return jdbc.queryForList("SELECT e.id, c.name AS campaign, e.item_name AS item, e.category, e.amount, e.spent_on AS spentOn FROM expenses e JOIN campaigns c ON c.id=e.campaign_id ORDER BY e.spent_on DESC");
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody LoginInput input) {
        if ("ngo@cleargive.org".equalsIgnoreCase(input.email()) && "ngo123".equals(input.password())) {
            String token = UUID.randomUUID().toString();
            sessions.put(token, "ClearGive NGO");
            return Map.of("token", token, "name", "ClearGive NGO");
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Incorrect email or password");
    }

    @PostMapping("/campaigns")
    public void addCampaign(@RequestBody CampaignInput input, HttpServletRequest request) {
        checkLogin(request);
        jdbc.update("INSERT INTO campaigns(name, description, goal_amount) VALUES(?,?,?)", input.name(), input.description(), input.goal());
    }

    @PostMapping("/donations")
    public void addDonation(@RequestBody DonationInput input, HttpServletRequest request) {
        checkLogin(request);
        jdbc.update("INSERT INTO donations(campaign_id, donor_name, amount, donated_on) VALUES(?,?,?,?)", input.campaignId(), input.donorName(), input.amount(), input.donatedOn());
    }

    @PostMapping("/expenses")
    public void addExpense(@RequestBody ExpenseInput input, HttpServletRequest request) {
        checkLogin(request);
        jdbc.update("INSERT INTO expenses(campaign_id, item_name, category, amount, spent_on) VALUES(?,?,?,?,?)", input.campaignId(), input.itemName(), input.category(), input.amount(), input.spentOn());
    }

    private void checkLogin(HttpServletRequest request) {
        if (!sessions.containsKey(request.getHeader("X-NGO-Token"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please log in as an NGO first");
        }
    }

    public record CampaignInput(String name, String description, BigDecimal goal) {}
    public record DonationInput(Integer campaignId, String donorName, BigDecimal amount, LocalDate donatedOn) {}
    public record ExpenseInput(Integer campaignId, String itemName, String category, BigDecimal amount, LocalDate spentOn) {}
    public record LoginInput(String email, String password) {}
}
