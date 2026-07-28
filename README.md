ClearGive — NGO Donation Transparency Dashboard
A simple college project made with HTML, CSS, JavaScript, Java (Spring Boot), and MySQL.

Features
Public dashboard with total donations, expenses, available balance, and campaign goals.
Campaign-level progress and category-wise spending display.
Public itemised expense log.
Simple NGO portal to add campaigns, donations, and expenses.
What you need
Java 17 or later
MySQL Server
Maven (or run the Maven wrapper if you add one)
Run it
In MySQL, create the database once:

CREATE DATABASE ngo_transparency;
Open src/main/resources/application.properties and replace YOUR_MYSQL_PASSWORD with your MySQL password. If your MySQL username is not root, change that too.

From this project folder run:

mvn spring-boot:run
Open http://localhost:8080 in a browser.

The first run automatically creates the three tables (campaigns, donations, expenses) and adds sample data. If you want a completely empty database later, delete the database, create it again, and run the project again.

Project structure
src/main/resources/static/ — frontend files
src/main/java/.../DashboardController.java — small REST API and SQL queries
src/main/resources/schema.sql — MySQL table definitions
src/main/resources/data.sql — sample data
Note
This is intentionally simple for learning. The NGO portal has no authentication; do not use it in production without adding login, validation, and proper security.
