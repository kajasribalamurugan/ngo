import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;

/*
 * A beginner-friendly backend with no database and no external libraries.
 * It stores records in standalone/ngo-data.txt so data remains after restart.
 */
public class SimpleServer {
    static final Path DATA_FILE = Paths.get("standalone", "ngo-data.txt");
    static final Path FRONTEND = Paths.get("src", "main", "resources", "static");
    static final List<Campaign> campaigns = new ArrayList<>();
    static final List<Donation> donations = new ArrayList<>();
    static final List<Expense> expenses = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        loadData();
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/dashboard", SimpleServer::dashboard);
        server.createContext("/api/campaigns", SimpleServer::campaigns);
        server.createContext("/api/donations", SimpleServer::donations);
        server.createContext("/api/expenses", SimpleServer::expenses);
        server.createContext("/", SimpleServer::staticFiles);
        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("ClearGive is running at http://localhost:8080");
        System.out.println("Press Ctrl + C to stop the server.");
    }

    static void dashboard(HttpExchange ex) throws IOException {
        if (!onlyGet(ex)) return;
        double raised = donations.stream().mapToDouble(d -> d.amount).sum();
        double spent = expenses.stream().mapToDouble(e -> e.amount).sum();
        double goal = campaigns.stream().mapToDouble(c -> c.goal).sum();
        Map<String, Double> categories = new LinkedHashMap<>();
        for (Expense e : expenses) categories.merge(e.category, e.amount, Double::sum);
        StringBuilder categoryJson = new StringBuilder();
        for (var item : categories.entrySet()) {
            if (categoryJson.length() > 0) categoryJson.append(',');
            categoryJson.append("{\"category\":\"").append(json(item.getKey())).append("\",\"amount\":").append(item.getValue()).append('}');
        }
        sendJson(ex, "{\"totalRaised\":" + raised + ",\"totalSpent\":" + spent + ",\"available\":" + (raised-spent) + ",\"totalGoal\":" + goal + ",\"categories\":[" + categoryJson + "]}");
    }

    static void campaigns(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            Map<String,String> body = readJson(ex);
            campaigns.add(new Campaign(nextId(campaigns), body.get("name"), body.get("description"), number(body.get("goal"))));
            saveData(); sendJson(ex, "{\"message\":\"Campaign saved\"}"); return;
        }
        if (!onlyGet(ex)) return;
        StringBuilder result = new StringBuilder("[");
        for (Campaign c : campaigns) {
            if (result.length() > 1) result.append(',');
            double raised = donations.stream().filter(d -> d.campaignId == c.id).mapToDouble(d -> d.amount).sum();
            double spent = expenses.stream().filter(e -> e.campaignId == c.id).mapToDouble(e -> e.amount).sum();
            result.append("{\"id\":").append(c.id).append(",\"name\":\"").append(json(c.name)).append("\",\"description\":\"").append(json(c.description)).append("\",\"goal\":").append(c.goal).append(",\"raised\":").append(raised).append(",\"spent\":").append(spent).append('}');
        }
        sendJson(ex, result.append(']').toString());
    }

    static void donations(HttpExchange ex) throws IOException {
        if (!ex.getRequestMethod().equals("POST")) { sendError(ex, 405); return; }
        Map<String,String> body = readJson(ex);
        donations.add(new Donation(nextId(donations), integer(body.get("campaignId")), emptyAs(body.get("donorName"), "Anonymous"), number(body.get("amount")), body.get("donatedOn")));
        saveData(); sendJson(ex, "{\"message\":\"Donation saved\"}");
    }

    static void expenses(HttpExchange ex) throws IOException {
        if (ex.getRequestMethod().equals("POST")) {
            Map<String,String> body = readJson(ex);
            expenses.add(new Expense(nextId(expenses), integer(body.get("campaignId")), body.get("itemName"), body.get("category"), number(body.get("amount")), body.get("spentOn")));
            saveData(); sendJson(ex, "{\"message\":\"Expense saved\"}"); return;
        }
        if (!onlyGet(ex)) return;
        StringBuilder result = new StringBuilder("[");
        for (int i=expenses.size()-1; i>=0; i--) {
            Expense e = expenses.get(i); Campaign c = campaigns.stream().filter(x -> x.id == e.campaignId).findFirst().orElse(new Campaign(0,"Unknown","",0));
            if (result.length() > 1) result.append(',');
            result.append("{\"id\":").append(e.id).append(",\"campaign\":\"").append(json(c.name)).append("\",\"item\":\"").append(json(e.item)).append("\",\"category\":\"").append(json(e.category)).append("\",\"amount\":").append(e.amount).append(",\"spentOn\":\"").append(json(e.date)).append("\"}");
        }
        sendJson(ex, result.append(']').toString());
    }

    static void staticFiles(HttpExchange ex) throws IOException {
        String requested = ex.getRequestURI().getPath();
        if (requested.equals("/")) requested = "/index.html";
        Path file = FRONTEND.resolve(requested.substring(1)).normalize();
        if (!file.startsWith(FRONTEND) || !Files.exists(file)) { sendError(ex, 404); return; }
        String type = requested.endsWith(".css") ? "text/css" : requested.endsWith(".js") ? "application/javascript" : "text/html";
        byte[] bytes = Files.readAllBytes(file); ex.getResponseHeaders().set("Content-Type", type + "; charset=UTF-8"); ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = ex.getResponseBody()) { out.write(bytes); }
    }

    static boolean onlyGet(HttpExchange ex) throws IOException { if (ex.getRequestMethod().equals("GET")) return true; sendError(ex,405); return false; }
    static void sendJson(HttpExchange ex, String text) throws IOException { byte[] bytes=text.getBytes(StandardCharsets.UTF_8); ex.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8"); ex.getResponseHeaders().set("Access-Control-Allow-Origin","*"); ex.sendResponseHeaders(200,bytes.length); try(OutputStream out=ex.getResponseBody()){out.write(bytes);} }
    static void sendError(HttpExchange ex, int code) throws IOException { ex.sendResponseHeaders(code,-1); ex.close(); }
    static double number(String value) { try { return Double.parseDouble(value); } catch(Exception e) { return 0; } }
    static int integer(String value) { try { return Integer.parseInt(value); } catch(Exception e) { return 0; } }
    static String emptyAs(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
    static String json(String value){return value==null?"":value.replace("\\","\\\\").replace("\"","\\\"").replace("\n"," ");}
    static int nextId(List<?> list) { return list.size() + 1; }

    // Small JSON reader for the simple form data sent by app.js.
    static Map<String,String> readJson(HttpExchange ex) throws IOException {
        String text = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8).trim(); Map<String,String> result = new HashMap<>();
        if (text.length()<2) return result; text=text.substring(1,text.length()-1);
        for(String pair:text.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)")){String[] part=pair.split(":",2);if(part.length==2)result.put(unquote(part[0]),unquote(part[1]));} return result;
    }
    static String unquote(String value){value=value.trim();if(value.startsWith("\"")&&value.endsWith("\""))value=value.substring(1,value.length()-1);return value.replace("\\\"","\"").replace("\\\\","\\");}

    static void loadData() throws IOException {
        if (!Files.exists(DATA_FILE)) { addSampleData(); saveData(); return; }
        for (String line : Files.readAllLines(DATA_FILE)) { String[] p=line.split("\\|",-1); if(p.length<2)continue;
            try { switch(p[0]) { case "C" -> campaigns.add(new Campaign(integer(p[1]),p[2],p[3],number(p[4]))); case "D" -> donations.add(new Donation(integer(p[1]),integer(p[2]),p[3],number(p[4]),p[5])); case "E" -> expenses.add(new Expense(integer(p[1]),integer(p[2]),p[3],p[4],number(p[5]),p[6])); } } catch(Exception ignored) {}
        }
    }
    static void saveData() throws IOException { Files.createDirectories(DATA_FILE.getParent()); StringBuilder text=new StringBuilder(); for(Campaign c:campaigns)text.append("C|").append(c.id).append('|').append(clean(c.name)).append('|').append(clean(c.description)).append('|').append(c.goal).append('\n'); for(Donation d:donations)text.append("D|").append(d.id).append('|').append(d.campaignId).append('|').append(clean(d.donor)).append('|').append(d.amount).append('|').append(d.date).append('\n'); for(Expense e:expenses)text.append("E|").append(e.id).append('|').append(e.campaignId).append('|').append(clean(e.item)).append('|').append(clean(e.category)).append('|').append(e.amount).append('|').append(e.date).append('\n'); Files.writeString(DATA_FILE,text.toString()); }
    static String clean(String s){return emptyAs(s,"").replace("|","/").replace("\n"," ");}
    static void addSampleData(){campaigns.add(new Campaign(1,"Healthy Children","Medical support and nutrition kits for children.",150000));campaigns.add(new Campaign(2,"Back to School","Books, uniforms and learning materials for students.",100000)); donations.add(new Donation(1,1,"Aarav Sharma",25000,"2026-07-02"));donations.add(new Donation(2,1,"Anonymous",18000,"2026-07-08"));donations.add(new Donation(3,2,"Rahul Mehta",30000,"2026-07-05"));expenses.add(new Expense(1,1,"Nutrition kits","Medical",16000,"2026-07-10"));expenses.add(new Expense(2,1,"Clinic medicines","Medical",11000,"2026-07-18"));expenses.add(new Expense(3,2,"School books","Education",18000,"2026-07-11"));}
    record Campaign(int id,String name,String description,double goal){} record Donation(int id,int campaignId,String donor,double amount,String date){} record Expense(int id,int campaignId,String item,String category,double amount,String date){}
}
