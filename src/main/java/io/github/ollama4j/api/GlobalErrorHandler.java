package io.github.ollama4j.api;

import io.vertx.ext.web.Router;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Registers Vert.x router-level error handlers so that when Quinoa's dev proxy
 * fails (e.g. "Client is closed" during shutdown) or any other server error
 * occurs on a non-API route, the browser receives a branded Doota HTML error
 * page instead of Quarkus's raw JSON stack trace.
 */
@ApplicationScoped
public class GlobalErrorHandler {

    // ─── Inline HTML error page (matches the Doota dark theme) ───────────────

    private static String buildErrorPage(int code, String title, String body) {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "  <meta charset=\"UTF-8\" />\n" +
            "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\" />\n" +
            "  <title>Doota \u2014 " + title + "</title>\n" +
            "  <link rel=\"icon\" href=\"/favicon.ico\" />\n" +
            "  <style>\n" +
            "    *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }\n" +
            "    body {\n" +
            "      font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\n" +
            "      background: #070A13;\n" +
            "      color: #F8FAFC;\n" +
            "      min-height: 100vh;\n" +
            "      display: flex;\n" +
            "      align-items: center;\n" +
            "      justify-content: center;\n" +
            "      overflow: hidden;\n" +
            "      background-image:\n" +
            "        radial-gradient(ellipse 60% 50% at 50% 0%, rgba(16,163,127,0.13) 0%, transparent 70%),\n" +
            "        radial-gradient(ellipse 40% 30% at 80% 80%, rgba(45,212,191,0.06) 0%, transparent 60%);\n" +
            "    }\n" +
            "    .card {\n" +
            "      position: relative;\n" +
            "      display: flex;\n" +
            "      flex-direction: column;\n" +
            "      align-items: center;\n" +
            "      gap: 20px;\n" +
            "      padding: 60px 56px;\n" +
            "      background: rgba(15,22,38,0.72);\n" +
            "      border: 1px solid rgba(16,163,127,0.18);\n" +
            "      border-radius: 24px;\n" +
            "      backdrop-filter: blur(24px);\n" +
            "      box-shadow: 0 0 0 1px rgba(255,255,255,0.04) inset, 0 32px 80px rgba(0,0,0,0.5);\n" +
            "      text-align: center;\n" +
            "      max-width: 480px;\n" +
            "      width: 90%;\n" +
            "      animation: fadeUp 0.5s cubic-bezier(0.16,1,0.3,1) both;\n" +
            "    }\n" +
            "    @keyframes fadeUp {\n" +
            "      from { opacity:0; transform:translateY(24px) scale(0.97); }\n" +
            "      to   { opacity:1; transform:translateY(0)   scale(1);    }\n" +
            "    }\n" +
            "    .glow {\n" +
            "      position: absolute;\n" +
            "      top: -80px; left: 50%;\n" +
            "      transform: translateX(-50%);\n" +
            "      width: 320px; height: 200px;\n" +
            "      background: radial-gradient(ellipse, rgba(16,163,127,0.22) 0%, transparent 70%);\n" +
            "      filter: blur(10px);\n" +
            "      pointer-events: none;\n" +
            "      animation: glowPulse 3s ease-in-out infinite alternate;\n" +
            "    }\n" +
            "    @keyframes glowPulse {\n" +
            "      from { opacity:0.7; transform:translateX(-50%) scaleX(1);    }\n" +
            "      to   { opacity:1;   transform:translateX(-50%) scaleX(1.12); }\n" +
            "    }\n" +
            "    .logo-wrap {\n" +
            "      width:72px; height:72px;\n" +
            "      border-radius:18px;\n" +
            "      background:rgba(16,163,127,0.10);\n" +
            "      border:1px solid rgba(16,163,127,0.25);\n" +
            "      display:flex; align-items:center; justify-content:center;\n" +
            "      overflow:hidden;\n" +
            "      box-shadow:0 0 24px rgba(16,163,127,0.15);\n" +
            "    }\n" +
            "    .logo-wrap img { width:52px; height:52px; object-fit:contain;\n" +
            "      filter:drop-shadow(0 0 8px rgba(16,163,127,0.4)); }\n" +
            "    .code {\n" +
            "      font-size:5.5rem; font-weight:800; line-height:1; letter-spacing:-4px;\n" +
            "      background:linear-gradient(135deg,#10A37F 0%,#2DD4BF 50%,#10A37F 100%);\n" +
            "      -webkit-background-clip:text; -webkit-text-fill-color:transparent;\n" +
            "      background-clip:text; background-size:200% 100%;\n" +
            "      animation:shimmer 4s linear infinite;\n" +
            "    }\n" +
            "    @keyframes shimmer {\n" +
            "      0%   { background-position:200%  center; }\n" +
            "      100% { background-position:-200% center; }\n" +
            "    }\n" +
            "    h1 { font-size:1.5rem; font-weight:600; letter-spacing:-0.3px; }\n" +
            "    p  { font-size:0.92rem; color:#94A3B8; line-height:1.6; }\n" +
            "    .btn {\n" +
            "      margin-top:8px;\n" +
            "      padding:11px 28px;\n" +
            "      background:linear-gradient(135deg,#10A37F,#0e8f6d);\n" +
            "      color:#fff; border:none; border-radius:10px;\n" +
            "      font-size:0.95rem; font-weight:600; cursor:pointer;\n" +
            "      transition:all 0.22s ease;\n" +
            "      box-shadow:0 4px 20px rgba(16,163,127,0.3);\n" +
            "      text-decoration:none; display:inline-block;\n" +
            "    }\n" +
            "    .btn:hover {\n" +
            "      background:linear-gradient(135deg,#12b88e,#10A37F);\n" +
            "      transform:translateY(-2px);\n" +
            "      box-shadow:0 8px 28px rgba(16,163,127,0.45);\n" +
            "    }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "  <div class=\"card\">\n" +
            "    <div class=\"glow\"></div>\n" +
            "    <div class=\"logo-wrap\">\n" +
            "      <img src=\"/logo.png\" alt=\"Doota\" />\n" +
            "    </div>\n" +
            "    <div class=\"code\">" + code + "</div>\n" +
            "    <h1>" + escapeHtml(title) + "</h1>\n" +
            "    <p>" + escapeHtml(body) + "</p>\n" +
            "    <a class=\"btn\" href=\"/\">\u2190 Back to Doota</a>\n" +
            "  </div>\n" +
            "</body>\n" +
            "</html>\n";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // ─── Router error handler registration ───────────────────────────────────

    public void init(@Observes Router router) {

        // 500 — internal server error (covers Quinoa "Client is closed" failures)
        router.errorHandler(500, ctx -> {
            String path = ctx.request().path();
            if (path != null && path.startsWith("/api")) {
                // API callers expect JSON — let Quarkus handle it normally
                ctx.next();
            } else {
                ctx.response()
                   .setStatusCode(500)
                   .putHeader("Content-Type", "text/html; charset=utf-8")
                   .end(buildErrorPage(500,
                       "Something went wrong",
                       "The server encountered an unexpected error. " +
                       "It may be starting up or restarting \u2014 please try again in a moment."));
            }
        });

        // 503 — service unavailable (dev-proxy not yet ready)
        router.errorHandler(503, ctx -> {
            String path = ctx.request().path();
            if (path != null && path.startsWith("/api")) {
                ctx.next();
            } else {
                ctx.response()
                   .setStatusCode(503)
                   .putHeader("Content-Type", "text/html; charset=utf-8")
                   .end(buildErrorPage(503,
                       "Service unavailable",
                       "The server is starting up. Please wait a moment and refresh the page."));
            }
        });

        // 404 — not found (handles Quinoa "failed to forward" for truly missing routes)
        router.errorHandler(404, ctx -> {
            String path = ctx.request().path();
            if (path != null && path.startsWith("/api")) {
                ctx.next();
            } else {
                ctx.response()
                   .setStatusCode(404)
                   .putHeader("Content-Type", "text/html; charset=utf-8")
                   .end(buildErrorPage(404,
                       "Page not found",
                       "The route \u201c" + escapeHtml(path) + "\u201d doesn't exist."));
            }
        });
    }
}
