:root {
  --bg: #f9f8f6;
  --surface: #ffffff;
  --text-main: #2a2a2a;
  --text-muted: #6b6b6b;
  --accent: #8c9a7b;
  --accent-hover: #7a8869;
  --border: #e0deda;
  --radius: 10px;
  --shadow: 0 4px 16px rgba(0,0,0,0.06);
  --font-main: system-ui, -apple-system, sans-serif;
  --spacing: clamp(1rem, 4vw, 2rem);
}
*, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
html { scroll-behavior: smooth; }
body { font-family: var(--font-main); background: var(--bg); color: var(--text-main); line-height: 1.6; -webkit-font-smoothing: antialiased; }
.container { max-width: 1000px; margin: 0 auto; padding: 0 var(--spacing); }
a { text-decoration: none; color: inherit; }

/* Header */
.header { position: sticky; top: 0; background: rgba(249,248,246,0.95); backdrop-filter: blur(8px); border-bottom: 1px solid var(--border); z-index: 100; padding: 1rem 0; }
.header-inner { display: flex; justify-content: space-between; align-items: center; }
.logo { font-size: 1.5rem; font-weight: 700; color: var(--accent); letter-spacing: 1px; }
.nav { display: flex; gap: 1.2rem; }
.nav a { font-weight: 500; transition: color 0.2s; }
.nav a:hover { color: var(--accent); }

/* Sections & Animations */
.section { padding: clamp(3rem, 8vw, 5rem) 0; opacity: 0; transform: translateY(20px); transition: all 0.6s ease; }
.section.visible { opacity: 1; transform: translateY(0); }
.section-title { font-size: clamp(1.5rem, 4vw, 2rem); margin-bottom: 1.5rem; text-align: center; color: var(--text-main); position: relative; }
.section-title::after { content: ''; display: block; width: 50px; height: 3px; background: var(--accent); margin: 0.5rem auto 0; border-radius: 2px; }

/* Hero */
.hero { text-align: center; padding-top: clamp(5rem, 10vw, 8rem); }
.hero-title { font-size: clamp(2rem, 6vw, 3rem); font-weight: 300; line-height: 1.2; margin-bottom: 1rem; }
.hero-sub { color: var(--text-muted); font-size: 1.1rem; max-width: 600px; margin: 0 auto 2rem; }

/* Buttons */
.btn { display: inline-block; padding: 0.8rem 1.8rem; border-radius: var(--radius); font-weight: 500; transition: all 0.2s; cursor: pointer; border: none; }
.btn-primary { background: var(--accent); color: #fff; }
.btn-primary:hover { background: var(--accent-hover); transform: translateY(-2px); box-shadow: var(--shadow); }
.btn-outline { background: transparent; border: 1.5px solid var(--accent); color: var(--accent); margin: 0.5rem; }
.btn-outline:hover { background: var(--accent); color: #fff; transform: translateY(-2px); }

/* About */
.about-content { display: flex; flex-direction: column; gap: 2rem; align-items: center; }
.about-text { text-align: center; max-width: 700px; margin: 0 auto; color: var(--text-muted); }
.img-grid { display: grid; grid-template-columns: 1fr; gap: 1rem; width: 100%; max-width: 600px; }
.img-placeholder { background: #e8e7e4; height: 180px; border-radius: var(--radius); display: flex; align-items: center; justify-content: center; color: var(--text-muted); font-weight: 500; border: 1px dashed var(--border); }

/* Directions */
.cards-grid { display: grid; grid-template-columns: 1fr; gap: 1.5rem; }
.card { background: var(--surface); padding: 1.5rem; border-radius: var(--radius); box-shadow: var(--shadow); transition: transform 0.2s; border: 1px solid var(--border); }
.card:hover { transform: translateY(-4px); }
.card .img-placeholder { height: 100px; margin-bottom: 1rem; background: #f0f0ed; border: none; }
.card h3 { margin-bottom: 0.5rem; font-weight: 600; }
.card p { color: var(--text-muted); font-size: 0.95rem; }

/* Schedule */
.schedule-table { display: grid; gap: 0; border-radius: var(--radius); overflow: hidden; border: 1px solid var(--border); }
.schedule-row { display: grid; grid-template-columns: 1fr 1fr 1fr; padding: 0.8rem; background: var(--surface); border-bottom: 1px solid var(--border); font-size: 0.9rem; }
.schedule-row:last-child { border-bottom: none; }
.header-row { background: var(--accent); color: #fff; font-weight: 600; }

/* Contact */
.contact { text-align: center; }
.contact-text { color: var(--text-muted); margin-bottom: 2rem; }
.contact-buttons { display: flex; flex-wrap: wrap; justify-content: center; gap: 0.5rem; }

/* Footer */
.footer { text-align: center; padding: 2rem 0; border-top: 1px solid var(--border); color: var(--text-muted); font-size: 0.9rem; }

/* Demo Overlay */
.demo-overlay {
  position: fixed; inset: 0; z-index: 9999; pointer-events: none; overflow: hidden;
  background: repeating-linear-gradient(
    45deg, transparent, transparent 80px, rgba(140, 154, 123, 0.05) 80px, rgba(140, 154, 123, 0.05) 160px
  );
}
.demo-overlay::after {
  content: "ДЕМО  ДЕМО  ДЕМО  ДЕМО  ДЕМО  ДЕМО  ДЕМО  ДЕМО  ДЕМО  ДЕМО";
  position: absolute; top: 50%; left: -20%; transform: rotate(-30deg) translateY(-50%);
  font-size: 4rem; font-weight: 800; color: rgba(0,0,0,0.08); white-space: nowrap; letter-spacing: 0.1em;
  text-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

/* Responsive */
@media (min-width: 768px) {
  .img-grid { grid-template-columns: 1fr 1fr; }
  .cards-grid { grid-template-columns: repeat(3, 1fr); }
  .contact-buttons { gap: 1rem; }