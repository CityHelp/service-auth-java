package com.crudzaso.cityhelp.auth.infrastructure.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * OAuth2 Controller for Google authentication redirect.
 * Handles the OAuth2 redirect endpoint outside of /api/auth path.
 *
 * @author CityHelp Team
 */
@RestController
public class OAuth2Controller {

    /**
     * OAuth2 redirect endpoint.
     * This endpoint receives tokens after successful Google OAuth2 authentication.
     * Returns a simple HTML page displaying the tokens or can redirect to mobile app.
     *
     * Note: This endpoint is configured in application.yml as:
     * app.oauth2.redirect-uri=http://localhost:8001/oauth2/redirect
     *
     * @param accessToken JWT access token
     * @param refreshToken JWT refresh token
     * @param error Error message if authentication failed
     * @return HTML page with tokens or error message
     */
    @GetMapping(value = "/oauth2/redirect", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> oauth2RedirectPage(
            @RequestParam(required = false) String access_token,
            @RequestParam(required = false) String refresh_token,
            @RequestParam(required = false) String error
    ) {
        // Handle error case
        if (error != null) {
            String errorHtml = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Error - CityHelp Auth</title>
                    <style>
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                            display: flex;
                            justify-content: center;
                            align-items: center;
                            min-height: 100vh;
                            margin: 0;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                        }
                        .container {
                            background: white;
                            padding: 2rem;
                            border-radius: 10px;
                            box-shadow: 0 10px 25px rgba(0,0,0,0.2);
                            max-width: 500px;
                            text-align: center;
                        }
                        h1 { color: #e74c3c; margin-top: 0; }
                        p { color: #555; line-height: 1.6; }
                        .error-icon { font-size: 3rem; margin-bottom: 1rem; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <div class="error-icon">❌</div>
                        <h1>Error de Autenticación</h1>
                        <p>Hubo un problema al autenticarte con Google.</p>
                        <p><strong>Error:</strong> %s</p>
                        <p>Por favor, intenta nuevamente.</p>
                    </div>
                </body>
                </html>
                """.formatted(error);

            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorHtml);
        }

        // Success case - display tokens
        String successHtml = """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Autenticación Exitosa - CityHelp</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        display: flex;
                        justify-content: center;
                        align-items: center;
                        min-height: 100vh;
                        margin: 0;
                        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    }
                    .container {
                        background: white;
                        padding: 2rem;
                        border-radius: 10px;
                        box-shadow: 0 10px 25px rgba(0,0,0,0.2);
                        max-width: 600px;
                        width: 90%%;
                    }
                    h1 { color: #27ae60; margin-top: 0; text-align: center; }
                    .success-icon { text-align: center; font-size: 3rem; margin-bottom: 1rem; }
                    .token-section {
                        margin: 1.5rem 0;
                        padding: 1rem;
                        background: #f8f9fa;
                        border-radius: 5px;
                        border-left: 4px solid #667eea;
                    }
                    .token-label {
                        font-weight: bold;
                        color: #333;
                        margin-bottom: 0.5rem;
                    }
                    .token-value {
                        font-family: 'Courier New', monospace;
                        font-size: 0.85rem;
                        word-break: break-all;
                        color: #555;
                        background: white;
                        padding: 0.75rem;
                        border-radius: 3px;
                        border: 1px solid #dee2e6;
                    }
                    .copy-btn {
                        background: #667eea;
                        color: white;
                        border: none;
                        padding: 0.5rem 1rem;
                        border-radius: 5px;
                        cursor: pointer;
                        margin-top: 0.5rem;
                        font-size: 0.9rem;
                    }
                    .copy-btn:hover { background: #5568d3; }
                    .copy-btn:active { background: #4557c2; }
                    .info {
                        text-align: center;
                        color: #555;
                        margin-top: 1.5rem;
                        padding-top: 1.5rem;
                        border-top: 1px solid #dee2e6;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="success-icon">✅</div>
                    <h1>¡Autenticación Exitosa!</h1>

                    <div class="token-section">
                        <div class="token-label">Access Token (24h):</div>
                        <div class="token-value" id="accessToken">%s</div>
                        <button class="copy-btn" onclick="copyToken('accessToken')">📋 Copiar Access Token</button>
                    </div>

                    <div class="token-section">
                        <div class="token-label">Refresh Token (7d):</div>
                        <div class="token-value" id="refreshToken">%s</div>
                        <button class="copy-btn" onclick="copyToken('refreshToken')">📋 Copiar Refresh Token</button>
                    </div>

                    <div class="info">
                        <p><strong>Guarda estos tokens de forma segura.</strong></p>
                        <p>Úsalos en el header Authorization: Bearer &lt;access_token&gt;</p>
                        <p>Puedes cerrar esta ventana.</p>
                    </div>
                </div>

                <script>
                    function copyToken(elementId) {
                        const text = document.getElementById(elementId).textContent;
                        navigator.clipboard.writeText(text).then(() => {
                            alert('Token copiado al portapapeles ✅');
                        }).catch(err => {
                            console.error('Error al copiar:', err);
                            alert('Error al copiar token. Por favor copia manualmente.');
                        });
                    }

                    // For mobile apps: you can extract tokens from URL and send to app
                    // Example: window.location.href = 'cityhelp://oauth?access_token=' + accessToken;

                    // Auto-close after 30 seconds (optional)
                    // setTimeout(() => { window.close(); }, 30000);
                </script>
            </body>
            </html>
            """.formatted(access_token, refresh_token);

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(successHtml);
    }
}
