# Rules for Development

> Read this before every session. These rules apply to ALL phases and tasks.

---

## 1. One Task at a Time

Complete one task from the current phase fully, then confirm to me what you did in brief and ask if I want to move to the next task. Wait for my response before proceeding. Never do an entire phase in one go.

---

## 2. Document Every File / Code Block

Every time you create a file or write a block of code, log it in the current phase's doc file (e.g., `phase-1.md`) under the **Code Log** section with:
- Date
- File name(s) 
- Brief description of what the code does
- Key method names, variable names, or class names if relevant

---

## 3. Comment the Code

Write clear, plain-English comments in the code itself so it's easy to understand. Comments should explain *why* something is done, not just *what* (the code already shows what).

Example:
```kotlin
// Fetch movie results from Simkl API based on the search query
// Returns a list of Movie objects with title, poster, year, and simkl_id
fun searchMovies(query: String): List<MovieSearchResult>
```

---

## 4. Track Progress in Phase Docs

After completing a task, update the phase doc:
- Mark the task checkbox from `[ ]` to `[x]`
- Add the code log entry
- Keep it clean and readable

---

## 5. No Scope Creep

Stick to the current task. Don't jump ahead to future phases or add features not in the plan. If you see something that needs fixing or could be improved, note it in the phase doc under a "Notes" section but don't act on it unless I ask.

---

## 6. Confirm Before Moving On

Every time a task is complete, send a brief message like:

> **Task X done:** [what was built in 1-2 lines]
> 
> Move to the next task?

Then wait for my answer.

---

## 7. No .gitignore Override

The `docs/` folder is gitignored. Keep it that way. Do not commit or push anything from `docs/`.

---

## 8. Keep It Simple

Don't over-engineer. Build what's asked, nothing more. Use the tech stack decided in the plan. No external libraries beyond what's listed unless absolutely necessary (and ask first).

---

## 9. Commit & Push Code After Every Task

After completing a task (marking it `[x]` and logging in the Code Log), commit all code changes and push to the remote repository before moving on. The commit message should reference the phase and task number.

⚠️ `docs/` stays local — never commit or push it. Only code and build files go to GitHub.
