# 档案系统本地一键部署（保底方案，不依赖 GitHub Actions）
# 用法：在仓库根目录执行  powershell -ExecutionPolicy Bypass -File deploy-local.ps1
# 前提：本地已配好 SSH 免密（公钥在服务器 /root/.ssh/authorized_keys，见 deploy/README.md）
param(
    [string]$ServerHost = "111.228.56.207",
    [string]$User = "root",
    [int]$Port = 22,
    [string]$KeyPath = "$env:USERPROFILE\.ssh\stuarchives_deploy"
)

$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot

# 指定了密钥就用它（GitHub Actions 用的同一把），否则退回系统默认密钥
$sshOpts = @("-p", "$Port", "-o", "StrictHostKeyChecking=accept-new")
$scpOpts = @("-P", "$Port", "-o", "StrictHostKeyChecking=accept-new")
if ($KeyPath -and (Test-Path $KeyPath)) {
    $sshOpts += @("-i", $KeyPath)
    $scpOpts += @("-i", $KeyPath)
} else {
    Write-Host "    提示：未找到密钥 $KeyPath，将使用系统默认密钥" -ForegroundColor Yellow
}

$jar = "target/student-archives-0.0.1-SNAPSHOT.jar"

Write-Host "==> 1/3 构建 jar" -ForegroundColor Cyan
& .\mvnw -B -q -DskipTests package
if ($LASTEXITCODE -ne 0) { throw "构建失败" }
if (-not (Test-Path $jar)) { throw "找不到产物 $jar" }
Write-Host "    产物大小：$([math]::Round((Get-Item $jar).Length / 1MB, 1)) MB"

Write-Host "==> 2/3 上传 jar 与部署脚本到服务器" -ForegroundColor Cyan
scp @scpOpts $jar "${User}@${ServerHost}:/tmp/stu-new.jar"
if ($LASTEXITCODE -ne 0) { throw "上传 jar 失败" }
scp @scpOpts deploy/deploy-jar.sh "${User}@${ServerHost}:/tmp/stu-deploy-jar.sh"
if ($LASTEXITCODE -ne 0) { throw "上传部署脚本失败" }

Write-Host "==> 3/3 远程部署（备份 -> 替换 -> 重启 -> 健康检查 -> 失败自动回滚）" -ForegroundColor Cyan
ssh @sshOpts "${User}@${ServerHost}" "bash /tmp/stu-deploy-jar.sh /tmp/stu-new.jar; rm -f /tmp/stu-new.jar /tmp/stu-deploy-jar.sh"
if ($LASTEXITCODE -ne 0) { throw "远程部署失败（脚本已在服务器侧尝试回滚，详见上方输出）" }

Write-Host "==> 完成" -ForegroundColor Green
