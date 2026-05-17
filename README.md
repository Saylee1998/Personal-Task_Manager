# Personal Task Manager API

Spring Boot task management application with CRUD operations, filtering/search, analytics, AI-powered task assistance, and a frontend UI.

---

## Tech Stack

- Java 17
- Spring Boot 3.5.14
- Spring Data JPA
- H2 Database
- Maven
- JUnit 5
- Google Gemini API
- HTML/CSS/JavaScript

---

## Features

### Task Management
- Create, update, delete, and view tasks
- Filter by status and priority
- Sort by due date, created date, title, and priority
- Search by ID or title

### AI Features
- AI task suggestion from natural language
- AI task breakdown into subtasks

### Analytics
- Task counts by status and priority
- Overdue task count
- Upcoming tasks (next 7 days)

### Frontend UI
- Create/manage tasks
- AI suggestion and AI breakdown buttons
- Task details modal
- API quick links for reviewers

---

## Run the Application

### 1. Set Gemini API Key

Mac/Linux:
```bash
export GEMINI_API_KEY=your-api-key
```

Windows PowerShell:
```powershell
$env:GEMINI_API_KEY="your-api-key"
```

---

### 2. Run the Project

```bash
./mvnw spring-boot:run
```

Application runs at:

```text
http://localhost:8080
```

---

## Run Tests

```bash
./mvnw test
```

---

## Main API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| POST | `/tasks` | Create task |
| GET | `/tasks` | Get all tasks |
| GET | `/tasks/{id}` | Get task by ID |
| PUT | `/tasks/{id}` | Update task |
| DELETE | `/tasks/{id}` | Delete task |
| GET | `/tasks/analytics` | Task analytics |
| POST | `/tasks/suggest` | AI task suggestion |
| POST | `/tasks/{id}/breakdown` | AI task breakdown |

---

## Example Task
#### You can find more example tasks in sample-tasks.json (convenient for testing)

```json
{
  "title": "Prepare quarterly report",
  "description": "Finalize metrics and submit",
  "dueDate": "2026-05-30T17:00:00",
  "priority": "HIGH",
  "status": "TODO"
}
```

---

## Validation Rules

- Title required (max 200 chars)
- Description max 1000 chars
- Due date must be future date
- Priority: LOW / MEDIUM / HIGH
- Status: TODO / IN_PROGRESS / DONE

---
---

## Timestamps

Each task response includes:

- `createdAt`: set when the task is created
- `updatedAt`: set when the task is created and refreshed whenever the task is updated

How to verify:

1. Create a task using `POST /tasks`
2. Note the `createdAt` and `updatedAt` values
3. Update the same task using `PUT /tasks/{id}`
4. Call `GET /tasks/{id}`

Expected result:

- `createdAt` stays the same
- `updatedAt` changes to the latest update time
## Project Structure

```text
src/main/java/com/eulerity/taskmanager
├── controller
├── dto
├── exception
├── model
├── repository
├── service
└── TaskManagerApplication.java
```

Frontend files:

```text
src/main/resources/static
├── index.html
├── css/styles.css
└── js/app.js
```

---

## Testing

Includes:
- Unit tests
- Integration tests
- Controller tests
- AI endpoint tests

Run:

```bash
./mvnw test
```

---
## AI Usage Examples

### AI Task Suggestion

Endpoint:

```text
POST /tasks/suggest
```

Example request:
You can also find some more examples in text file "AI-PromptsForTestingAIEndpoint.txt"

```json
{
  "description": "Prepare slides for the quarterly review meeting next Friday"
}
```

Other example prompts:
- "Remind me to submit tax documents tomorrow"
- "Plan onboarding session for new interns next week"
- "Finish backend API integration before Monday"

---

### AI Task Breakdown

Endpoint:

```text
POST /tasks/{id}/breakdown
```

Example workflow:
1. Create a complex task
2. Click **AI Breakdown** in the UI
3. Or call:

```text
POST /tasks/1/breakdown
```

Recommended complex tasks:
- "Launch company onboarding portal"
- "Prepare annual engineering roadmap"
- "Organize company hackathon event"
- "Migrate legacy authentication system"

--- 

## AI Assistance

AI tools were used (Claude code CLI) Sonnet 4.6 for implementation support, debugging assistance, and documentation refinement.

---

### AI transcript
You can find all AI Transcripts in folder AITranscripts. It also has a dedicated folder "Prompt Highlights"
which has a text file of all my prompts used.

---