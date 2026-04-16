# Minimal Clean Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add Minimal Clean theme with Settings modal toggle, preserving all existing functionality.

**Architecture:** CSS class toggle system (`body.classic` / `body.minimal`) with theme-specific variable overrides. Settings modal replaces direct dark mode button. All changes in single index.html file.

**Tech Stack:** Vanilla HTML/CSS/JS, CSS custom properties, localStorage

---

## File Structure

**Modify:** `src/main/resources/static/index.html`
- Lines 8-134: CSS styles (add minimal theme variables and overrides)
- Lines 160-210: Add Settings modal HTML
- Lines 212-220: Update topbar HTML
- Lines 328-510: JavaScript (add theme functions)

**Delete after completion:**
- `mockups.html`
- `mockup-final.html`

---

## Task 1: Add Minimal Theme CSS Variables

**Files:**
- Modify: `src/main/resources/static/index.html:8-12`

- [ ] **Step 1: Add minimal light theme variables after existing dark theme**

Find the existing CSS variables section and add minimal theme variables. Insert after line 10 (the `body.dark` block):

```css
body.minimal{--bg:#FAFAF8;--card:#fff;--card2:#F5F5F3;--border:#E8E6E1;--text:#1A1A1A;--text-secondary:#6B6B6B;--accent:#1A1A1A;--accent-light:#F5F5F3;--accent-border:#E0E0E0;--success:#2D8A4E;--success-light:#E8F5ED;--error:#C43A31;--error-light:#FEF2F1;--sep:#E8E6E1;--ibg:#F5F5F3;--r:14px;--rs:10px;--sh:0 1px 3px rgba(0,0,0,.08);}
body.minimal.dark{--bg:#0F0F0E;--card:#1A1A18;--card2:#252523;--border:#2A2A28;--text:#F5F5F3;--text-secondary:#A0A0A0;--accent:#FFFFFF;--accent-light:#2A2A28;--accent-border:#3A3A38;--success:#3DA364;--success-light:#1A2E20;--error:#D4534A;--error-light:#2E1A18;--sep:#2A2A28;--ibg:#252523;}
```

- [ ] **Step 2: Verify CSS is valid by opening page**

Open `index.html` in browser, check console for CSS errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add minimal theme CSS variables"
```

---

## Task 2: Add Settings Modal HTML

**Files:**
- Modify: `src/main/resources/static/index.html:209-211`

- [ ] **Step 1: Add Settings modal after notifyM modal**

Insert after the closing `</div>` of `notifyM` modal (around line 210):

```html
<div class="mo" id="settingsM" onclick="moOut(event,'settingsM')">
    <div class="modal">
        <div class="mh"></div>
        <div style="font-size:1.5rem">⚙️</div>
        <div style="font-weight:800;font-size:1rem;margin-top:7px">Einstellungen</div>

        <div style="margin-top:16px;text-align:left">
            <div style="font-size:.7rem;font-weight:700;color:var(--ts);text-transform:uppercase;letter-spacing:.05em;margin-bottom:8px">Theme</div>
            <div class="tsw" style="width:100%;justify-content:center">
                <div class="to" id="themeClassic" onclick="setTheme('classic')">Classic</div>
                <div class="to" id="themeMinimal" onclick="setTheme('minimal')">Minimal</div>
            </div>
        </div>

        <div style="margin-top:16px;text-align:left">
            <div style="font-size:.7rem;font-weight:700;color:var(--ts);text-transform:uppercase;letter-spacing:.05em;margin-bottom:8px">Darstellung</div>
            <div class="tsw" style="width:100%;justify-content:center">
                <div class="to" id="darkOff" onclick="setDarkMode(false)">Hell</div>
                <div class="to" id="darkOn" onclick="setDarkMode(true)">Dunkel</div>
            </div>
        </div>

        <div style="margin-top:16px;text-align:left">
            <div style="font-size:.7rem;font-weight:700;color:var(--ts);text-transform:uppercase;letter-spacing:.05em;margin-bottom:8px">Sprache</div>
            <div class="tsw" style="width:100%;justify-content:center">
                <div class="to" id="langDe" onclick="setLang('de')">Deutsch</div>
                <div class="to" id="langRu" onclick="setLang('ru')">Русский</div>
            </div>
        </div>

        <div class="macts" style="margin-top:20px"><button class="mb mbs" onclick="closeM('settingsM')">Schliessen</button></div>
    </div>
