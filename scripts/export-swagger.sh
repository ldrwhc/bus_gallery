#!/usr/bin/env bash
set -euo pipefail

API_URL=${1:-"http://localhost:8080/v3/api-docs"}
OUTPUT_DIR=${2:-"docs/swagger"}

mkdir -p "$OUTPUT_DIR"

curl -s "$API_URL" -o "$OUTPUT_DIR/openapi.json"

if [ ! -f "$OUTPUT_DIR/index.html" ]; then
  cat > "$OUTPUT_DIR/index.html" <<'HTML'
<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>Bus Gallery API Docs</title>
  <link rel="stylesheet" href="https://unpkg.com/swagger-ui-dist@5/swagger-ui.css" />
</head>
<body>
  <div id="swagger-ui"></div>
  <script src="https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js"></script>
  <script>
    window.onload = () => {
      SwaggerUIBundle({
        url: './openapi.json',
        dom_id: '#swagger-ui'
      });
    };
  </script>
</body>
</html>
HTML
fi

echo "Exported to $OUTPUT_DIR/openapi.json"
