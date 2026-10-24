# Run Campus Slot locally on Windows

Use two terminal windows: one for Java and one for React. PostgreSQL runs in the background. The first dependency download may take several minutes.

## 1. Get the source

Clone the repository once the feature branch is available, or extract the provided source ZIP. Open the `campus-slot` folder in your editor.

```powershell
git clone https://github.com/sanuoa/campus-slot.git
cd campus-slot
# After the feature branch has been pushed:
git switch feat/campus-slot-mvp
```

## 2. Select JDK 17

You already have Java 17 and 25 installed. In IntelliJ select **JDK 17** for Project SDK, module SDK, Maven importer and Maven runner. The `pom.xml` targets Java 17.

In PowerShell, set `JAVA_HOME` to the actual JDK 17 folder on your machine (the folder containing `bin\javac.exe`), and place its `bin` first in `Path` for this terminal:

```powershell
# Replace this example with the actual folder on your PC.
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
node --version
git --version
```

Both Java commands should show 17. Use Node.js 22.12 or later; Node 24 is the CI target. The Maven wrapper downloads Maven, so a global Maven installation is optional.

## 3. Install PostgreSQL

Download the Windows installer from the [official PostgreSQL Windows page](https://www.postgresql.org/download/windows/). Install the database server and command-line tools; pgAdmin is useful but optional. Remember the password you select for the `postgres` administrator. Keep the standard local port `5432` unless it is already in use.

Open **SQL Shell (psql)**, connect locally as `postgres`, and run:

```sql
CREATE ROLE campus_slot LOGIN;
\password campus_slot
CREATE DATABASE campus_slot OWNER campus_slot;
CREATE DATABASE campus_slot_test OWNER campus_slot;
```

The `\password` command prompts for the application's database password without putting it into a SQL file. Keep the same password for `DB_PASSWORD` below. `campus_slot_test` is separate because automated integration tests erase their tables.

If using pgAdmin instead, create a Login/Group Role named `campus_slot`, set a password in its Definition tab, enable login, then create the two databases owned by that role.

### Optional alternative: Docker

Only use this route if Docker Desktop is installed; native PostgreSQL above is sufficient. Stop any other server occupying port 5432 first.

```powershell
Copy-Item .env.example .env
# Edit .env to choose DB_PASSWORD before starting.
docker compose up -d db
```

Docker creates the main database only. Create the disposable test database separately if needed. The root `.env` is read by Docker Compose; Spring Boot does not automatically load it.

## 4. Start the backend

From the repository root in terminal 1:

```powershell
cd backend
$env:SPRING_PROFILES_ACTIVE="local"
$env:DB_URL="jdbc:postgresql://localhost:5432/campus_slot"
$env:DB_USERNAME="campus_slot"
$env:DB_PASSWORD="the-password-you-chose"

# First run only: choose an administrator address and a strong local password.
$env:APP_ADMIN_EMAIL="your-admin-address@iit.ac.lk"
$env:APP_ADMIN_PASSWORD="choose-a-unique-password-of-at-least-12-characters"

.\mvnw.cmd spring-boot:run
```

Use an administrator address different from your student test account. The explicit bootstrap creates an admin only when absent. It refuses to silently promote an existing student. Admins are not student bookers. Remove the two admin bootstrap variables after initial creation; an existing administrator password is not changed by restarting.

Flyway creates the tables. The local profile adds fictional study room/lab/court examples and upcoming sample sessions on the **first empty-database startup only**. Use the administrator UI to publish more sessions later.

Keep this terminal open. Startup should finish with `Started CampusSlotApplication` and no database errors. Registration verification links are printed here in local mode. This is a test mail replacement, not real email delivery.

## 5. Start the frontend

From the repository root in terminal 2:

```powershell
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Use this origin consistently rather than switching between `localhost` and `127.0.0.1` in the browser.

1. Choose **Create an account** and register a test `@iit.ac.lk` address with a password of at least 12 characters.
2. Copy the local verification link from the backend terminal into your browser, then choose **Verify my email**.
3. Log in. Choose tomorrow's date, select a facility and book an available hour.
4. Try another facility on the same date. The daily limit must prevent booking it.
5. Cancel the first booking before it starts, then choose another space.
6. In a private browser window, log in as the bootstrap admin and test publishing/blocking sessions.

The capacity describes the facility; selecting it always reserves the entire space. The application does not connect to an actual campus access system or physically control room access.

## 6. Run checks

Frontend:

```powershell
cd frontend
npm test
npm run build
```

Java domain tests (no database needed):

```powershell
cd backend
.\mvnw.cmd test
```

Full integration suite — **use the disposable test database only**:

```powershell
cd backend
$env:DB_URL="jdbc:postgresql://localhost:5432/campus_slot_test"
$env:DB_USERNAME="campus_slot"
$env:DB_PASSWORD="the-password-you-chose"
$env:RUN_DB_TESTS="true"
Remove-Item Env:APP_ADMIN_EMAIL -ErrorAction SilentlyContinue
Remove-Item Env:APP_ADMIN_PASSWORD -ErrorAction SilentlyContinue
.\mvnw.cmd verify

# Restore application settings before running the application again.
Remove-Item Env:RUN_DB_TESTS
$env:DB_URL="jdbc:postgresql://localhost:5432/campus_slot"
```

For Linux/macOS use `./mvnw` and `export VARIABLE=value` instead of the PowerShell syntax. For frontend browser tests see `frontend/playwright.config.js`; install Chromium using `npx playwright install chromium` before `npm run test:e2e`.

## Common problems

| Problem | Fix |
| --- | --- |
| JVM target mismatch | Select JDK 17 everywhere, including IntelliJ's Maven runner; check `javac -version` |
| Password authentication failed | Confirm the `campus_slot` role's password matches `DB_PASSWORD` |
| Connection refused on 5432 | Start PostgreSQL, or change `DB_URL` to the configured local port |
| Login fails after registration | Complete verification; in local mode find the link in the backend terminal |
| Browser shows backend unavailable | Keep terminal 1 running; verify Spring Boot started successfully on 8080 |
| No slots on a date | Publish sessions in Administration; sample generation only happens once |
| CSRF/session errors | Use `http://localhost:5173` consistently and log in again after a server restart |

Stop each development process with Ctrl+C. PostgreSQL keeps the records between runs. Never commit passwords, verification links or your `.env` file.
