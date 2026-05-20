import { useState, useEffect } from 'react';

const screenshots = [
  { id: 'chat', label: 'Chat', url: 'https://github.com/user-attachments/assets/46a6d913-79db-48b3-a1f8-753ffba9ae5c', title: 'Doota - Chat with Model Selection' },
  { id: 'agent', label: 'Agentic Loop', url: 'https://github.com/user-attachments/assets/f25381ba-1200-463d-8814-59710035be84', title: 'Doota - Autonomous Browser Reasoning' },
  { id: 'model-spec', label: 'Model Spec', url: 'https://github.com/user-attachments/assets/77a39875-4692-444c-a066-d3c590f667fd', title: 'Doota - Comprehensive Model Specifications' },
  { id: 'hover-spec', label: 'Spec Details', url: 'https://github.com/user-attachments/assets/3d11553d-bacf-4b83-811e-faf8d55599b1', title: 'Doota - SPEC Hover Details' },
  { id: 'models', label: 'Model List', url: 'https://github.com/user-attachments/assets/328a3fdb-43a0-420d-b4c7-1c261863e2f8', title: 'Doota - Advanced Model Management' },
  { id: 'settings', label: 'Settings', url: 'https://github.com/user-attachments/assets/89802742-881f-4d35-a78d-d1c6caa00e94', title: 'Doota - Custom Connection Settings' },
  { id: 'diagnostics', label: 'Diagnostics', url: 'https://github.com/user-attachments/assets/236abf5b-d385-4382-9527-50c65f9cc9ed', title: 'Doota - Live System Diagnostics' },
  { id: 'history', label: 'History', url: 'https://github.com/user-attachments/assets/8f2a784b-8334-4abf-a007-70a028b0b524', title: 'Doota - Local Chat Persistence' }
];

const testimonials = [
  { author: '@milo_dev', quote: 'Doota is game-changing for local AI. Running autonomous agent loops directly inside my browser while inspecting Ollama details is amazing.', platform: 'Twitter' },
  { author: '@java_wizard', quote: 'Finally, a beautiful and clean UI for Ollama that lets me debug model weights, RAM, and sequential tool execution.', platform: 'GitHub' },
  { author: 'luka_k', quote: 'The collapsible execution accordion is exactly what I needed. I can see parsed JSON args and raw outputs for every tool invoked.', platform: 'Product Hunt' },
  { author: 'tech_hack', quote: 'Built-in tools like calculator and filesystem search work out-of-the-box. Clean Quarkus + React stack!', platform: 'Medium' },
  { author: '@alex_llm', quote: 'The live tokens-per-second generator metric is highly accurate. Highly recommend Doota for local LLM hacking!', platform: 'Twitter' },
  { author: 'no_cloud_guy', quote: 'No cloud dependencies. Clean, local persistent chats stored as JSON. A must-have tool for Ollama developers.', platform: 'GitHub' }
];

const commands = {
  curl: {
    comment: '# Downloader script (macOS/Linux) 🚀',
    cmd: 'curl -fsSL https://raw.githubusercontent.com/ollama4j/Doota/main/install.sh | bash'
  },
  docker: {
    comment: '# Run immediately via Docker hub',
    cmd: 'docker run -d -p 8080:8080 --name doota ollama4j/doota:latest'
  },
  jar: {
    comment: '# Download and execute runner jar file',
    cmd: 'curl -L -O https://github.com/ollama4j/doota/releases/latest/download/doota-runner.jar && java -jar doota-runner.jar'
  },
  build: {
    comment: '# Clone and build from source code',
    cmd: 'git clone https://github.com/ollama4j/Doota.git && cd Doota && mvn clean package && java -jar target/*-runner.jar'
  }
};

