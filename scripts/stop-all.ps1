foreach ($port in 8080,8081,8082,3000) {
    $conns = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    if ($conns) {
        $conns | ForEach-Object {
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
            Write-Host "已停止端口 $port PID $($_.OwningProcess)"
        }
    } else {
        Write-Host "端口 $port 空闲"
    }
}
