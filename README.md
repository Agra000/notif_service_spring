# Notification Service

A Spring Boot service that **listens to RabbitMQ**.

When a new user is registered in the [user management service](https://github.com/Agra000/user_management_spring), that service sends a `user_registration` event to RabbitMQ. This service:

1. Receives the event.
2. Builds a welcome message for the new user.
3. Saves the message type and body into the `notif_logs` table.

```
user management service ──► RabbitMQ (auth.exchange) ──► auth.events.queue ──► this service ──► notif_logs table
```

This service has **no API endpoints**. It only reacts to messages from RabbitMQ.

---

## What you need

- **IntelliJ IDEA**
- **JDK 21**
- **PostgreSQL** (installed on your computer, port `5432`)
- **Docker Desktop**
- The **user management service** project (it has the `docker-compose.yml` and sends the events)

---

## How to run (step by step)

### Step 1 — Open the project

1. Open IntelliJ → **File → Open...** → choose this project folder (the one with `pom.xml`).
2. If IntelliJ asks *"This window or New Window?"*, choose **New Window**. Then both services can run at the same time.
3. Wait until the loading bar at the bottom is finished.
4. Go to **File → Project Structure → Project** and make sure **SDK = 21**.
5. If IntelliJ shows *"Lombok requires enabled annotation processing"*, click **Enable**.

### Step 2 — Create the `resources` folder and `application.yml`

The config file is **not** in the repo, so you need to make it yourself.

1. In the project panel, right-click `src/main` → **New → Directory** → type `resources`.
2. Paste the file (application.yml) that i give to the resource folder

#### What you need to change in `application.yml`

| Line | What to put |
|---|---|
| `url` | Change `notification_db` at the end if your database has a different name. |
| `username` | Your PostgreSQL username (usually `postgres`). |
| `password` | **Your PostgreSQL password.** |

Leave the RabbitMQ part as it is:

- `exchange` **must be the same** as in the user management service (`auth.exchange`).
- `queue` is the queue this service listens to. Write it without anything in front (just `auth.events.queue`).

> Use **spaces**, not Tab, in the YAML file.

### Step 3 — Run Docker Compose

This project does **not** have its own `docker-compose.yml`. RabbitMQ is started from the **user management service** project.

1. Open **Docker Desktop** and wait until it says *Engine running*.
2. In the **user management service** project, open the Terminal (`Alt + F12`) and run:

```bash
docker compose up -d
```

3. Check that RabbitMQ is running:

```bash
docker ps
```

You should see `rabbitmq` with status `Up`.

### Step 4 — Prepare the database

Open pgAdmin or DBeaver, create a database called `notification_db`, then run the SQL inside folder that i gave to you.

### Step 5 — Clean and build (optional, like `dotnet clean` / `dotnet build`)

Open the **Maven** panel on the right side of IntelliJ → **notification_service → Lifecycle**:

1. Double-click **clean**
2. Double-click **install**

You don't have to do this every time. It is useful for the first run, or when something acts strange after you change code or config.

### Step 6 — Run

1. Open `src/main/java/com/example/notification_service/NotificationServiceApplication.java`.
2. Click the green **▶** button next to `public class NotificationServiceApplication`.

The service is ready when the console shows:

```
Started NotificationServiceApplication in ... seconds
```

You will **not** see `Tomcat started on port ...`. That is normal, because this service has no web server. It keeps running and waits for messages.

Next time, just press **▶** at the top-right of IntelliJ.
To stop it, press the red **■** button.

### Running both services

Keep two IntelliJ windows open and press **▶** in both:

- **Notification service** (this project)
- **User management service**

The order doesn't matter. If this service is off when a user is registered, the message waits in RabbitMQ and is processed when this service starts.

---

## Test cases

### TC-01 — Service is connected to RabbitMQ

1. Run this service.
2. Open **http://localhost:15672** and log in with `guest` / `guest`.
3. Go to **Queues and Streams** → click `auth.events.queue`.

Expected: **Consumers = 1**.

### TC-02 — Register a user in User Management Service, then check `notif_logs`

1. Run **both** services.
2. In Postman, log in as admin in the user management service:
   `POST http://localhost:8080/api/auth/login`
   ```json
   { "email": "admin@example.com", "password": "admin12345" }
   ```
3. Register a new user with the admin token (**Authorization → Bearer Token**):
   `POST http://localhost:8080/api/auth/regist-user`
   ```json
   {
     "fullName": "Siti Aminah",
     "userName": "siti",
     "password": "siti12345",
     "email": "siti@example.com",
     "roleId": 2
   }
   ```
4. Check the database:
   ```sql
   SELECT * FROM notif_logs ORDER BY id DESC;
   ```

Expected: a new row where

- `notif_type` = `user_registration`
- `notif_body` contains `Halo Siti Aminah` and `Email login: siti@example.com`
- `created_at` is filled

### TC-03 — Send a test message by hand (without the user service)

In **http://localhost:15672** → **Exchanges** → click `auth.exchange` → **Publish message**:

- **Routing key:** `auth.user.registered`
- **Properties:** `content_type` = `application/json`
- **Payload:**

```json
{
  "eventId": "test-001",
  "eventType": "user_registration",
  "createdAt": "2026-11-23T04:11:40.223Z",
  "createdBy": "admin-test",
  "data": {
    "id": "11111111-1111-1111-1111-111111111111",
    "fullName": "Test User",
    "email": "test@example.com",
    "roleId": 2
  }
}
```

Click **Publish message**.

Expected: a new row in `notif_logs` with `Halo Test User` in the body.

### TC-04 — Wrong event type is ignored

Publish the same message as TC-03, but change `"eventType"` to `"user_login"`.

Expected: **no** new row in `notif_logs`.

### TC-05 — Message waits while the service is off

1. Stop this service (**■**).
2. Register a new user (TC-02, with a different email).
3. In RabbitMQ → `auth.events.queue`, **Ready** shows `1`.
4. Start this service again (**▶**).

Expected: **Ready** goes back to `0`, and a new row appears in `notif_logs`.

---

## Common problems

| Problem | What to do |
|---|---|
| Red imports / `Cannot resolve symbol` | Open `pom.xml` and click the small **"m"** reload icon at the top-right. |
| `Could not resolve placeholder 'app.rabbit...'` | `application.yml` is missing, in the wrong folder, or a key is misspelled. It must be at `src/main/resources/application.yml`. |
| `Unable to determine Dialect without JDBC metadata` | Can't connect to PostgreSQL. Check the database name, username and password, and make sure `notification_db` exists. |
| `Connection refused` | Docker Desktop or the `rabbitmq` container is not running. Run `docker compose up -d` in the user management project. |
| `Failed to check/redeclare auto-delete queue(s)` | RabbitMQ was not ready yet. Wait a few seconds after starting Docker, then press **▶** again. |
| Consumers = 0 in RabbitMQ | This service is not running, or it stopped because of an error. Check the console. |
