param(
  [string]$ApiUrl = "http://localhost:8080/v3/api-docs",
  [string]$OutputDir = "docs/swagger"
)

if (!(Test-Path $OutputDir)) {
  New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null
}

$swaggerJson = Invoke-RestMethod -Uri $ApiUrl -Method Get
$swaggerJson | ConvertTo-Json -Depth 99 | Set-Content -Path (Join-Path $OutputDir "openapi.json") -Encoding UTF8

$indexPath = Join-Path $OutputDir "index.html"
if (!(Test-Path $indexPath)) {
  $indexHtml = @"
<!doctype html>
<html lang=\"zh-CN\">
<head>
  <meta charset=\"UTF-8\" />
  <title>Bus Gallery API Docs</title>
  <link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui.css\" />
</head>
<body>
  <div id=\"swagger-ui\"></div>
  <script src=\"https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js\"></script>
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
"@
  Set-Content -Path $indexPath -Value $indexHtml -Encoding UTF8
}

Write-Host "Exported to $OutputDir/openapi.json"
