# StudyOS — AI Personal Study Operating System

Paste your notes → AI generates a quiz + flashcards + summary → the app tracks what you get wrong and schedules smart review reminders using spaced repetition, so you always know exactly what to study today.

## Features
- **AI Content Generation** — Google Gemini turns raw notes into a 5-question quiz, 6 flashcards, and a summary
- **Spaced Repetition Engine** — wrong answers get rescheduled sooner; correct streaks push reviews further out (Anki-style algorithm); 5 correct in a row = "Mastered"
- **Today's Study Plan Dashboard** — shows exactly what's due for review right now
- **Topic Library** — all your generated study kits in one place
- **Interactive Flashcards** — click to flip

## Tech Stack
- Backend: Java 17+, Spring Boot 3.2.5, Spring Data JPA, H2 (file-based DB)
- AI: Google Gemini API (`gemini-2.0-flash`, free tier)
- Frontend: Vanilla HTML/CSS/JS (no build step, single page app)

---

## PART 1 — Run Locally

### Prerequisites
- Java 17+ (you have Java 21 — that's fine)
- Maven (already working on your machine)
- A free Gemini API key: https://aistudio.google.com/apikey

### Step 1: Set your Gemini API key
The app reads the key from an environment variable — **never hardcode it in code or commit it to GitHub.**

**Windows PowerShell (for current session only):**
```powershell
$env:GEMINI_API_KEY="your-key-here"
```

**Windows PowerShell (permanently, so you don't retype it every time):**
```powershell
setx GEMINI_API_KEY "your-key-here"
```
Close and reopen your terminal after using `setx`.

### Step 2: Run the app
From inside the `studyos` project folder (where `pom.xml` is):
```
mvn spring-boot:run
```

Wait for `Started StudyOsApplication`, then open **http://localhost:8080**

### Step 3: Try it
1. Click **New Topic**
2. Paste any notes (a paragraph from a textbook, lecture notes, anything)
3. Click **Generate Study Kit** — takes 10-20 seconds
4. You'll see a quiz, flashcards, and a summary
5. Answer a question wrong on purpose → go to **Dashboard** → it now shows in "Review Queue" scheduled for tomorrow
6. Answer correctly a few times → it eventually gets marked "Mastered"

---

## PART 2 — Push to GitHub

1. Open **GitHub Desktop** → File → Add Local Repository → select the `studyos` folder
2. Click **Create Repository** (since it isn't a git repo yet)
3. **Important:** GitHub Desktop will respect `.gitignore`, so your API key (which lives in an environment variable, not a file) will never be committed — safe by default
4. Write a commit message → **Commit to main**
5. Click **Publish repository** (top right)

---

## PART 3 — Deploy Live (Render, free tier)

1. Go to https://render.com → sign up/log in with GitHub
2. Click **New +** → **Web Service**
3. Connect your `studyos` GitHub repo
4. Configure:
   - **Environment:** Java
   - **Build Command:** `./mvnw clean install` (or `mvn clean install` if no wrapper)
   - **Start Command:** `java -jar target/studyos-1.0.0.jar`
5. Under **Environment Variables**, add:
   - Key: `GEMINI_API_KEY`
   - Value: your actual Gemini key
6. Click **Create Web Service** — first deploy takes a few minutes
7. Render gives you a live URL like `https://studyos-xyz.onrender.com` — this is what you share with judges

**Note:** Render's free tier "sleeps" after inactivity — the first request after idle time can take ~30-50 seconds to wake up. Mention this if judges test it after a gap, or just refresh once before your demo starts.

---

## For Your Hackathon Pitch (30 seconds)
"Students juggle notes, quiz apps, and flashcards separately, and revision is usually just re-reading everything randomly. StudyOS turns any notes into an AI-generated quiz and flashcards instantly, then tracks exactly what you got wrong and uses spaced repetition — the same memory science behind Anki — to tell you precisely what to review today. It's not just AI wrapped around a text box; the scheduling algorithm is real, explainable CS, and the whole loop — input, AI generation, practice, adaptive scheduling — works end to end."

## Project Structure
```
src/main/java/com/studyos/app/
├── StudyOsApplication.java
├── model/          # StudyTopic, QuizQuestion, Flashcard
├── repository/      # Spring Data JPA repos
├── service/          # GeminiService (AI calls), SpacedRepetitionService (algorithm), StudyTopicService, QuizAttemptService
├── controller/       # REST controllers
└── dto/               # Request/response objects
src/main/resources/
├── static/            # Frontend (index.html, css, js)
└── application.properties
```
