# Harel Travel Policy — E2E UI Test (Selenium + TestNG)

## A. Test Plan (What the test does & how)

**Goal:** validate the happy-path purchase flow for a **first-time buyer** on Harel’s travel policy site, using the **calendar UI** (no manual date typing).

**Flow under test**
1. **Home** → click **“לרכישה בפעם הראשונה”** (`data-hrl-bo="purchase-for-new-customer"`).
2. **Destination step** → select **USA** (tile with `data-hrl-bo^="USA"`), click **Next** (`data-hrl-bo="wizard-next-button"`).
3. **Date step** → open **start** and **end** date pickers and **select by calendar buttons**:
   - **Start date** = **today + 7 days** (TZ: `Asia/Jerusalem`)
   - **End date** = **start + 30 days**
   - Day buttons are located by `button[data-hrl-bo="yyyy-MM-dd"]`.
   - If needed, the test pages the calendar forward using the **arrow-forward** control (`[data-hrl-bo="arrow-forward"]`).
4. Click **Next** → land on **Travelers** step and assert URL contains `/wizard/travelers`.

**Stability aids**
- Explicit waits on URL & element visibility.
- Lightweight popup/overlay suppression via JS (does **not** close cookie consent—just prevents overlays from blocking clicks).
- Headless Chrome default (configurable).

**Artifacts per step**
- For each major step the test saves **PNG screenshots** and **raw HTML** under `target/`.
- All attachments are also published inside **Allure** for easy review.
- An additional **Extent** HTML report is written to `target/extent-report/index.html`.

---

## B. How to run from GitHub & view results

> ⚠️ **Access note:** To trigger runs, you must be added as a **collaborator with “Write”** permission. Please contact me via mail with your GitHub username so I can add you. Unfortunately Heroku Cloud isn't free anymore, and there arent any other suitable cloud providers, as well as I would run it using CI/CD pipeline anyways.

### 1) Trigger the workflow
- Open the repo → **Actions** tab → choose **“Run E2E & Publish Allure”**.
- Click **Run workflow** (use the default branch).  
  The job:
  - spins up Java 17 + Chrome (headless),
  - runs TestNG suite via Maven,
  - generates **Allure** results,
  - publishes the **Allure static site** to the **`gh-pages`** branch.

### 2) View the Allure report (GitHub Pages)
- After the job completes, open the GitHub Pages link:
https://maxily1.github.io/qa-exam/

- You’ll see:
  - Test summary & timeline
  - Steps with inline **screenshots** and **page source**
  - History across runs

> Tip: The first Pages publish may take ~1 minute to appear.

---

## C. Run locally (optional)

### Prerequisites
- **Java 17+**
- **Maven 3.8+**
- **Chrome** or **Chrome for Testing** (Selenium Manager will fetch what’s needed)
- (Optional) **Allure CLI** if you want to use `allure serve`

### 1) Execute the test
```bash
# In the repo root
mvn -q -Dheadless=true -Dsurefire.suiteXmlFiles=TestNG.xml test

