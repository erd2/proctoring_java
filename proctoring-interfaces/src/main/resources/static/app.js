const state = {
  token: localStorage.getItem("accessToken") || "",
  user: JSON.parse(localStorage.getItem("user") || "null")
};

const output = document.querySelector("#consoleOutput");
const currentUser = document.querySelector("#currentUser");

function log(title, payload) {
  const body = typeof payload === "string" ? payload : JSON.stringify(payload, null, 2);
  output.textContent = `[${new Date().toLocaleTimeString()}] ${title}\n${body}\n\n${output.textContent}`;
}

function setUser(auth) {
  if (!auth) {
    state.token = "";
    state.user = null;
    localStorage.removeItem("accessToken");
    localStorage.removeItem("user");
  } else {
    state.token = auth.accessToken;
    state.user = auth.user;
    localStorage.setItem("accessToken", auth.accessToken);
    localStorage.setItem("user", JSON.stringify(auth.user));
  }
  renderUser();
}

function renderUser() {
  const canCreateSessions = state.user && ["PROCTOR", "ADMIN", "SUPER_ADMIN"].includes(state.user.role);
  document.querySelector("#sessionForm button").disabled = !canCreateSessions;

  if (!state.user) {
    currentUser.textContent = "Not signed in";
    return;
  }
  currentUser.innerHTML = `
    <strong>${state.user.firstName} ${state.user.lastName}</strong><br>
    ${state.user.role} · ${state.user.username}<br>
    <span class="meta">ID: ${state.user.id}</span>
  `;
}

function values(form) {
  return Object.fromEntries(new FormData(form).entries());
}

async function request(path, options = {}) {
  const headers = {
    ...(options.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
    ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}),
    ...(options.headers || {})
  };

  const response = await fetch(path, { ...options, headers });
  const text = await response.text();
  const data = text ? JSON.parse(text) : null;

  if (!response.ok) {
    throw data || { message: `HTTP ${response.status}` };
  }
  return data;
}

async function checkHealth() {
  try {
    const data = await request("/health");
    const badge = document.querySelector("#systemStatus");
    badge.textContent = "System UP";
    badge.classList.add("ok");
    log("Health", data);
  } catch (error) {
    document.querySelector("#systemStatus").textContent = "Unavailable";
    log("Health error", error);
  }
}

async function loadSessions() {
  try {
    const data = await request("/api/sessions");
    const sessions = data.data || [];
    const list = document.querySelector("#sessionList");
    if (!sessions.length) {
      list.className = "list empty";
      list.textContent = "No sessions found for this user.";
      return;
    }
    list.className = "list";
    list.innerHTML = sessions.map(session => `
      <article class="item" data-session-id="${session.id}">
        <strong>${session.disciplineCode} · ${session.status}</strong>
        <div class="meta">
          ${session.disciplineName}<br>
          Session: ${session.id}<br>
          Student: ${session.studentName || session.studentId}<br>
          Token: ${session.examToken || "-"}<br>
          Violations: ${session.violationCount ?? 0}
        </div>
        <div class="item-actions">
          <button class="secondary" type="button" data-action="start-session">Start</button>
          <button class="secondary" type="button" data-action="end-session">End</button>
          <button class="secondary" type="button" data-action="cancel-session">Cancel</button>
        </div>
      </article>
    `).join("");
    log("Sessions loaded", data);
  } catch (error) {
    log("Load sessions failed", error);
  }
}

document.querySelectorAll(".tab").forEach(tab => {
  tab.addEventListener("click", () => {
    document.querySelectorAll(".tab").forEach(item => item.classList.remove("active"));
    tab.classList.add("active");
    document.querySelector("#loginForm").classList.toggle("hidden", tab.dataset.tab !== "login");
    document.querySelector("#registerForm").classList.toggle("hidden", tab.dataset.tab !== "register");
  });
});

document.querySelector("#loginForm").addEventListener("submit", async event => {
  event.preventDefault();
  try {
    const data = await request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(values(event.currentTarget))
    });
    setUser(data.data);
    log("Signed in", data);
    await loadSessions();
  } catch (error) {
    log("Login failed", error);
  }
});

document.querySelector("#registerForm").addEventListener("submit", async event => {
  event.preventDefault();
  try {
    const formValues = values(event.currentTarget);
    const data = await request("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(formValues)
    });
    log("Registered", data);
    const login = await request("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ username: formValues.username, password: formValues.password })
    });
    setUser(login.data);
    log("Signed in", login);
  } catch (error) {
    log("Registration failed", error);
  }
});

document.querySelector("#sessionForm").addEventListener("submit", async event => {
  event.preventDefault();
  const formValues = values(event.currentTarget);
  const payload = {
    ...formValues,
    scheduledStart: formValues.scheduledStart,
    scheduledEnd: formValues.scheduledEnd,
    maxViolations: Number(formValues.maxViolations),
    violationThreshold: Number(formValues.violationThreshold)
  };
  try {
    const data = await request("/api/sessions", {
      method: "POST",
      body: JSON.stringify(payload)
    });
    log("Session created", data);
    await loadSessions();
  } catch (error) {
    log("Create session failed", error);
  }
});

document.querySelector("#violationForm").addEventListener("submit", async event => {
  event.preventDefault();
  const formValues = values(event.currentTarget);
  const params = new URLSearchParams({
    sessionId: formValues.sessionId,
    type: formValues.type,
    confidence: formValues.confidence,
    frameTimestamp: String(Date.now()),
    description: formValues.description || ""
  });
  try {
    const data = await request(`/api/violations?${params}`, { method: "POST" });
    log("Violation reported", data);
  } catch (error) {
    log("Report violation failed", error);
  }
});

document.querySelector("#statsForm").addEventListener("submit", async event => {
  event.preventDefault();
  const { sessionId } = values(event.currentTarget);
  try {
    const data = await request(`/api/violations/stats/${encodeURIComponent(sessionId)}`);
    log("Violation stats", data);
  } catch (error) {
    log("Load stats failed", error);
  }
});

document.querySelector("#loadSessions").addEventListener("click", loadSessions);
document.querySelector("#sessionList").addEventListener("click", async event => {
  const button = event.target.closest("button[data-action]");
  if (!button) {
    return;
  }

  const sessionId = button.closest(".item").dataset.sessionId;
  const action = button.dataset.action;
  const config = {
    "start-session": {
      path: `/api/sessions/${sessionId}/start`,
      options: { method: "POST" },
      title: "Session started"
    },
    "end-session": {
      path: `/api/sessions/${sessionId}/end`,
      options: {
        method: "POST",
        body: JSON.stringify({ sessionId, notes: "Ended from web console" })
      },
      title: "Session ended"
    },
    "cancel-session": {
      path: `/api/sessions/${sessionId}/cancel`,
      options: { method: "POST" },
      title: "Session cancelled"
    }
  }[action];

  try {
    const data = await request(config.path, config.options);
    log(config.title, data);
    await loadSessions();
  } catch (error) {
    log(`${config.title} failed`, error);
  }
});
document.querySelector("#logoutButton").addEventListener("click", () => {
  setUser(null);
  log("Session cleared", "Local token removed.");
});
document.querySelector("#clearLog").addEventListener("click", () => {
  output.textContent = "Ready.";
});

renderUser();
checkHealth();
if (state.token) {
  loadSessions();
}
