# Run Campus Slot locally on Windows

Use two terminal windows: one for Java and one for React. PostgreSQL runs in the background. The first dependency download may take several minutes.

## 1. Get the source

Clone the repository and open the `campus-slot` folder in your editor.

```powershell
git clone https://github.com/sanuoa/campus-slot.git
cd campus-slot
```

## 2. Select JDK 17

Install JDK 17. In IntelliJ select **JDK 17** for Project SDK, module SDK, Maven importer and Maven runner. The `pom.xml` targets Java 17.

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

Keep this terminal open. Startup should finish with `Started CampusSlotApplication` and no database errors. By default, local verification links are printed here instead of being emailed. To deliver real messages while keeping the local profile, configure Gmail below.

## Send verification emails with Gmail

The local profile defaults to `MAIL_DELIVERY=console`: no email is sent. For real delivery, use a Gmail account as the sender; student recipients still need an `@iit.ac.lk` address.

1. Enable [2-Step Verification](https://support.google.com/accounts/answer/185839) on the sender's Google account.
2. Create a [Google App Password](https://support.google.com/accounts/answer/185833) for Campus Slot. Use that password, not your usual Google password. App Passwords may be unavailable for managed accounts or accounts with restrictive security policies; use an approved SMTP provider in that case.
3. Stop the backend with Ctrl+C. In the same PowerShell window, keep the database variables and local profile from step 4, then run:

```powershell
$env:MAIL_DELIVERY="smtp"
$env:SMTP_HOST="smtp.gmail.com"
$env:SMTP_PORT="587"
$env:SMTP_AUTH="true"
$env:SMTP_STARTTLS="true"
$env:SMTP_SSL="false"
$env:SMTP_USERNAME=Read-Host "Your Gmail sender address"
$env:MAIL_FROM=$env:SMTP_USERNAME
$gmailSecret=Read-Host "Google App Password (not your Google password)" -AsSecureString
$env:SMTP_PASSWORD=([System.Net.NetworkCredential]::new("", $gmailSecret)).Password.Replace(" ", "")
Remove-Variable gmailSecret
$env:FRONTEND_URL="http://localhost:5173"
.\mvnw.cmd spring-boot:run
```

The password stays in this process environment; do not paste it into source files, screenshots, chat or Git. SMTP uses authenticated STARTTLS, requires encryption, and verifies the server certificate. For another provider, use its host, port, authentication and sender requirements. Port 465 uses `SMTP_SSL=true` and `SMTP_STARTTLS=false` instead.

Register, then check the recipient's IIT inbox and spam folder. If the account already exists but is unverified, enter its email and password on the login form and choose **Resend verification link**. Wait at least one minute after the previous link. Successful resends replace older links; each link expires after 24 hours. Login does not automatically send another email.

The sender must be a Gmail account you control; it need not be the student's IIT address. Gmail acceptance does not guarantee inbox delivery: an institution may filter external messages. If Gmail rejects authentication, confirm the App Password, 2-Step Verification and account policy. If registration reports delivery unavailable, it is rolled back so it can be retried. A failed resend preserves the previous verification link.

To return to terminal-only development, stop the backend, set `$env:MAIL_DELIVERY="console"`, and restart. Never use console delivery publicly. For production, use the `prod` profile, HTTPS, secure cookies and a publicly reachable `FRONTEND_URL`.

## 5. Start the frontend

From the repository root in terminal 2:

```powershell
cd frontend
npm ci
npm run dev
```

Open **http://localhost:5173**. Use this origin consistently rather than switching between `localhost` and `127.0.0.1` in the browser.

1. Choose **Create an account** and register a test `@iit.ac.lk` address with a password of at least 12 characters.
2. Open the verification link from your email (SMTP mode) or backend terminal (console mode), then choose **Verify my email**.
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
| Password authentication failed for user `campus_slot` | The database role password and `DB_PASSWORD` differ. In SQL Shell connected as `postgres`, run `\password campus_slot` and choose a new password. In the backend PowerShell terminal, set `$env:DB_PASSWORD` to that same password and restart the backend. With Docker, changing `.env` does not update a password in an already initialized database volume; run `docker compose exec db psql -U campus_slot -d campus_slot`, then `\password campus_slot`, and update `DB_PASSWORD` in the backend terminal too. |
| Connection refused on 5432 | Start PostgreSQL, or change `DB_URL` to the configured local port |
| Login fails after registration | Complete verification; check email in SMTP mode or the backend terminal in console mode. Use Resend verification link with your account credentials if needed |
| No email arrives | Check MAIL_DELIVERY is smtp, Gmail App Password settings, spam folder, and SMTP connectivity on port 587 |
| Browser shows backend unavailable | Keep terminal 1 running; verify Spring Boot started successfully on 8080 |
| No slots on a date | Publish sessions in Administration; sample generation only happens once |
| CSRF/session errors | Use `http://localhost:5173` consistently and log in again after a server restart |

Stop each development process with Ctrl+C. PostgreSQL keeps the records between runs. Never commit passwords, verification links or your `.env` file.