export default function App() {
  const [activeTab, setActiveTab] = useState('curl');
  const [activeScreenshot, setActiveScreenshot] = useState('chat');
  const [copied, setCopied] = useState(false);
  const [ssPaused, setSsPaused] = useState(false);

  // Auto-play screenshots carousel
  useEffect(() => {
    if (ssPaused) return;
    const interval = setInterval(() => {
      setActiveScreenshot((prev) => {
        const index = screenshots.findIndex((s) => s.id === prev);
        const nextIndex = (index + 1) % screenshots.length;
        return screenshots[nextIndex].id;
      });
    }, 4500);
    return () => clearInterval(interval);
  }, [ssPaused]);

  const copyToClipboard = () => {
    navigator.clipboard.writeText(commands[activeTab].cmd);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const currentScreenshot = screenshots.find((s) => s.id === activeScreenshot) || screenshots[0];

  return (
    <div>
      {/* BACKGROUND GLOW */}
      <div className="hero-glow"></div>

      {/* NAVIGATION */}
      <nav>
        <div className="nav-inner">
          <a href="#" className="nav-logo">
            <img src="/logo.png" alt="Doota Logo" />
            <div className="nav-logo-text">
              <span>Doota</span>
            </div>
          </a>
          <div className="nav-links">
            <a href="#features">Features</a>
            <a href="#demo">How it works</a>
            <a href="#get-started">Install</a>
            <a 
              href="https://github.com/ollama4j/Doota" 
              target="_blank" 
              rel="noopener noreferrer" 
              className="nav-gh"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="currentColor" style={{ marginRight: '4px' }}>
                <path d="M12 0c-6.626 0-12 5.373-12 12 0 5.302 3.438 9.8 8.207 11.387.599.111.793-.261.793-.577v-2.234c-3.338.726-4.033-1.416-4.033-1.416-.546-1.387-1.333-1.756-1.333-1.756-1.089-.745.083-.729.083-.729 1.205.084 1.839 1.237 1.839 1.237 1.07 1.834 2.807 1.304 3.492.997.107-.775.418-1.305.762-1.604-2.665-.305-5.467-1.334-5.467-5.931 0-1.311.469-2.381 1.236-3.221-.124-.303-.535-1.524.117-3.176 0 0 1.008-.322 3.301 1.23.957-.266 1.983-.399 3.003-.404 1.02.005 2.047.138 3.006.404 2.291-1.552 3.297-1.23 3.297-1.23.653 1.653.242 2.874.118 3.176.77.84 1.235 1.911 1.235 3.221 0 4.609-2.807 5.624-5.479 5.921.43.372.823 1.102.823 2.222v3.293c0 .319.192.694.801.576 4.765-1.589 8.199-6.086 8.199-11.386 0-6.627-5.373-12-12-12z"/>
              </svg>
              GitHub
            </a>
            <a href="#get-started" className="nav-cta">Download App</a>
          </div>
        </div>
      </nav>

      {/* HERO SECTION */}
      <section className="hero">
        <div className="hero-badge">
          <div className="hero-badge-dot"></div>
          Local Agentic Loop &amp; Beautiful UI for Ollama. Free forever.
        </div>
        <h1>
          Run autonomous agents locally. <span className="accent">With complete visibility.</span>
        </h1>
        <p className="hero-sub">
          Your local LLM agents run in loops, call system tools, compile code. Doota shows you exactly what they are doing in real-time, displaying execution state diagnostics, and tracking model specs on disk and memory.
        </p>
        <div className="hero-buttons">
          <a href="#get-started" className="btn-primary">Get Started Free</a>
          <a 
            href="https://github.com/ollama4j/Doota" 
            target="_blank" 
            rel="noopener noreferrer" 
            className="btn-secondary"
          >
            Star on GitHub
          </a>
        </div>
        <div className="hero-stats">
          <a href="https://github.com/ollama4j/Doota" target="_blank" rel="noopener noreferrer">
            <strong style={{ color: '#fff' }}>Ollama4J</strong> powered
          </a>
          <span>&middot;</span>
          <a href="https://github.com/ollama4j/Doota" target="_blank" rel="noopener noreferrer">
            <strong style={{ color: '#fff' }}>100%</strong> Private &amp; Local
          </a>
          <span>&middot;</span>
          <a href="https://github.com/ollama4j/Doota" target="_blank" rel="noopener noreferrer">
            <strong style={{ color: '#fff' }}>React &amp; Quarkus</strong> architecture
          </a>
        </div>
      </section>

      {/* TESTIMONIALS */}
      <section className="testimonials-section">
        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
          <h2 className="section-title" style={{ fontSize: '1.5rem', color: 'var(--text-muted)' }}>
            Loved by local LLM hackers
          </h2>
        </div>
        <div className="tt-container">
          <div className="tt-track">
            {/* Double the list for infinite loop */}
            {[...testimonials, ...testimonials].map((t, index) => (
              <div key={index} className="tt-card">
                <div className="tt-head">
                  <div className="tt-avatar" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'var(--bg-card)', fontSize: '14px', fontWeight: 'bold', color: 'var(--accent)' }}>
                    {t.author.substring(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <div className="tt-author">{t.author}</div>
                    <div className="tt-handle">{t.platform} user</div>
                  </div>
                  <span className="tt-badge">{t.platform}</span>
                </div>
                <div className="tt-quote">"{t.quote}"</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* QUICK START INSTALLATION */}
      <section id="get-started" style={{ paddingTop: '40px', paddingBottom: '40px' }}>
        <h2 className="section-title">Quick Start</h2>
        <p className="section-sub">Get Doota up and running in less than 30 seconds.</p>

        <div className="terminal-container">
          <div className="terminal-header">
            <div className="terminal-dot red"></div>
            <div className="terminal-dot yellow"></div>
            <div className="terminal-dot green"></div>

            <div className="terminal-tabs">
              <button 
                className={`terminal-tab-btn ${activeTab === 'curl' ? 'active' : ''}`}
                onClick={() => setActiveTab('curl')}
              >
                One-liner
              </button>
              <button 
                className={`terminal-tab-btn ${activeTab === 'docker' ? 'active' : ''}`}
                onClick={() => setActiveTab('docker')}
              >
                Docker
              </button>
              <button 
                className={`terminal-tab-btn ${activeTab === 'jar' ? 'active' : ''}`}
                onClick={() => setActiveTab('jar')}
              >
                Executable JAR
              </button>
              <button 
                className={`terminal-tab-btn ${activeTab === 'build' ? 'active' : ''}`}
                onClick={() => setActiveTab('build')}
              >
                Build Source
              </button>
            </div>

            <button className="terminal-copy-btn" onClick={copyToClipboard} title="Copy command">
              {copied ? (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="var(--green)" strokeWidth="2.5">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              ) : (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <rect x="9" y="9" width="13" height="13" rx="2" />
                  <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1" />
                </svg>
              )}
            </button>
          </div>

          <div className="terminal-content">
            <div className="terminal-comment">{commands[activeTab].comment}</div>
            <div className="terminal-line">
              <span className="terminal-prompt">$</span>
              <span className="terminal-cmd">{commands[activeTab].cmd}</span>
            </div>
          </div>
        </div>
      </section>

      {/* SCREENSHOTS / DEMO */}
      <section id="demo" style={{ paddingTop: '40px', paddingBottom: '40px' }}>
        <h2 className="section-title">See Doota in Action</h2>
        <p className="section-sub">A responsive, fully local dashboard to supervise your autonomous LLM loops.</p>

        {/* Tab selection */}
        <div className="ss-tabs-container">
          {screenshots.map((s) => (
            <button
              key={s.id}
              className={`ss-tab-btn ${activeScreenshot === s.id ? 'active' : ''}`}
              onClick={() => {
                setActiveScreenshot(s.id);
                setSsPaused(true);
              }}
            >
              {s.label}
            </button>
          ))}
        </div>

        {/* Main interactive terminal dashboard screenshot */}
        <div className="demo-container">
          <div className="demo-bar">
            <div className="terminal-dot red"></div>
            <div className="terminal-dot yellow"></div>
            <div className="terminal-dot green"></div>
            <span className="demo-title">doota-dashboard --view {currentScreenshot.id}</span>
          </div>
          <div className="demo-img-container">
            <img 
              className="demo-img" 
              src={currentScreenshot.url} 
              alt={currentScreenshot.title} 
              loading="eager"
            />
          </div>
        </div>
      </section>

      {/* HIGH IMPACT FEATURE */}
      <section id="features" style={{ paddingTop: '60px', paddingBottom: '40px' }}>
        <h2 className="section-title">What you get</h2>
        <p className="section-sub">Designed for local LLMs, running 100% on your own system.</p>

        <div className="killer-feature">
          <div className="killer-feature-icon">
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
            </svg>
          </div>
          <div className="killer-feature-content">
            <div className="killer-feature-badge">Core Engine</div>
            <h3>Client-Driven Autonomous Reasoning Loops</h3>
            <p>
              Run complex multi-turn thinking loops locally. Doota automatically determines when to call built-in tools (like Calculator, Weather, File search), compiles the raw results, handles errors, and returns a cohesive final output—allowing up to 10 diagnostic steps per query.
            </p>
          </div>
        </div>

        {/* Showcase side-by-side screenshots */}
        <div className="killer-feature-showcase">
          <div className="showcase-item">
            <div className="showcase-img-wrap">
              <img src="https://github.com/user-attachments/assets/f25381ba-1200-463d-8814-59710035be84" alt="Agent reasoning steps" />
            </div>
            <p>Step-by-step diagnostic agent reasoning</p>
          </div>
          <div className="showcase-item">
            <div className="showcase-img-wrap">
              <img src="https://github.com/user-attachments/assets/3d11553d-bacf-4b83-811e-faf8d55599b1" alt="Model specifications details" />
            </div>
            <p>Hover specs detailing family, quant, and file size</p>
          </div>
        </div>
      </section>

      {/* FEATURES GRID */}
      <section style={{ paddingTop: '0px', paddingBottom: '80px' }}>
        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon teal">
              🤖
            </div>
            <h3>Autonomous loops</h3>
            <p>Full client-side decision loops that sequence multiple tool calls dynamically and show live "Thinking..." cues.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon blue">
              ⚙️
            </div>
            <h3>Tool call inspection</h3>
            <p>Collapsible accordions displaying parsed JSON arguments, execution durations, and raw returns for total observability.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon green">
              🛠️
            </div>
            <h3>Out-of-the-box tools</h3>
            <p>Comes pre-packaged with filesystem search, calculadora, current date-time info, weather reports, and system diagnostics.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon purple">
              📊
            </div>
            <h3>Advanced model management</h3>
            <p>Real-time loaded-model specification panels, allowing one-click memory unloading, spec inspections, and disk deletions.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon yellow">
              ⚡
            </div>
            <h3>Server Diagnostics</h3>
            <p>Heartbeat monitors pinging Ollama (every 3 seconds) to verify network reachability and system load specifications.</p>
          </div>

          <div className="feature-card">
            <div className="feature-icon teal">
              💾
            </div>
            <h3>Local Chat Persistence</h3>
            <p>Conversations are safely preserved as JSON files locally under `~/doota/chats`, maintaining complete session history.</p>
          </div>
        </div>
      </section>

      {/* FOOTER */}
      <footer>
        <div className="footer-inner">
          <div className="footer-text">
            &copy; {new Date().getFullYear()} Doota. Released under the MIT License. Built with ❤️ by the <a href="https://github.com/ollama4j" target="_blank" rel="noopener noreferrer">Ollama4J</a> community.
          </div>
          <div className="footer-links">
            <a href="https://github.com/ollama4j/Doota" target="_blank" rel="noopener noreferrer">GitHub</a>
            <a href="https://ollama.com" target="_blank" rel="noopener noreferrer">Ollama</a>
            <a href="https://github.com/ollama4j/ollama4j" target="_blank" rel="noopener noreferrer">Ollama4J</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