</div>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add settings modal HTML structure"
```

---

## Task 3: Update Topbar - Replace Buttons

**Files:**
- Modify: `src/main/resources/static/index.html:212-220`

- [ ] **Step 1: Update topbar to have settings button instead of separate toggles**

Replace the topbar section:

```html
<div class="topbar">
    <div class="tlogo" id="logoText">🐕 Kira</div>
    <div class="tacts">
        <button class="ibtn gold" id="pauseB" onclick="openPauseM()" style="display:none">🐾</button>
        <button class="ibtn" id="settingsB" onclick="openSettings()">⚙️</button>
    </div>
</div>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: simplify topbar with settings button"
```

---

## Task 4: Add Theme Toggle JavaScript Functions

**Files:**
- Modify: `src/main/resources/static/index.html` (script section)

- [ ] **Step 1: Add theme variable to state**

Find the line with `let lang='de',lbD=3,...` and add `_theme`:

```javascript
let lang='de',lbD=3,hC=[],showAll=false,_lb={},_food=[],_cr=null,_pi=null,_sleep=false,_theme='classic';
```

- [ ] **Step 2: Add initTheme function after initDark**

Insert after `toggleDark` function:

```javascript
function initTheme(){
    _theme=localStorage.getItem('theme')||'classic';
    applyTheme(_theme);
}
function applyTheme(t){
    _theme=t;
    document.body.classList.remove('classic','minimal');
    document.body.classList.add(t);
    updateThemeUI();
    updateLogoForTheme();
}
function setTheme(t){
    haptic('l');
    localStorage.setItem('theme',t);
    applyTheme(t);
}
function updateThemeUI(){
    document.getElementById('themeClassic').classList.toggle('active',_theme==='classic');
    document.getElementById('themeMinimal').classList.toggle('active',_theme==='minimal');
}
function updateLogoForTheme(){
    const logo=document.getElementById('logoText');
    if(_theme==='minimal'){
        logo.textContent='Kira';
        logo.style.background='none';
        logo.style.webkitTextFillColor='var(--text)';
        logo.style.color='var(--text)';
    }else{
        logo.textContent='🐕 Kira';
        logo.style.background='linear-gradient(90deg,var(--gold),var(--gold3))';
        logo.style.webkitBackgroundClip='text';
        logo.style.webkitTextFillColor='transparent';
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add theme toggle JavaScript functions"
```

---

## Task 5: Add Settings Modal JavaScript Functions

**Files:**
- Modify: `src/main/resources/static/index.html` (script section)

- [ ] **Step 1: Add openSettings function and update dark/lang functions**

Insert after the theme functions:

```javascript
function openSettings(){
    haptic('m');
    updateSettingsUI();
    document.getElementById('settingsM').classList.add('open');
}
function updateSettingsUI(){
    updateThemeUI();
    document.getElementById('darkOff').classList.toggle('active',!document.body.classList.contains('dark'));
    document.getElementById('darkOn').classList.toggle('active',document.body.classList.contains('dark'));
    document.getElementById('langDe').classList.toggle('active',lang==='de');
    document.getElementById('langRu').classList.toggle('active',lang==='ru');
}
function setDarkMode(d){
    haptic('l');
    localStorage.setItem('dark',d?'1':'0');
    applyDark(d);
    updateSettingsUI();
}
function setLang(l){
    haptic('l');
    lang=l;
    localStorage.setItem('lang',l);
    applyLang();
    updateSettingsUI();
    renderH();renderLb(_lb);renderFood(_food);
}
```

- [ ] **Step 2: Update DOMContentLoaded to call initTheme**

Find the `DOMContentLoaded` event listener and add `initTheme()`:

```javascript
document.addEventListener('DOMContentLoaded',()=>{
    initTheme();initDark();initLang();initPause();checkSleepTime();initReveal();
    loadWeather();loadStatus();loadFood();loadH();loadLb();loadStats();
    document.getElementById('nameI').addEventListener('keypress',e=>{if(e.key==='Enter')addWalk();});
    document.getElementById('fdI').addEventListener('keypress',e=>{if(e.key==='Enter')addFood();});
});
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: add settings modal JavaScript and init"
```

---

## Task 6: Add Minimal Topbar Styles

**Files:**
- Modify: `src/main/resources/static/index.html` (CSS section)

- [ ] **Step 1: Add minimal topbar overrides after existing topbar styles**

Insert after line ~33 (after `.ibtn.gold` styles):

```css
body.minimal .topbar{background:rgba(250,250,248,.92);border-bottom:1px solid var(--border);}
body.minimal.dark .topbar{background:rgba(15,15,14,.92);}
body.minimal .tlogo{font-weight:700;font-size:1rem;background:none;-webkit-text-fill-color:var(--text);color:var(--text);}
body.minimal .ibtn{background:var(--card);border:1px solid var(--border);border-radius:var(--rs);}
body.minimal .ibtn.gold{background:var(--card);border:1px solid var(--border);color:var(--text);}
body.minimal .ibtn.active{background:var(--accent);color:var(--bg);}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "style: add minimal theme topbar styles"
```

---

## Task 7: Add Minimal Hero Styles

**Files:**
- Modify: `src/main/resources/static/index.html` (CSS section)

- [ ] **Step 1: Add minimal hero overrides**

Insert after existing hero styles (around line 42):

```css
body.minimal .hero{text-align:left;padding:20px 0 24px;}
body.minimal .av{display:none;}
body.minimal h1{font-size:1.6rem;background:none;-webkit-text-fill-color:var(--text);color:var(--text);margin-bottom:4px;}
body.minimal .hsub{font-size:.9rem;}
```

- [ ] **Step 2: Add greeting time logic to JavaScript**

Update the `applyTheme` function to also update greeting:

```javascript
function updateGreeting(){
    if(_theme!=='minimal')return;
    const h=new Date().getHours();
    let greet='Guten Tag';
    if(h>=5&&h<12)greet='Guten Morgen';
    else if(h>=12&&h<18)greet='Guten Tag';
    else greet='Guten Abend';
    const h1=document.querySelector('h1');
    if(h1)h1.textContent=greet;
}
```

Add `updateGreeting();` call at the end of `applyTheme` function.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "style: add minimal hero styles with greeting"
```

---

## Task 8: Add Minimal Status Card Styles

**Files:**
- Modify: `src/main/resources/static/index.html` (CSS section)

- [ ] **Step 1: Add minimal status card overrides**

Insert after existing status styles (around line 63):

```css
body.minimal .sgrid{gap:12px;}
body.minimal .sbox{border-radius:var(--r);padding:18px 16px;text-align:left;border:1px solid var(--border);box-shadow:none;}
body.minimal .sbox.done{background:var(--success-light);border-color:var(--success);border-left:4px solid var(--success);}
body.minimal .sbox.open{background:var(--error-light);border-color:var(--error);border-left:4px solid var(--error);}
body.minimal .semoji{font-size:1.2rem;margin-bottom:0;margin-right:10px;display:inline;}
body.minimal .slbl{display:inline;font-size:.75rem;}
body.minimal .sval{font-size:.95rem;margin-top:8px;}
body.minimal .done .sval{color:var(--success);}
body.minimal .open .sval{color:var(--error);}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "style: add minimal status card styles"
```

---

## Task 9: Add Minimal Stats Row Styles

**Files:**
- Modify: `src/main/resources/static/index.html` (CSS section)

- [ ] **Step 1: Add minimal stats overrides**

Insert after existing stats styles (around line 69):

```css
body.minimal .stats{gap:10px;}
body.minimal .stat{border-radius:var(--rs);box-shadow:none;border:1px solid var(--border);}
body.minimal .stn{background:none;-webkit-text-fill-color:var(--accent);color:var(--accent);}
body.minimal .streak{background:var(--card);border:1px solid var(--border);box-shadow:none;}
body.minimal .snum{background:none;-webkit-text-fill-color:var(--accent);color:var(--accent);}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "style: add minimal stats and streak styles"
```

---

## Task 10: Add Minimal Card and Input Styles

**Files:**
- Modify: `src/main/resources/static/index.html` (CSS section)

- [ ] **Step 1: Add minimal card and input overrides**

Insert after existing card styles (around line 82):

```css
body.minimal .slc{color:var(--text-secondary);margin:20px 0 10px;}
body.minimal .card{border-radius:var(--r);box-shadow:none;border:1px solid var(--border);}
body.minimal .ch{font-size:.9rem;}
body.minimal input{border-radius:var(--rs);border:1px solid var(--border);}
body.minimal input:focus{border-color:var(--accent);box-shadow:none;}
body.minimal .bc{background:var(--accent);border-radius:var(--rs);box-shadow:none;width:46px;height:46px;}
body.minimal .qb{background:var(--accent-light);border:1px solid var(--accent-border);color:var(--text);border-radius:var(--rs);}
body.minimal .qb:active{background:var(--accent);color:var(--bg);}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "style: add minimal card and input styles"
```

---

## Task 11: Add Minimal Leaderboard and History Styles

**Files:**
- Modify: `src/main/resources/static/index.html` (CSS section)

- [ ] **Step 1: Add minimal leaderboard and history overrides**

Insert after existing leaderboard styles (around line 101):

```css
body.minimal .tsw{border:1px solid var(--border);background:var(--card2);}
body.minimal .to{border-radius:var(--rs);}
body.minimal .to.active{background:var(--card);box-shadow:none;}
body.minimal .lbb{background:var(--accent);}
body.minimal .ed{background:var(--accent);}
body.minimal .htog{color:var(--accent);}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "style: add minimal leaderboard and history styles"
```

---

## Task 12: Add Minimal Weather and Action Card Styles

**Files:**
- Modify: `src/main/resources/static/index.html` (CSS section)

- [ ] **Step 1: Add minimal weather and action card overrides**

Insert after existing weather styles (around line 53):

```css
body.minimal .wth{background:var(--card);border:1px solid var(--border);box-shadow:none;}
body.minimal .wth::after{display:none;}
body.minimal .wtemp{background:none;-webkit-text-fill-color:var(--text);color:var(--text);}
body.minimal .wtip{background:var(--accent-light);border:1px solid var(--accent-border);color:var(--text);}
body.minimal .wac{background:var(--card);border:1px solid var(--border);box-shadow:none;}
body.minimal .gamble{background:var(--card);border:1px solid var(--border);box-shadow:none;}
body.minimal .gamble>div:first-child>div:first-child{background:none;-webkit-text-fill-color:var(--text);color:var(--text);}
body.minimal .stlnk{box-shadow:none;}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "style: add minimal weather and action card styles"
```

---

## Task 13: Add Minimal Modal and Banner Styles

**Files:**
- Modify: `src/main/resources/static/index.html` (CSS section)

- [ ] **Step 1: Add minimal modal and banner overrides**

Insert after existing modal styles (around line 128):

```css
body.minimal .modal{border-radius:var(--r) var(--r) 0 0;}
body.minimal .mb{border-radius:var(--rs);}
body.minimal .mbg{background:var(--accent);box-shadow:none;}
body.minimal .po{border-radius:var(--rs);border:1px solid var(--border);}
body.minimal .po:active{border-color:var(--accent);}
body.minimal .pi{background:var(--card);border:1px solid var(--border);box-shadow:none;}
body.minimal .ptitle{background:none;-webkit-text-fill-color:var(--text);color:var(--text);}
body.minimal .pclose{background:var(--card2);border:1px solid var(--border);color:var(--text);}
body.minimal .toast{border-radius:var(--rs);}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "style: add minimal modal and banner styles"
```

---

## Task 14: Test All Theme Combinations

**Files:**
- Test: `src/main/resources/static/index.html` in browser

- [ ] **Step 1: Test Classic Light**

1. Open page, verify default is Classic theme
2. Open Settings, select "Classic" + "Hell"
3. Verify gold gradients, glassmorphism effects

- [ ] **Step 2: Test Classic Dark**

1. Open Settings, select "Classic" + "Dunkel"
2. Verify gold on dark background

- [ ] **Step 3: Test Minimal Light**

1. Open Settings, select "Minimal" + "Hell"
2. Verify clean white cards, black accents, time-based greeting

- [ ] **Step 4: Test Minimal Dark**

1. Open Settings, select "Minimal" + "Dunkel"
2. Verify dark background, white accents

- [ ] **Step 5: Test all features in Minimal theme**

Verify each feature works:
- [ ] Walk entry with quick-select
- [ ] Food tracking
- [ ] Leaderboard time range
- [ ] History expand/collapse
- [ ] Gamble modal
- [ ] Notify modal
- [ ] Pause modal (admin)
- [ ] Confetti on first walk
- [ ] Language switch (DE/RU)

- [ ] **Step 6: Commit if any fixes needed**

```bash
git add src/main/resources/static/index.html
git commit -m "fix: theme combination fixes"
```

---

## Task 15: Cleanup Mockup Files

**Files:**
- Delete: `mockups.html`
- Delete: `mockup-final.html`

- [ ] **Step 1: Remove mockup files**

```bash
rm /home/wlad/IdeaProjects/kiratracker/mockups.html
rm /home/wlad/IdeaProjects/kiratracker/mockup-final.html
```

- [ ] **Step 2: Final commit**

```bash
git add -A
git commit -m "chore: remove mockup files after redesign complete"
```

- [ ] **Step 3: Push all changes**

```bash
git push origin master
```

---

## Summary

After completing all tasks:
- 2 themes available: Classic (gold) and Minimal (clean)
- Settings modal with Theme, Dark Mode, Language toggles
- All 16 original features preserved
- Both themes support Light/Dark mode
- Theme persisted to localStorage
