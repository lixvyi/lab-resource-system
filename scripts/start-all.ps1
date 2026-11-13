# 一键初始化数据库并启动三个微服务 (Windows)
$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
Set-Location $Root

$env:DB_USER = if ($env:DB_USER) { $env:DB_USER } else { "root" }
if (-not $env:DB_PASSWORD) {
    $dotenv = Join-Path $Root "material-service\.env"
    if (Test-Path $dotenv) {
        foreach ($line in Get-Content $dotenv) {
            if ($line -match '^\s*DB_PASSWORD=(.*)$') { $env:DB_PASSWORD = $Matches[1].Trim() }
        }
    }
}
if (-not $env:DB_PASSWORD) { throw "请设置环境变量 DB_PASSWORD，或在 material-service/.env 中填写" }
$env:DB_NAME = "lab_resource_system"

$mysql = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
if (-not (Test-Path $mysql)) { $mysql = "mysql" }

Write-Host "[1/4] 初始化 MySQL (utf8mb4) ..."
$initSql = Join-Path $Root "db\init.sql"
# 必须用 cmd 的二进制重定向。PowerShell 管道会按系统 ANSI(中文 Windows 为 GBK)
# 重编码 UTF-8 脚本，客户端再按 utf8mb4 解析时会把「男」的引号吃掉，
# 整行被塞进 gender，STRICT 模式就报 Data too long for column 'gender'。
$mysqlCmd = "`"$mysql`" -u $env:DB_USER -p$($env:DB_PASSWORD) --default-character-set=utf8mb4 < `"$initSql`""
cmd /c $mysqlCmd
if ($LASTEXITCODE -ne 0) { throw "数据库初始化失败，请确认 MySQL 已启动且密码正确" }

function Stop-Port($port) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($c in $conns) {
        Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
    }
}
8080,8081,8082 | ForEach-Object { Stop-Port $_ }

Write-Host "[2/4] 启动 Node.js 物资服务 :8082 ..."
Set-Location (Join-Path $Root "material-service")
if (-not (Test-Path "node_modules")) { npm install }
Start-Process -WindowStyle Hidden -FilePath "node" -ArgumentList "src/app.js" -WorkingDirectory (Join-Path $Root "material-service") -RedirectStandardOutput (Join-Path $Root "scripts\material-service.log") -RedirectStandardError (Join-Path $Root "scripts\material-service.err.log")

Write-Host "[3/4] 启动 Python 借用服务 :8081 ..."
$py = Join-Path $Root "borrow-service\venv\Scripts\python.exe"
if (-not (Test-Path $py)) { $py = "python" }
Start-Process -WindowStyle Hidden -FilePath $py -ArgumentList "app.py" -WorkingDirectory (Join-Path $Root "borrow-service") -RedirectStandardOutput (Join-Path $Root "scripts\borrow-service.log") -RedirectStandardError (Join-Path $Root "scripts\borrow-service.err.log")

Write-Host "[4/4] 构建并启动 Java 网关 :8080 ..."
$mvn = Join-Path $Root "tools\apache-maven-3.9.9\bin\mvn.cmd"
if (-not (Test-Path $mvn)) { $mvn = "mvn" }
Set-Location (Join-Path $Root "gateway-java")
& $mvn -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "Maven 打包失败" }
$env:SERVER_PORT = "8080"
Start-Process -WindowStyle Hidden -FilePath "java" -ArgumentList "-Dfile.encoding=UTF-8","-Dserver.port=8080","-jar","target/gateway-java.jar" -WorkingDirectory (Join-Path $Root "gateway-java") -RedirectStandardOutput (Join-Path $Root "scripts\gateway-java.log") -RedirectStandardError (Join-Path $Root "scripts\gateway-java.err.log")

Set-Location $Root
Write-Host "等待服务启动..."
Start-Sleep -Seconds 18
foreach ($u in @("http://localhost:8080/health","http://localhost:8082/health","http://localhost:8081/health")) {
    try {
        $r = Invoke-RestMethod $u -TimeoutSec 5
        Write-Host ("  {0} -> {1}" -f $u, ($r.message))
    } catch {
        Write-Host ("  {0} -> 未响应 ({1})" -f $u, $_.Exception.Message)
    }
}
Write-Host "客户端: http://localhost:8080/"
